package com.eleazar.localhive

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.data.LinkInbox
import com.eleazar.localhive.data.appLinkFrom
import com.eleazar.localhive.data.checkInstallReferrer
import com.eleazar.localhive.ui.LocalHiveApp
import com.eleazar.localhive.ui.share.LocalShareInbox
import com.eleazar.localhive.ui.share.ShareInbox
import com.eleazar.localhive.ui.share.toIncomingShare
import com.eleazar.localhive.ui.update.InAppUpdater

class MainActivity : ComponentActivity() {

    private val shareInbox: ShareInbox by viewModels()
    private val linkInbox: LinkInbox by viewModels()
    private val updater = InAppUpdater(this)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Debug builds only: `adb shell am start -n com.eleazar.localhive/.MainActivity --ez crashTest true`
        // crashes on purpose, to check that Crashlytics reports arrive.
        if (BuildConfig.DEBUG && intent.getBooleanExtra("crashTest", false)) {
            throw RuntimeException("Crashlytics test crash")
        }

        // Only on first creation: after a rotation the inbox (a ViewModel) still has it, or it's been used.
        if (savedInstanceState == null) {
            intent.toIncomingShare(contentResolver)?.let(shareInbox::offer)
            appLinkFrom(intent.data)?.let(linkInbox::offer)
            checkInstallReferrer(this, linkInbox::offer)
        }
        // The link now waits in the inbox. Left on the intent, the nav graph would also follow it
        // itself, restarting the app with the screen on top of the splash, which Back then leaves.
        clearLink()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val pendingShare by shareInbox.pending.collectAsStateWithLifecycle()
            val pendingLink by linkInbox.pending.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalShareInbox provides shareInbox) {
                LocalHiveApp(
                    hasPendingShare = pendingShare != null,
                    // Tapped pushes and invite links, opened on top of wherever the user is.
                    pendingLink = pendingLink,
                    onLinkHandled = linkInbox::handled,
                    // Follows the in-app theme, which can differ from the phone's: dark icons on the
                    // white app, light icons in dark mode.
                    onDarkThemeChange = { darkTheme ->
                        WindowCompat.getInsetsController(window, window.decorView).apply {
                            isAppearanceLightStatusBars = !darkTheme
                            isAppearanceLightNavigationBars = !darkTheme
                        }
                    },
                    overlay = {
                        if (updater.updateReady) {
                            AlertDialog(
                                onDismissRequest = updater::postpone,
                                title = { Text("Update ready") },
                                text = { Text("A new version of LocalHive has downloaded. Restart to start using it.") },
                                confirmButton = { TextButton(onClick = updater::install) { Text("Restart") } },
                                dismissButton = { TextButton(onClick = updater::postpone) { Text("Later") } }
                            )
                        }
                    }
                )
            }
        }
    }

    /** A push tapped, or a link opened, while the app is already running. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        appLinkFrom(intent.data)?.let(linkInbox::offer)
        setIntent(intent)
        clearLink()
    }

    private fun clearLink() {
        if (intent.data != null) intent = Intent(intent).setData(null)
    }
}
