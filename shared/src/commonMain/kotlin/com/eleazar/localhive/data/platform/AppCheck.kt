package com.eleazar.localhive.data.platform

/**
 * A Firebase App Check token proving requests come from the genuine app, for the Worker to
 * check. Null when the platform doesn't provide one yet, or it couldn't be fetched.
 */
expect suspend fun appCheckToken(): String?
