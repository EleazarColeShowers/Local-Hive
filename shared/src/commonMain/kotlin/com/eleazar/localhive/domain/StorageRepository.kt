package com.eleazar.localhive.domain

/**
 * Media is passed as the platform's URI string for a picked file: a content:// URI on Android,
 * a file:// URL on iOS. Each platform's implementation knows how to read its own.
 */
interface StorageRepository {
    /** Uploads an avatar for [userId] and returns its public download URL. */
    suspend fun uploadProfileImage(userId: String, image: String): Result<String>

    /** Uploads a photo attached to a post by [userId] and returns its public download URL. */
    suspend fun uploadPostImage(userId: String, image: String): Result<String>

    /** Uploads a proof-of-payment screenshot or an expense receipt and returns its URL. */
    suspend fun uploadReceipt(userId: String, image: String): Result<String>

    /**
     * Uploads a video attached to a post by [userId], reporting progress from 0 to 1 as it goes.
     * Fails with a readable reason when the video is over the size limit.
     */
    suspend fun uploadPostVideo(userId: String, video: String, onProgress: (Float) -> Unit = {}): Result<UploadedVideo>
}

/** Where an uploaded video plays from, and a still of its first frame for the feed. */
data class UploadedVideo(val url: String, val thumbnailUrl: String)
