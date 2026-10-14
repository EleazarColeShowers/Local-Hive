package com.eleazar.localhive

import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.data.remote.PushTokenSync
import com.eleazar.localhive.di.initKoin
import com.eleazar.localhive.domain.AppSettingsRepository
import com.eleazar.localhive.ui.LocalHiveApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController

/** The app's screen, hosted by the SwiftUI app in iosApp. */
fun MainViewController(): UIViewController = ComposeUIViewController {
    val link by LocalHiveIos.pendingLink.collectAsStateWithLifecycle()
    LocalHiveApp(pendingLink = link, onLinkHandled = { LocalHiveIos.pendingLink.value = null })
}

/** What the Swift side of the app calls into. Swift sees it as `LocalHiveIos.shared`. */
object LocalHiveIos {

    internal val pendingLink = MutableStateFlow<String?>(null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Call once at launch, after `FirebaseApp.configure()`. */
    fun start() {
        initKoin(LOCALHIVE_API_URL)
    }

    /** A localhive:// link to open: a tapped push, or the URL scheme. */
    fun openLink(url: String) {
        pendingLink.value = url
    }

    /** FCM issued this device a token, which happens once APNs has registered it. */
    fun onPushToken(token: String) {
        scope.launch { KoinPlatform.getKoin().get<PushTokenSync>().onNewToken(token) }
    }

    /** Whether to show a push that arrives while the app is open, per the switch in Settings. */
    fun pushNotificationsEnabled(): Boolean =
        KoinPlatform.getKoin().get<AppSettingsRepository>().pushNotificationsEnabled.value
}
