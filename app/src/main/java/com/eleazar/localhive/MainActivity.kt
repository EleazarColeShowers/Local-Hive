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
import com.eleazar.localhive.data.checkInstallReferrer
import com.eleazar.localhive.data.inviteLinkFrom
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

        // Only on first creation: after a rotation the inbox (a ViewModel) still has it, or it's been used.
        if (savedInstanceState == null) {
            intent.toIncomingShare(contentResolver)?.let(shareInbox::offer)
            inviteLinkFrom(intent.data)?.let(linkInbox::offer)
            checkInstallReferrer(this, linkInbox::offer)
        }

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
                    // Invite links (https and from a Play install); localhive:// links go to the
                    // nav graph's own deep links.
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        inviteLinkFrom(intent.data)?.let(linkInbox::offer)
    }
}
