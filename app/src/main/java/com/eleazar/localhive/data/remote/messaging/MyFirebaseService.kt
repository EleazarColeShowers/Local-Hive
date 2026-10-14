package com.eleazar.localhive.data.remote.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.eleazar.localhive.MainActivity
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

        showNotification(
            title = title,
            message = body,
            deepLink = data["deepLink"] ?: DEFAULT_DEEP_LINK,
            // One entry per conversation or event rather than a single overwritten slot.
            tag = data["tag"] ?: remoteMessage.messageId ?: DEFAULT_DEEP_LINK,
            emergency = data["channel"] == EMERGENCY_CHANNEL_ID
        )
    }

    private fun showNotification(title: String?, message: String?, deepLink: String, tag: String, emergency: Boolean) {
        val channelId = if (emergency) EMERGENCY_CHANNEL_ID else "fcm_channel"

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = if (emergency) {
                // Its own channel, so people can keep SOS alerts loud while muting everything else.
                NotificationChannel(channelId, "Emergency alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "SOS alerts from neighbours in your estate"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
                }
            } else {
                NotificationChannel(channelId, "FCM Channel", NotificationManager.IMPORTANCE_HIGH)
            }
            manager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(deepLink)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        // Distinct request codes keep each deep link on its own PendingIntent.
        val pendingIntent = PendingIntent.getActivity(
            this, tag.hashCode(), contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .apply {
                if (emergency) {
                    setCategory(NotificationCompat.CATEGORY_ALARM)
                    setColor(EMERGENCY_COLOR)
                    setStyle(NotificationCompat.BigTextStyle().bigText(message))
                }
            }
            .build()

        manager.notify(tag, tag.hashCode(), notification)
    }

    private companion object {
        const val DEFAULT_DEEP_LINK = "localhive://notifications"
        /** Matches the `channel` the Worker puts on SOS pushes. */
        const val EMERGENCY_CHANNEL_ID = "emergency"
        const val EMERGENCY_COLOR = 0xFFC62828.toInt()
    }
}
