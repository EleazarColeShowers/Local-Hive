package com.eleazar.localhive.data.platform

import io.ktor.utils.io.ByteReadChannel
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSLog
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.dataWithContentsOfFile
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

actual fun logWarning(tag: String, message: String, error: Throwable?) {
    NSLog("%@", "W/$tag: $message${error?.let { " ($it)" }.orEmpty()}")
}

class UserDefaultsStore : KeyValueStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? = defaults.stringForKey(key)
    override fun putString(key: String, value: String) = defaults.setObject(value, key)
    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) default else defaults.boolForKey(key)
    override fun putBoolean(key: String, value: Boolean) = defaults.setBool(value, key)
}

/** Picked media arrives as a copy in the app's temporary folder, given as a file:// URL or a path. */
@OptIn(ExperimentalForeignApi::class)
class IosMediaReader : MediaReader {

    override suspend fun compressedJpeg(uri: String, maxSide: Int, quality: Int): ByteArray =
        withContext(Dispatchers.Default) {
            val image = UIImage.imageWithContentsOfFile(pathOf(uri))
                ?: error("That file isn't an image we can read")
            val (width, height) = image.size.useContents { width to height }
            val scale = minOf(1.0, maxSide / maxOf(width, height))
            val targetWidth = width * scale
            val targetHeight = height * scale
            // Drawing applies the photo's orientation, so the JPEG comes out upright.
            val format = UIGraphicsImageRendererFormat.defaultFormat().apply { setScale(1.0) }
            val renderer = UIGraphicsImageRenderer(size = CGSizeMake(targetWidth, targetHeight), format = format)
            val resized = renderer.imageWithActions { _ ->
                image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
            }
            UIImageJPEGRepresentation(resized, quality / 100.0)?.toByteArray()
                ?: error("Couldn't read that image")
        }

    override fun sizeOf(uri: String): Long {
        val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(pathOf(uri), null) ?: return -1L
        return (attributes[NSFileSize] as? NSNumber)?.longLongValue ?: -1L
    }

    override fun mimeTypeOf(uri: String): String? = when (pathOf(uri).substringAfterLast('.').lowercase()) {
        "mp4", "m4v" -> "video/mp4"
        "mov" -> "video/quicktime"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "heic" -> "image/heic"
        else -> null
    }

    // Read whole: iOS has no stream to hand Ktor, and videos are capped at 40 MB before this runs.
    override fun open(uri: String): ByteReadChannel {
        val data = NSData.dataWithContentsOfFile(pathOf(uri)) ?: error("Couldn't read that file")
        return ByteReadChannel(data.toByteArray())
    }

    private fun pathOf(uri: String): String =
        if (uri.startsWith("file:")) NSURL.URLWithString(uri)?.path ?: uri.removePrefix("file://") else uri
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}
