package com.eleazar.localhive.data.repository

import android.content.Context
import android.net.Uri
import com.eleazar.localhive.domain.StorageRepository
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : StorageRepository {

    override suspend fun uploadProfileImage(userId: String, image: Uri): Result<String> =
        upload("profileImages", userId, image)

    override suspend fun uploadPostImage(userId: String, image: Uri): Result<String> =
        upload("postImages", userId, image)

    /**
     * Paths are namespaced by uploader — `<folder>/<userId>/<uuid>` — which is what lets
     * storage.rules tie a file to its owner without a lookup.
     */
    private suspend fun upload(folder: String, userId: String, image: Uri): Result<String> {
        return try {
            val extension = image.extension() ?: "jpg"
            val ref = storage.reference.child("$folder/$userId/${UUID.randomUUID()}.$extension")

            // Set explicitly rather than relying on inference: storage.rules rejects
            // anything that is not image/*, and an unresolved type would read as binary.
            val metadata = storageMetadata {
                contentType = context.contentResolver.getType(image) ?: "image/jpeg"
            }

            ref.putFile(image, metadata).await()
            Result.success(ref.downloadUrl.await().toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Uri.extension(): String? =
        context.contentResolver.getType(this)
            ?.substringAfterLast('/', "")
            ?.takeIf { it.isNotBlank() }
}
