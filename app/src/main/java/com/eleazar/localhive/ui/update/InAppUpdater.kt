package com.eleazar.localhive.ui.update

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/** Play's in-app update priority (0–5, set per release) from which an update can't be skipped. */
private const val REQUIRED_PRIORITY = 4

/**
 * Keeps Play Store installs up to date. A new release downloads in the background while the
 * app is in use, then [updateReady] asks for a restart to install it. A release marked
 * high-priority in Play (in-app update priority 4 or 5) blocks the app until it's installed.
 *
 * Only builds installed from Play see updates; sideloaded and debug builds never get one.
 */
class InAppUpdater(private val activity: ComponentActivity) : DefaultLifecycleObserver {

    /** Created on first use: the activity has no context yet while it's being constructed. */
    private val manager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(activity) }

    // Play shows its own UI; the outcome arrives through the listener and on the next resume.
    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }

    /** The update has downloaded and installs once the app restarts. */
    var updateReady by mutableStateOf(false)
        private set

    private var postponed = false

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED && !postponed) updateReady = true
    }

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        manager.registerListener(listener)
        // Once per launch, so declining isn't met by the same prompt on every return to the app.
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return@addOnSuccessListener
            val type = if (info.updatePriority() >= REQUIRED_PRIORITY) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
            if (info.isUpdateTypeAllowed(type)) start(info, type)
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED && !postponed) updateReady = true
            // A required update the user left part-way through picks up where it stopped.
            // (A background download in progress reports the same, so priority tells them apart.)
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                info.updatePriority() >= REQUIRED_PRIORITY
            ) {
                start(info, AppUpdateType.IMMEDIATE)
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        manager.unregisterListener(listener)
    }

    /** Restarts the app into the downloaded version. */
    fun install() {
        manager.completeUpdate()
    }

    /** Hides the prompt until the app is next launched. */
    fun postpone() {
        postponed = true
        updateReady = false
    }

    private fun start(info: AppUpdateInfo, type: Int) {
        manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(type))
    }
}
