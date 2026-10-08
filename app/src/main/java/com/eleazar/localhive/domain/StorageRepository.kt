package com.eleazar.localhive.domain

import android.net.Uri

interface StorageRepository {
    /** Uploads an avatar for [userId] and returns its public download URL. */
    suspend fun uploadProfileImage(userId: String, image: Uri): Result<String>

    /** Uploads a photo attached to a post by [userId] and returns its public download URL. */
    suspend fun uploadPostImage(userId: String, image: Uri): Result<String>
}
