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
     * Shrinks a video attached to a post by [userId], then uploads it, reporting each stage's
     * progress from 0 to 1 as it goes. Fails with a readable reason when the video is too long.
     */
    suspend fun uploadPostVideo(
        userId: String,
        video: String,
        onProgress: (VideoUploadStage, Float) -> Unit = { _, _ -> }
    ): Result<UploadedVideo>
}

/** Making the smaller copy on the phone, then sending it. */
enum class VideoUploadStage { PREPARING, UPLOADING }

/** Where an uploaded video plays from, and a still of its first frame for the feed. */
data class UploadedVideo(val url: String, val thumbnailUrl: String)
