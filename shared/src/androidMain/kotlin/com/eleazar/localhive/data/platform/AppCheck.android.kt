package com.eleazar.localhive.data.platform

import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// The provider (Play Integrity, or the debug one in debug builds) is installed by the app at startup.
actual suspend fun appCheckToken(): String? = suspendCancellableCoroutine { continuation ->
    FirebaseAppCheck.getInstance().getAppCheckToken(false).addOnCompleteListener { task ->
        if (!task.isSuccessful) logWarning("AppCheck", "No App Check token", task.exception)
        continuation.resume(task.result?.token.takeIf { task.isSuccessful })
    }
}
