package com.eleazar.localhive.data.remote.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/**
 * Shrinks a picked photo to a JPEG no larger than [maxSide] on its longest edge, upright.
 * Phone photos are often 4–12 MB; this keeps uploads quick on mobile data and well inside
 * Cloudinary's free allowance.
 */
object ImageCompressor {

    fun compress(context: Context, uri: Uri, maxSide: Int, quality: Int = 82): ByteArray {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "That file isn't an image we can read" }

        // Decode at the smallest power-of-two scale that is still at least maxSide, then finish exactly.
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val decoded = resolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Couldn't read that image")

        val scale = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
        // BitmapFactory ignores EXIF orientation, so camera photos would otherwise upload sideways.
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotationOf(context, uri))
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (upright !== decoded) decoded.recycle()

        return ByteArrayOutputStream().use { out ->
            upright.compress(Bitmap.CompressFormat.JPEG, quality, out)
            upright.recycle()
            out.toByteArray()
        }
    }

    private fun rotationOf(context: Context, uri: Uri): Float = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
    }.getOrDefault(0f)
}
