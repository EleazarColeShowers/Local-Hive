package com.eleazar.localhive.data.remote

import com.eleazar.localhive.domain.AppSettingsRepository
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.UserRepository
import dev.gitlive.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay

/**
 * Keeps this device's FCM token on the signed-in user's profile, which is where the Worker looks
 * when it pushes. With push notifications switched off in Settings the token is taken off instead:
 * iOS draws a push itself, so the app can't drop it on arrival the way Android does.
 */
class PushTokenSync(
    private val messaging: FirebaseMessaging,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val appSettingsRepository: AppSettingsRepository
) {
    /**
     * Best-effort: a stale or missing token just means push targeting lags until the next launch.
     * FCM only reports a new token when it rotates one, which on a fresh install happens before
     * anyone has signed in, so this runs on every sign-in and launch too.
     */
    suspend fun sync(userId: String) {
        if (!appSettingsRepository.pushNotificationsEnabled.value) {
            userRepository.clearFcmToken(userId)
            return
        }
        // On iOS the token can't be issued until Apple has registered the device, which may not
        // have happened yet this early in a launch, so try again a few times before giving up.
        repeat(TOKEN_ATTEMPTS) { attempt ->
            val token = runCatching { messaging.getToken() }.getOrNull()
            if (token != null) {
                userRepository.updateFcmToken(userId, token)
                return
            }
            if (attempt < TOKEN_ATTEMPTS - 1) delay(TOKEN_RETRY_MS)
        }
    }

    private companion object {
        const val TOKEN_ATTEMPTS = 4
        const val TOKEN_RETRY_MS = 5_000L
    }

    /** FCM issued a new token for this device. */
    suspend fun onNewToken(token: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        if (appSettingsRepository.pushNotificationsEnabled.value) userRepository.updateFcmToken(userId, token)
    }
}
