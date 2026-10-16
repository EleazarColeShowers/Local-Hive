package com.eleazar.localhive.data.platform

// iOS isn't released yet; App Attest gets wired up before it is. Until then the Worker must not
// require App Check, or iOS builds will be refused.
actual suspend fun appCheckToken(): String? = null
