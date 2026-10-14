package com.eleazar.localhive.data.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.edit
import androidx.exifinterface.media.ExifInterface
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

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

    override fun sizeOf(uri: String): Long =
        context.contentResolver.query(Uri.parse(uri), arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        } ?: -1L

    override fun mimeTypeOf(uri: String): String? = context.contentResolver.getType(Uri.parse(uri))

    override fun open(uri: String): ByteReadChannel =
        (context.contentResolver.openInputStream(Uri.parse(uri)) ?: error("Couldn't read that file")).toByteReadChannel()
}
