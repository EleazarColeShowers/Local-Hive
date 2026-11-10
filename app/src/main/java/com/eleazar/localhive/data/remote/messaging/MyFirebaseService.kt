package com.eleazar.localhive.data.remote.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.eleazar.localhive.R
import com.eleazar.localhive.data.remote.PushTokenSync
import com.eleazar.localhive.domain.AppSettingsRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val appSettingsRepository: AppSettingsRepository by inject()
    private val pushTokenSync: PushTokenSync by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch { pushTokenSync.onNewToken(token) }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (!appSettingsRepository.pushNotificationsEnabled.value) return

        // The backend sends data-only payloads so this runs in the background too and the
        // in-app toggle above is always honoured. A `notification` block would instead be
        // drawn by the system tray while the app is backgrounded, bypassing the check.
        val data = remoteMessage.data
        val title = data["title"] ?: remoteMessage.notification?.title
        val body = data["body"] ?: remoteMessage.notification?.body
        if (title == null && body == null) return

        val deepLink = data["deepLink"] ?: DEFAULT_DEEP_LINK
        // One entry per conversation or event rather than a single overwritten slot.
        val tag = data["tag"] ?: remoteMessage.messageId ?: DEFAULT_DEEP_LINK
        if (data["channel"] == EMERGENCY_PUSH_CHANNEL) {
            // The Worker picks how loud, from this person's SOS settings and the sender's false alarms.
            SosNotifications.show(this, title, body, deepLink, tag, SosLevel.fromKey(data["sosSound"]))
        } else {
            showNotification(title, body, deepLink, tag)
        }
    }

    private fun showNotification(title: String?, message: String?, deepLink: String, tag: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "FCM Channel", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent(this, deepLink, tag))
            .setAutoCancel(true)
            .build()
        manager.notify(tag, tag.hashCode(), notification)
    }

    private companion object {
        const val DEFAULT_DEEP_LINK = "localhive://notifications"
        const val CHANNEL_ID = "fcm_channel"
        /** The `channel` the Worker puts on SOS pushes. */
        const val EMERGENCY_PUSH_CHANNEL = "emergency"
    }
}
