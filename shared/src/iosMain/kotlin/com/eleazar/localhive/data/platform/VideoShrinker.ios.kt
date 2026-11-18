package com.eleazar.localhive.data.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAssetReader
import platform.AVFoundation.AVAssetReaderStatusCompleted
import platform.AVFoundation.AVAssetReaderTrackOutput
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVAssetWriter
import platform.AVFoundation.AVAssetWriterInput
import platform.AVFoundation.AVAssetWriterStatusCompleted
import platform.AVFoundation.AVAssetWriterStatusWriting
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFoundation.AVFileTypeMPEG4
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVLinearPCMBitDepthKey
import platform.AVFAudio.AVLinearPCMIsBigEndianKey
import platform.AVFAudio.AVLinearPCMIsFloatKey
import platform.AVFAudio.AVLinearPCMIsNonInterleaved
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVVideoAverageBitRateKey
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.AVVideoCodecTypeH264
import platform.AVFoundation.AVVideoCompressionPropertiesKey
import platform.AVFoundation.AVVideoHeightKey
import platform.AVFoundation.AVVideoWidthKey
import platform.AVFoundation.duration
import platform.AVFoundation.estimatedDataRate
import platform.AVFoundation.naturalSize
import platform.AVFoundation.preferredTransform
import platform.AVFoundation.tracksWithMediaType
import platform.AVFoundation.transform
import platform.CoreAudioTypes.kAudioFormatLinearPCM
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.CoreFoundation.CFRelease
import platform.CoreMedia.CMSampleBufferGetPresentationTimeStamp
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.CoreVideo.kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.darwin.dispatch_queue_create
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/*
 * Re-encodes a video with AVAssetReader and AVAssetWriter rather than an export preset, since
 * the presets can't be told a bitrate and their 720p one comes out several times bigger.
 */

// The values of kCVPixelBufferPixelFormatTypeKey and friends, which Kotlin only sees as CF strings.
private const val PIXEL_FORMAT_KEY = "PixelFormatType"
private const val PIXEL_WIDTH_KEY = "Width"
private const val PIXEL_HEIGHT_KEY = "Height"

private const val AUDIO_BITRATE = 96_000

@OptIn(ExperimentalForeignApi::class)
internal fun iosVideoDurationMillis(url: NSURL): Long {
    val seconds = CMTimeGetSeconds(AVURLAsset.URLAssetWithURL(url, options = null).duration)
    return if (seconds.isFinite() && seconds > 0) (seconds * 1000).toLong() else -1L
}

/** See [MediaReader.shrinkVideo]. Returns the copy's file:// URL, or null to upload the original. */
@OptIn(ExperimentalForeignApi::class)
internal suspend fun iosShrinkVideo(source: NSURL, bitrate: Int, onProgress: (Float) -> Unit): String? {
    val asset = AVURLAsset.URLAssetWithURL(source, options = null)
    val videoTrack = asset.tracksWithMediaType(AVMediaTypeVideo).firstOrNull() as? AVAssetTrack ?: return null
    val audioTrack = asset.tracksWithMediaType(AVMediaTypeAudio).firstOrNull() as? AVAssetTrack

    // The size as stored; the track's transform turns it upright when played.
    val (width, height) = videoTrack.naturalSize.useContents { width to height }
    val shortSide = minOf(width, height)
    if (shortSide <= 0) return null
    val rate = videoTrack.estimatedDataRate
    if (shortSide <= VIDEO_UPLOAD_SHORT_SIDE && rate > 0 && rate <= bitrate * 1.5f) return null

    val scale = minOf(1.0, VIDEO_UPLOAD_SHORT_SIDE / shortSide)
    // H.264 wants even dimensions.
    val outWidth = (width * scale / 2).roundToInt() * 2
    val outHeight = (height * scale / 2).roundToInt() * 2
    val durationSeconds = CMTimeGetSeconds(asset.duration).takeIf { it.isFinite() && it > 0 } ?: 0.0

    val reader = AVAssetReader(asset = asset, error = null)
    val videoOut = AVAssetReaderTrackOutput(
        track = videoTrack,
        outputSettings = mapOf<Any?, Any?>(
            PIXEL_FORMAT_KEY to NSNumber(unsignedInt = kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange),
            PIXEL_WIDTH_KEY to outWidth,
            PIXEL_HEIGHT_KEY to outHeight
        )
    ).apply { alwaysCopiesSampleData = false }
    // Decoded to plain 16-bit stereo, which the AAC encoder below accepts from any source.
    val audioOut = audioTrack?.let {
        AVAssetReaderTrackOutput(
            track = it,
            outputSettings = mapOf<Any?, Any?>(
                AVFormatIDKey to kAudioFormatLinearPCM,
                AVSampleRateKey to 44_100,
                AVNumberOfChannelsKey to 2,
                AVLinearPCMBitDepthKey to 16,
                AVLinearPCMIsFloatKey to false,
                AVLinearPCMIsBigEndianKey to false,
                AVLinearPCMIsNonInterleaved to false
            )
        ).apply { alwaysCopiesSampleData = false }
    }

    val output = NSURL.fileURLWithPath(NSTemporaryDirectory() + "upload-" + NSUUID().UUIDString + ".mp4")
    val writer = AVAssetWriter(uRL = output, fileType = AVFileTypeMPEG4, error = null)
    writer.shouldOptimizeForNetworkUse = true
    val videoIn = AVAssetWriterInput(
        mediaType = AVMediaTypeVideo,
        outputSettings = mapOf<Any?, Any?>(
            AVVideoCodecKey to AVVideoCodecTypeH264,
            AVVideoWidthKey to outWidth,
            AVVideoHeightKey to outHeight,
            AVVideoCompressionPropertiesKey to mapOf<Any?, Any?>(AVVideoAverageBitRateKey to bitrate)
        )
    ).apply {
        expectsMediaDataInRealTime = false
        // Phones store portrait video sideways with a rotation; keeping it keeps the copy upright.
        transform = videoTrack.preferredTransform
    }
    val audioIn = audioOut?.let {
        AVAssetWriterInput(
            mediaType = AVMediaTypeAudio,
            outputSettings = mapOf<Any?, Any?>(
                AVFormatIDKey to kAudioFormatMPEG4AAC,
                AVSampleRateKey to 44_100,
                AVNumberOfChannelsKey to 2,
                AVEncoderBitRateKey to AUDIO_BITRATE
            )
        ).apply { expectsMediaDataInRealTime = false }
    }

    fun discard(): String? {
        reader.cancelReading()
        if (writer.status == AVAssetWriterStatusWriting) writer.cancelWriting()
        NSFileManager.defaultManager.removeItemAtURL(output, error = null)
        return null
    }

    reader.addOutput(videoOut)
    writer.addInput(videoIn)
    if (audioOut != null && audioIn != null) {
        reader.addOutput(audioOut)
        writer.addInput(audioIn)
    }
    if (!reader.startReading() || !writer.startWriting()) return discard()
    writer.startSessionAtSourceTime(CMTimeMake(0, 1))

    try {
        coroutineScope {
            launch {
                pump(videoOut, videoIn, reader, "localhive.video") { seconds ->
                    if (durationSeconds > 0) onProgress((seconds / durationSeconds).toFloat().coerceIn(0f, 1f))
                }
            }
            if (audioOut != null && audioIn != null) launch { pump(audioOut, audioIn, reader, "localhive.audio") }
        }
    } catch (e: CancellationException) {
        discard()
        throw e
    }

    if (reader.status != AVAssetReaderStatusCompleted || writer.status != AVAssetWriterStatusWriting) {
        logWarning("MediaReader", "Couldn't shrink the video (${reader.error ?: writer.error}), uploading the original", null)
        return discard()
    }
    suspendCancellableCoroutine { done -> writer.finishWritingWithCompletionHandler { done.resume(Unit) } }
    if (writer.status != AVAssetWriterStatusCompleted) return discard()
    onProgress(1f)
    return output.absoluteString
}

/** Feeds one track from [output] to [input] until it runs out, the writer refuses, or it's cancelled. */
@OptIn(ExperimentalForeignApi::class)
private suspend fun pump(
    output: AVAssetReaderTrackOutput,
    input: AVAssetWriterInput,
    reader: AVAssetReader,
    queueName: String,
    onSampleAt: (Double) -> Unit = {}
) = suspendCancellableCoroutine { continuation ->
    var finished = false
    // Cancelling reading makes copyNextSampleBuffer return null, which ends the loop below.
    continuation.invokeOnCancellation { reader.cancelReading() }
    input.requestMediaDataWhenReadyOnQueue(dispatch_queue_create(queueName, null)) {
        while (!finished && input.readyForMoreMediaData) {
            val buffer = output.copyNextSampleBuffer()
            val appended = buffer != null && run {
                onSampleAt(CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(buffer)))
                input.appendSampleBuffer(buffer).also { CFRelease(buffer) }
            }
            if (!appended) {
                finished = true
                input.markAsFinished()
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }
}
