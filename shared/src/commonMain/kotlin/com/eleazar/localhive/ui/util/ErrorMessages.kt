package com.eleazar.localhive.ui.util

import com.eleazar.localhive.data.remote.ApiException
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.FirebaseTooManyRequestsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.code

const val OfflineMessage = "Couldn't connect. Check your internet connection and try again."
const val GenericErrorMessage = "Something went wrong. Please try again."

// Words that only show up in errors from the network stack (Ktor, OkHttp, NSURLSession, gRPC).
private val NetworkHints = listOf(
    "timeout", "timed out", "unable to resolve host", "failed to connect", "connection",
    "network", "offline", "socket", "unreachable", "no address associated", "host lookup",
    "ssl", "handshake", "unknownhost", "connectexception", "broken pipe", "deadline_exceeded", "unavailable",
)

// Signs that a message was written for developers, not people.
private val TechnicalHints = listOf(
    "exception", "[url=", "://", "http", "null", "permission_denied", "failed (", "firestore",
    "firebase", "kotlin", "java.", "nsurl", "error domain", "code=", "status=",
)

/**
 * Turns any failure into a sentence a resident can act on. Messages written for people (the
 * ones repositories throw on purpose, like "That invite code isn't valid") pass through as they
 * are; network failures, Firebase codes and stack-trace text are swapped for plain language.
 */
fun Throwable.friendlyMessage(fallback: String = GenericErrorMessage): String {
    val chain = generateSequence(this) { it.cause }.take(8).toList()

    chain.forEach { e ->
        when (e) {
            is FirebaseNetworkException -> return OfflineMessage
            is FirebaseTooManyRequestsException -> return "Too many tries. Wait a moment and try again."
            is FirebaseAuthWeakPasswordException -> return "Pick a stronger password, at least 6 characters."
            is FirebaseAuthUserCollisionException -> return "There's already an account with that email. Try logging in."
            is FirebaseAuthInvalidUserException -> return "We couldn't find an account with that email."
            is FirebaseAuthInvalidCredentialsException -> return "That email or password isn't right."
            is FirebaseFirestoreException -> return when (e.code) {
                FirestoreExceptionCode.UNAVAILABLE, FirestoreExceptionCode.DEADLINE_EXCEEDED -> OfflineMessage
                FirestoreExceptionCode.PERMISSION_DENIED, FirestoreExceptionCode.UNAUTHENTICATED ->
                    "You don't have permission to do that."
                FirestoreExceptionCode.NOT_FOUND -> "That's no longer available."
                FirestoreExceptionCode.RESOURCE_EXHAUSTED -> "We're busy right now. Try again in a little while."
                else -> fallback
            }
            is ApiException -> {
                val text = e.message.orEmpty()
                return if (e.code >= 500 || text.isBlank() || text.looksTechnical()) fallback else text
            }
        }
    }

    val texts = chain.mapNotNull { it.message?.trim()?.takeIf(String::isNotEmpty) }
    if (texts.any { it.looksLikeNetwork() }) return OfflineMessage
    // A Ktor/IO class name with no message (e.g. a bare SocketTimeoutException) still means offline.
    if (chain.any { it::class.simpleName.orEmpty().looksLikeNetwork() }) return OfflineMessage

    val own = texts.firstOrNull() ?: return fallback
    return if (own.looksTechnical() || own.length > 160) fallback else own
}

private fun String.looksLikeNetwork(): Boolean {
    val lower = lowercase()
    return NetworkHints.any { it in lower }
}

private fun String.looksTechnical(): Boolean {
    val lower = lowercase()
    return TechnicalHints.any { it in lower }
}
