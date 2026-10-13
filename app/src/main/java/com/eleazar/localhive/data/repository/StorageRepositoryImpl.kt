package com.eleazar.localhive.data.repository

import android.content.Context
import android.net.Uri
import com.eleazar.localhive.data.remote.api.ImageCompressor
import com.eleazar.localhive.data.remote.api.LocalHiveApi
import com.eleazar.localhive.domain.StorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

/**
 * Photos live on Cloudinary's free plan (Firebase Storage needs the paid Blaze plan).
 * The Worker signs each upload after checking the user's Firebase login, so the Cloudinary
 * secret never ships in the app and only signed-in residents can upload.
 */
class StorageRepositoryImpl @Inject constructor(
    private val api: LocalHiveApi,
    @ApplicationContext private val context: Context
) : StorageRepository {

    override suspend fun uploadProfileImage(userId: String, image: Uri): Result<String> =
        upload(kind = "profile", image = image, maxSide = 800, delivery = "c_fill,g_face,w_400,h_400,f_auto,q_auto")

    override suspend fun uploadPostImage(userId: String, image: Uri): Result<String> =
        upload(kind = "post", image = image, maxSide = 1600, delivery = "c_limit,w_1280,f_auto,q_auto")

    /**
     * @param delivery Cloudinary transformation baked into the returned URL, so every screen
     *   downloads a right-sized, modern-format copy rather than the original.
     */
    private suspend fun upload(kind: String, image: Uri, maxSide: Int, delivery: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!api.isConfigured) error("Photo uploads aren't set up yet")
                val bytes = ImageCompressor.compress(context, image, maxSide)
                val signed = api.uploadSignature(kind)

                val form = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", "photo.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
                    .addFormDataPart("api_key", signed.apiKey)
                    .addFormDataPart("timestamp", signed.timestamp.toString())
                    .addFormDataPart("folder", signed.folder)
                    .addFormDataPart("signature", signed.signature)
                    .build()
                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/${signed.cloudName}/image/upload")
                    .post(form)
                    .build()

                api.http.newCall(request).execute().use { response ->
                    val text = response.body?.string().orEmpty()
                    if (!response.isSuccessful) error("Upload failed (${response.code})")
                    val url = JSONObject(text).getString("secure_url")
                    url.replaceFirst("/image/upload/", "/image/upload/$delivery/")
                }
            }
        }
}
