package com.eleazar.localhive.data.platform

import io.ktor.utils.io.ByteReadChannel

/** Logs something worth knowing that the user isn't told about. */
expect fun logWarning(tag: String, message: String, error: Throwable? = null)

/** Small preferences that live on this device only. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

/**
 * Reads media the user picked, given as the platform's URI string: a content:// URI on Android,
 * a file:// URL on iOS.
 */
interface MediaReader {
    /**
     * Shrinks a photo to a JPEG no larger than [maxSide] on its longest edge, upright. Phone
     * photos are often 4–12 MB; this keeps uploads quick on mobile data and well inside
     * Cloudinary's free allowance.
     */
    suspend fun compressedJpeg(uri: String, maxSide: Int, quality: Int = 82): ByteArray

    /** Size in bytes, or -1 when it can't be told. */
    fun sizeOf(uri: String): Long

    fun mimeTypeOf(uri: String): String?

    /** The file's bytes, streamed rather than read into memory where the platform allows. */
    fun open(uri: String): ByteReadChannel
}
