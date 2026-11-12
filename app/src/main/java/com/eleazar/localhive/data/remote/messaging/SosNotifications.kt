package com.eleazar.localhive.data.remote.messaging

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.eleazar.localhive.MainActivity
import com.eleazar.localhive.R

/** Opens [deepLink] in the running app (onNewIntent) rather than restarting it, so Back returns to where the user was. */
internal fun openIntent(context: Context, deepLink: String, tag: String): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse(deepLink)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    // Distinct request codes keep each deep link on its own PendingIntent.
    return PendingIntent.getActivity(
        context, tag.hashCode(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/**
 * How loud an SOS is on this phone. The Worker decides, from the person's SOS settings (sound,
 * pause) and the sender's false alarms, and sends it as `sosSound`.
 */
enum class SosLevel(val key: String) {
    SIREN("siren"), SOUND("sound"), VIBRATE("vibrate");

    companion object {
        /** A push from before the Worker sent a level gets the siren, as it always did. */
        fun fromKey(key: String?): SosLevel = entries.firstOrNull { it.key == key } ?: SIREN
    }
}

/**
 * SOS notifications. Each level has its own channel, since Android fixes a channel's sound once
 * it exists. The siren is a 30-second recording that plays once, rather than a short one on
 * repeat, so it stops by itself; Silence stops it sooner and leaves the alert showing.
 */
object SosNotifications {
    private const val SIREN_CHANNEL = "sos_siren"
    private const val SOUND_CHANNEL = "sos_sound"
    private const val VIBRATE_CHANNEL = "sos_vibrate"
    private const val SILENCED_CHANNEL = "sos_silenced"
    /** Earlier SOS channels: the very first played the default sound, the next a looping siren. */
    private val OLD_CHANNELS = listOf("emergency", "sos_alarm")
    private val VIBRATION = longArrayOf(0, 600, 300, 600, 300, 600)
    private const val COLOR = 0xFFC62828.toInt()

    internal const val EXTRA_TITLE = "title"
    internal const val EXTRA_BODY = "body"
    internal const val EXTRA_LINK = "deepLink"
    internal const val EXTRA_TAG = "tag"

    private fun siren(context: Context) = Uri.parse("android.resource://${context.packageName}/${R.raw.sos_siren}")

    fun show(context: Context, title: String?, body: String?, deepLink: String, tag: String, level: SosLevel) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(context, manager)
        val channel = when (level) {
            SosLevel.SIREN -> SIREN_CHANNEL
            SosLevel.SOUND -> SOUND_CHANNEL
            SosLevel.VIBRATE -> VIBRATE_CHANNEL
        }
        val builder = base(context, channel, title, body, deepLink, tag)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .apply {
                // Only the siren counts as an alarm, which Do Not Disturb may let through; a
                // paused or quiet phone shouldn't be woken by it.
                if (level == SosLevel.SIREN) setCategory(NotificationCompat.CATEGORY_ALARM)
                if (level != SosLevel.VIBRATE) addAction(0, "Silence", silenceIntent(context, title, body, deepLink, tag))
                // Before Android 8 there are no channels, so sound and vibration are set here.
                when (level) {
                    SosLevel.SIREN -> setSound(siren(context), AudioManager.STREAM_ALARM)
                    SosLevel.SOUND -> setDefaults(NotificationCompat.DEFAULT_SOUND)
                    SosLevel.VIBRATE -> Unit
                }
                setVibrate(VIBRATION)
            }
        manager.notify(tag, tag.hashCode(), builder.build())
    }

    /**
     * Swaps the alert for the same one on a silent channel. Android stops a notification's sound
     * when it's replaced by one that makes none.
     */
    fun silence(context: Context, title: String?, body: String?, deepLink: String, tag: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(context, manager)
        val notification = base(context, SILENCED_CHANNEL, title, body, deepLink, tag)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(tag, tag.hashCode(), notification)
    }

    private fun base(context: Context, channel: String, title: String?, body: String?, deepLink: String, tag: String) =
        NotificationCompat.Builder(context, channel)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(COLOR)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openIntent(context, deepLink, tag))
            .setAutoCancel(true)

    private fun silenceIntent(context: Context, title: String?, body: String?, deepLink: String, tag: String): PendingIntent {
        val intent = Intent(context, SosSilenceReceiver::class.java)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_BODY, body)
            .putExtra(EXTRA_LINK, deepLink)
            .putExtra(EXTRA_TAG, tag)
        return PendingIntent.getBroadcast(
            context, tag.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannels(context: Context, manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        OLD_CHANNELS.forEach(manager::deleteNotificationChannel)
        val alarm = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        manager.createNotificationChannels(
            listOf(
                // Played as an alarm, so it follows the alarm volume rather than the notification one.
                NotificationChannel(SIREN_CHANNEL, "Emergency alerts: siren", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "A siren when a neighbour in your estate sends an SOS"
                    setSound(siren(context), alarm)
                    vibrate()
                },
                NotificationChannel(SOUND_CHANNEL, "Emergency alerts: sound", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Emergency alerts with your usual notification sound"
                    vibrate()
                },
                NotificationChannel(VIBRATE_CHANNEL, "Emergency alerts: vibrate", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Emergency alerts while SOS sound is paused or set to vibrate"
                    setSound(null, null)
                    vibrate()
                },
                NotificationChannel(SILENCED_CHANNEL, "Emergency alerts: silenced", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "An emergency alert after you've tapped Silence"
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun NotificationChannel.vibrate() {
        enableVibration(true)
        vibrationPattern = VIBRATION
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    }
}

/** The Silence button on an SOS notification. */
class SosSilenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tag = intent.getStringExtra(SosNotifications.EXTRA_TAG) ?: return
        SosNotifications.silence(
            context,
            title = intent.getStringExtra(SosNotifications.EXTRA_TITLE),
            body = intent.getStringExtra(SosNotifications.EXTRA_BODY),
            deepLink = intent.getStringExtra(SosNotifications.EXTRA_LINK) ?: "localhive://notifications",
            tag = tag
        )
    }
}
