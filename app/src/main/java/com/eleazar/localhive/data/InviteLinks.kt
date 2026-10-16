package com.eleazar.localhive.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.eleazar.localhive.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "InviteLinks"
private val INVITE_CODE = Regex("[A-Za-z0-9]{4,12}")

/**
 * A localhive:// link waiting for the nav graph to follow it once the user is signed in: an
 * invite link that opened the app, or an invite code carried through a Play Store install.
 */
class LinkInbox : ViewModel() {
    private val _pending = MutableStateFlow<String?>(null)
    val pending = _pending.asStateFlow()

    fun offer(link: String) {
        _pending.value = link
    }

    fun handled() {
        _pending.value = null
    }
}

/** "localhive://join/CODE" for an https://<invite host>/join/CODE link, or null for anything else. */
fun inviteLinkFrom(uri: Uri?): String? {
    if (uri == null || uri.scheme != "https" || uri.host != BuildConfig.INVITE_HOST) return null
    val segments = uri.pathSegments
    if (segments.size != 2 || segments[0] != "join") return null
    return joinLink(segments[1])
}

private fun joinLink(code: String): String? =
    code.takeIf { INVITE_CODE.matches(it) }?.let { "localhive://join/${it.uppercase()}" }

/**
 * Once per install: the invite code from the /join link that sent someone to the Play Store,
 * which Play hands back as the install referrer "invite=CODE". Installs from anywhere else, and
 * every later launch, find nothing.
 */
fun checkInstallReferrer(context: Context, onInvite: (String) -> Unit) {
    val prefs = context.getSharedPreferences("install_referrer", Context.MODE_PRIVATE)
    if (prefs.getBoolean("checked", false)) return
    val client = InstallReferrerClient.newBuilder(context.applicationContext).build()
    runCatching {
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        runCatching { client.installReferrer.installReferrer }
                            .onSuccess { referrer -> inviteFromReferrer(referrer)?.let(onInvite) }
                            .onFailure { Log.w(TAG, "Couldn't read the install referrer", it) }
                        prefs.edit().putBoolean("checked", true).apply()
                    }
                    // No Play Store on this device (or a sideloaded build): nothing will ever come.
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED ->
                        prefs.edit().putBoolean("checked", true).apply()
                    // Otherwise Play is busy or unavailable; try again on the next launch.
                }
                client.endConnection()
            }

            override fun onInstallReferrerServiceDisconnected() = Unit
        })
    }.onFailure { Log.w(TAG, "Couldn't reach the install referrer service", it) }
}

/** The join link for a referrer like "invite=ABC123&utm_source=…", or null. */
internal fun inviteFromReferrer(referrer: String?): String? =
    referrer.orEmpty().split('&')
        .map { it.split('=', limit = 2) }
        .firstOrNull { it.size == 2 && it[0] == "invite" }
        ?.let { joinLink(Uri.decode(it[1])) }
