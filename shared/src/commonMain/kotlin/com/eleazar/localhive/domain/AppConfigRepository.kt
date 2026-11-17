package com.eleazar.localhive.domain

import kotlinx.coroutines.flow.Flow

/**
 * App-wide settings kept in Firestore at config/app and changed from the Firebase console:
 *
 *   minVersion  the oldest version still allowed to run, e.g. "1.07"; older ones must update
 *   androidUrl  where Android phones get the update (defaults to the Play Store listing)
 *   iosUrl      where iPhones get it (the App Store listing)
 */
interface AppConfigRepository {
    fun observeConfig(): Flow<AppConfig>
}

data class AppConfig(
    val minVersion: String = "",
    val androidUrl: String = "",
    val iosUrl: String = ""
)

/**
 * True when [installed] is older than [minimum]. Versions compare number by number ("1.10" is
 * newer than "1.9"); anything that isn't a version never blocks, so a typo can't lock everyone out.
 */
fun isOutdated(installed: String, minimum: String): Boolean {
    val have = versionParts(installed) ?: return false
    val need = versionParts(minimum) ?: return false
    for (i in 0 until maxOf(have.size, need.size)) {
        val a = have.getOrElse(i) { 0 }
        val b = need.getOrElse(i) { 0 }
        if (a != b) return a < b
    }
    return false
}

private fun versionParts(version: String): List<Int>? =
    version.trim().takeIf { it.isNotEmpty() }
        ?.split('.')
        ?.map { it.toIntOrNull() ?: return null }
