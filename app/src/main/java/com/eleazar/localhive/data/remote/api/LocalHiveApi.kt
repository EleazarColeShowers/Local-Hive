package com.eleazar.localhive.data.remote.api

import android.util.Log
import com.eleazar.localhive.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LocalHiveApi"

/** Signed-upload parameters for one Cloudinary upload, issued by the Worker. */
data class UploadSignature(
    val cloudName: String,
    val apiKey: String,
    val folder: String,
    val timestamp: Long,
    val signature: String
)

/** The Worker refused a request; [message] is its reason. */
class ApiException(val code: Int, message: String) : Exception(message)

/**
 * Client for the LocalHive Cloudflare Worker (see /worker), which does what Cloud Functions
 * would on Firebase's paid plan. Every call carries the user's Firebase ID token.
 */
@Singleton
class LocalHiveApi @Inject constructor(
    private val auth: FirebaseAuth,
    val http: OkHttpClient
) {
    private val baseUrl = BuildConfig.LOCALHIVE_API_URL.trimEnd('/')

    /** Pushes outlive the screen that triggered them, so they run on their own scope. */
    private val pushScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val isConfigured: Boolean get() = baseUrl.isNotBlank()

    suspend fun uploadSignature(kind: String): UploadSignature {
        val body = post("/upload-signature", JSONObject().put("kind", kind))
        return UploadSignature(
            cloudName = body.getString("cloudName"),
            apiKey = body.getString("apiKey"),
            folder = body.getString("folder"),
            timestamp = body.getLong("timestamp"),
            signature = body.getString("signature")
        )
    }

    /**
     * Deletes the signed-in user's account and everything they made. The Worker only accepts this
     * shortly after the password was entered, so call it straight after reauthenticating.
     */
    suspend fun deleteAccount() {
        post("/delete-account", JSONObject(), freshToken = true)
    }

    /**
     * Asks the Worker to push about something just written. Best-effort and fire-and-forget:
     * the write already succeeded, and a missed push must never surface as a failure.
     */
    fun notifyInBackground(vararg fields: Pair<String, String>) {
        if (!isConfigured) return
        pushScope.launch {
            runCatching { post("/notify", JSONObject(fields.toMap())) }
                .onFailure { Log.w(TAG, "Push request failed for ${fields.toMap()}", it) }
        }
    }

    private suspend fun post(path: String, json: JSONObject, freshToken: Boolean = false): JSONObject = withContext(Dispatchers.IO) {
        check(isConfigured) { "The LocalHive API URL isn't set (localhive.apiUrl in local.properties)" }
        val user = auth.currentUser ?: error("Not signed in")
        val token = user.getIdToken(freshToken).await().token ?: error("No ID token")
        val request = Request.Builder()
            .url(baseUrl + path)
            .header("Authorization", "Bearer $token")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                // The Worker explains refusals in {"error": "..."}; those are written for people.
                val reason = runCatching { JSONObject(text).getString("error") }.getOrNull()
                throw ApiException(response.code, reason ?: "$path failed (${response.code})")
            }
            JSONObject(text)
        }
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
