package com.eleazar.localhive.data.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.edit
import androidx.exifinterface.media.ExifInterface
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

actual fun logWarning(tag: String, message: String, error: Throwable?) {
    Log.w(tag, message, error)
}

class SharedPreferencesStore(context: Context) : KeyValueStore {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String) = prefs.edit { putString(key, value) }
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }
}

class AndroidMediaReader(private val context: Context) : MediaReader {

    override suspend fun compressedJpeg(uri: String, maxSide: Int, quality: Int): ByteArray =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val image = Uri.parse(uri)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(image).use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "That file isn't an image we can read" }

            // Decode at the smallest power-of-two scale that is still at least maxSide, then finish exactly.
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val decoded = resolver.openInputStream(image).use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: error("Couldn't read that image")

            val scale = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
            // BitmapFactory ignores EXIF orientation, so camera photos would otherwise upload sideways.
            val matrix = Matrix().apply {
                postScale(scale, scale)
                postRotate(rotationOf(image))
            }
            val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (upright !== decoded) decoded.recycle()

            ByteArrayOutputStream().use { out ->
                upright.compress(Bitmap.CompressFormat.JPEG, quality, out)
                upright.recycle()
                out.toByteArray()
            }
        }

    private fun rotationOf(uri: Uri): Float = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
    }.getOrDefault(0f)

    override fun sizeOf(uri: String): Long {
        val parsed = Uri.parse(uri)
        // Shrunk videos are plain files in the cache, which the content resolver can't size.
        if (parsed.scheme == "file") return parsed.path?.let { File(it).length() } ?: -1L
        return context.contentResolver.query(parsed, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        } ?: -1L
    }

    override fun mimeTypeOf(uri: String): String? =
        if (uri.startsWith("file:") && uri.endsWith(".mp4")) "video/mp4" else context.contentResolver.getType(Uri.parse(uri))

    override fun videoDurationMillis(uri: String): Long =
        videoInfo(uri)?.durationMillis ?: -1L

    @OptIn(UnstableApi::class)
    override suspend fun shrinkVideo(uri: String, bitrate: Int, onProgress: (Float) -> Unit): String? {
        val info = withContext(Dispatchers.IO) { videoInfo(uri) } ?: return null
        val shortSide = minOf(info.width, info.height)
        if (shortSide <= 0) return null
        val alreadySmall = shortSide <= VIDEO_UPLOAD_SHORT_SIDE && info.bitrate in 1..(bitrate * 3 / 2)
        if (alreadySmall) return null

        val output = File(context.cacheDir, "upload-${UUID.randomUUID()}.mp4")
        val scale = minOf(1f, VIDEO_UPLOAD_SHORT_SIDE.toFloat() / shortSide)
        // HDR clips from newer phones look washed out on most screens unless they're tone-mapped
        // to normal colour. Not every phone's graphics can do that, so each way is tried in turn,
        // ending with reading HDR as if it were normal: duller colours, but the video still goes.
        val hdrModes = listOf(
            Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL,
            Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_MEDIACODEC,
            Composition.HDR_MODE_EXPERIMENTAL_FORCE_INTERPRET_HDR_AS_SDR
        )
        for (hdrMode in hdrModes) {
            val error = export(uri, output, scale, bitrate, hdrMode, onProgress)
            if (error == null) {
                onProgress(1f)
                return Uri.fromFile(output).toString()
            }
            logWarning("MediaReader", "Couldn't shrink the video with HDR mode $hdrMode", error)
            output.delete()
            onProgress(0f)
        }
        return null
    }

    /** One attempt at re-encoding [uri] into [output]; the reason it failed, or null when it worked. */
    @OptIn(UnstableApi::class)
    private suspend fun export(
        uri: String,
        output: File,
        scale: Float,
        bitrate: Int,
        hdrMode: Int,
        onProgress: (Float) -> Unit
    ): ExportException? = withContext(Dispatchers.Main) {
        // Transformer runs on the thread it's built on, which needs a Looper: the main one.
        val finished = CompletableDeferred<ExportException?>()
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setEncoderFactory(
                DefaultEncoderFactory.Builder(context)
                    .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(bitrate).build())
                    .build()
            )
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    finished.complete(null)
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    finished.complete(exportException)
                }
            })
            .build()
        val item = EditedMediaItem.Builder(MediaItem.fromUri(uri))
            .setEffects(Effects(emptyList(), listOf(ScaleAndRotateTransformation.Builder().setScale(scale, scale).build())))
            .build()
        val composition = Composition.Builder(EditedMediaItemSequence(item)).setHdrMode(hdrMode).build()

        transformer.start(composition, output.path)
        val progress = launch {
            val holder = ProgressHolder()
            while (true) {
                delay(250)
                if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(holder.progress / 100f)
                }
            }
        }
        try {
            finished.await()
        } catch (e: CancellationException) {
            transformer.cancel()
            output.delete()
            throw e
        } finally {
            progress.cancel()
        }
    }

    override fun deleteCopy(uri: String) {
        Uri.parse(uri).path?.let { File(it).delete() }
    }

    private class VideoInfo(val durationMillis: Long, val width: Int, val height: Int, val bitrate: Int)

    private fun videoInfo(uri: String): VideoInfo? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(uri))
            fun number(key: Int) = retriever.extractMetadata(key)?.toLongOrNull() ?: -1L
            VideoInfo(
                durationMillis = number(MediaMetadataRetriever.METADATA_KEY_DURATION),
                width = number(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt(),
                height = number(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt(),
                bitrate = number(MediaMetadataRetriever.METADATA_KEY_BITRATE).toInt()
            )
        } finally {
            retriever.release()
        }
    }.getOrNull()

    override fun open(uri: String): ByteReadChannel =
        (context.contentResolver.openInputStream(Uri.parse(uri)) ?: error("Couldn't read that file")).toByteReadChannel()
}

actual val isIos: Boolean = false
