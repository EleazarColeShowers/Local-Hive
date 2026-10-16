package com.eleazar.localhive.data

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds aren't from the Play Store, so Play Integrity would refuse them. The debug
 * provider prints a secret to logcat ("Enter this debug secret into the allow list") that has to
 * be added under Firebase console > App Check > Apps > Manage debug tokens.
 */
internal fun installAppCheck() {
    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
