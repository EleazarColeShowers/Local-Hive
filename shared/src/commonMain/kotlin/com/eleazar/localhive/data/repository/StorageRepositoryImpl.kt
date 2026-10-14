package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.platform.MediaReader
import com.eleazar.localhive.data.remote.LocalHiveApi
import com.eleazar.localhive.data.remote.UploadSignature
import com.eleazar.localhive.data.remote.string
import com.eleazar.localhive.domain.StorageRepository
import com.eleazar.localhive.domain.UploadedVideo
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Cloudinary's free plan only resizes and re-encodes videos up to 40 MB on the fly, which is
 * what makes them stream as a light MP4. That's about two minutes of 1080p phone video.
 */
private const val MAX_VIDEO_BYTES = 40L * 1024 * 1024

/**
 * Photos live on Cloudinary's free plan (Firebase Storage needs the paid Blaze plan).
 * The Worker signs each upload after checking the user's Firebase login, so the Cloudinary
 * secret never ships in the app and only signed-in residents can upload.
 */
class StorageRepositoryImpl(
    private val api: LocalHiveApi,
    private val media: MediaReader
) : StorageRepository {

    override suspend fun uploadProfileImage(userId: String, image: String): Result<String> =
        upload(kind = "profile", image = image, maxSide = 800, delivery = "c_fill,g_face,w_400,h_400,f_auto,q_auto")

    override suspend fun uploadPostImage(userId: String, image: String): Result<String> =
        upload(kind = "post", image = image, maxSide = 1600, delivery = "c_limit,w_1280,f_auto,q_auto")

    // Kept big enough that the amounts and names on a bank screenshot stay readable.
    override suspend fun uploadReceipt(userId: String, image: String): Result<String> =
        upload(kind = "receipt", image = image, maxSide = 2000, delivery = "c_limit,w_1600,f_auto,q_auto")

    override suspend fun uploadPostVideo(userId: String, video: String, onProgress: (Float) -> Unit): Result<UploadedVideo> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!api.isConfigured) error("Video uploads aren't set up yet")
                val size = media.sizeOf(video)
                if (size > MAX_VIDEO_BYTES) {
                    error("That video is too long to share. Keep it under ${MAX_VIDEO_BYTES / (1024 * 1024)} MB (about two minutes).")
                }
                val type = media.mimeTypeOf(video) ?: "video/mp4"
                val signed = api.uploadSignature("video")

                val response = api.http.submitFormWithBinaryData(
                    url = "https://api.cloudinary.com/v1_1/${signed.cloudName}/video/upload",
                    formData = formData {
                        // Streamed from disk rather than read into memory: videos run to tens of megabytes.
                        append(
                            "file",
                            ChannelProvider(size.takeIf { it > 0 }) { media.open(video) },
                            fileHeaders("video", type)
                        )
                        appendSignature(signed)
                    }
                ) {
                    // Cloudinary only answers once it has stored the whole video, which can take a while.
                    timeout { socketTimeoutMillis = 2 * 60_000 }
                    onUpload { sent, total ->
                        val length = total ?: size
                        if (length > 0) onProgress((sent.toFloat() / length).coerceAtMost(1f))
                    }
                }
                // Without its extension, so the URL can ask Cloudinary for whichever format it needs.
                val base = secureUrl(response).substringBeforeLast('.')
                UploadedVideo(
                    // Re-encoded as a phone-sized MP4, which every Android and iOS player can stream.
                    url = base.replaceFirst("/video/upload/", "/video/upload/c_limit,w_720,q_auto/") + ".mp4",
                    thumbnailUrl = base.replaceFirst("/video/upload/", "/video/upload/so_0,c_limit,w_1280,q_auto/") + ".jpg"
                )
            }
        }

    /**
     * @param delivery Cloudinary transformation baked into the returned URL, so every screen
     *   downloads a right-sized, modern-format copy rather than the original.
     */
    private suspend fun upload(kind: String, image: String, maxSide: Int, delivery: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!api.isConfigured) error("Photo uploads aren't set up yet")
                val bytes = media.compressedJpeg(image, maxSide)
                val signed = api.uploadSignature(kind)

                val response = api.http.submitFormWithBinaryData(
                    url = "https://api.cloudinary.com/v1_1/${signed.cloudName}/image/upload",
                    formData = formData {
                        append("file", bytes, fileHeaders("photo.jpg", "image/jpeg"))
                        appendSignature(signed)
                    }
                )
                secureUrl(response).replaceFirst("/image/upload/", "/image/upload/$delivery/")
            }
        }

    private fun fileHeaders(fileName: String, type: String) = Headers.build {
        append(HttpHeaders.ContentType, type)
        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
    }

    private fun FormBuilder.appendSignature(signed: UploadSignature) {
        append("api_key", signed.apiKey)
        append("timestamp", signed.timestamp.toString())
        append("folder", signed.folder)
        append("signature", signed.signature)
    }

    private suspend fun secureUrl(response: HttpResponse): String {
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) error("Upload failed (${response.status.value})")
        return LocalHiveApi.parse(text).string("secure_url")
    }
}
