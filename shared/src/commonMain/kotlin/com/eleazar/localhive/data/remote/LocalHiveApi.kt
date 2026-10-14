package com.eleazar.localhive.data.remote

import com.eleazar.localhive.data.platform.logWarning
import dev.gitlive.firebase.auth.FirebaseAuth
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

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
 *
 * @param baseUrl the Worker's address; blank leaves the app running without uploads or pushes.
 */
class LocalHiveApi(
    private val auth: FirebaseAuth,
    val http: HttpClient,
    baseUrl: String
) {
    private val baseUrl = baseUrl.trimEnd('/')

    /** Pushes outlive the screen that triggered them, so they run on their own scope. */
    private val pushScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val isConfigured: Boolean get() = baseUrl.isNotBlank()

    suspend fun uploadSignature(kind: String): UploadSignature {
        val body = post("/upload-signature", mapOf("kind" to kind))
        return UploadSignature(
            cloudName = body.string("cloudName"),
            apiKey = body.string("apiKey"),
            folder = body.string("folder"),
            timestamp = body.getValue("timestamp").jsonPrimitive.long,
            signature = body.string("signature")
        )
    }

    /**
     * Deletes the signed-in user's account and everything they made. The Worker only accepts this
     * shortly after the password was entered, so call it straight after reauthenticating.
     */
    suspend fun deleteAccount() {
        post("/delete-account", emptyMap(), freshToken = true)
    }

    /**
     * Asks the Worker to push about something just written. Best-effort and fire-and-forget:
     * the write already succeeded, and a missed push must never surface as a failure.
     */
    fun notifyInBackground(vararg fields: Pair<String, String>) {
        if (!isConfigured) return
        pushScope.launch {
            runCatching { post("/notify", fields.toMap()) }
                .onFailure { logWarning(TAG, "Push request failed for ${fields.toMap()}", it) }
        }
    }

    /**
     * Asks the Worker to push now and waits for the answer, for pushes the user is waiting on
     * (such as a levy reminder). Returns how many people it reached.
     */
    suspend fun notifyNow(vararg fields: Pair<String, String>): Int {
        val body = post("/notify", fields.toMap())
        return body["sent"]?.jsonPrimitive?.intOrNull ?: 0
    }

    private suspend fun post(path: String, fields: Map<String, String>, freshToken: Boolean = false): JsonObject {
        check(isConfigured) { "The LocalHive API URL isn't set" }
        val user = auth.currentUser ?: error("Not signed in")
        val token = user.getIdToken(freshToken) ?: error("No ID token")
        val json = JsonObject(fields.mapValues { JsonPrimitive(it.value) })
        val response = http.post(baseUrl + path) {
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(TextContent(json.toString(), ContentType.Application.Json))
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            // The Worker explains refusals in {"error": "..."}; those are written for people.
            val reason = runCatching { parse(text)["error"]?.jsonPrimitive?.contentOrNull }.getOrNull()
            throw ApiException(response.status.value, reason ?: "$path failed (${response.status.value})")
        }
        return parse(text)
    }

    companion object {
        fun defaultClient(): HttpClient = HttpClient {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }

        internal fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject
    }
}

internal fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content
