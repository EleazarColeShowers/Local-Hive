package com.eleazar.localhive.ui.navigation

/**
 * The route a localhive:// link opens, matching the deep links [LocalHiveNavGraph] declares,
 * or null for a link the app doesn't know.
 */
fun routeForLink(link: String): String? {
    val parts = link.substringAfter("://", "").trim('/').split('/').filter { it.isNotBlank() }
    val id = parts.getOrNull(1)
    return when (parts.firstOrNull()) {
        "post" -> id?.let(Screen.PostDetail::createRoute)
        "chat" -> id?.let(Screen.ChatDetail::createRoute)
        "levy" -> id?.let(Screen.LevyDetail::createRoute)
        "join" -> Screen.JoinEstate.createRoute(id.orEmpty())
        "notifications" -> Screen.Notifications.route
        "polls" -> Screen.Polls.route
        "utilities" -> Screen.Utilities.route
        else -> null
    }
}
