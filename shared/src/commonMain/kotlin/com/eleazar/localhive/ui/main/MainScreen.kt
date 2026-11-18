package com.eleazar.localhive.ui.main

// ───────────────────────────────────────────────────────────────────────────
// SHARED MODULE  (bare UI: no Hilt, no NavController, no R.* resources)
// ───────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.eleazar.localhive.ui.components.LocalHeaderActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** One bottom-navigation destination. Routes are plain strings so this module doesn't depend on app navigation. */
@Immutable
data class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeCount: Int = 0,
    /** True when the screen draws its own header behind the status bar (e.g. Profile); the shared top bar is hidden. */
    val edgeToEdge: Boolean = false
)

/** Brand colours injected by the host app so this module needs no Android resources. */
@Immutable
data class MainScreenColors(
    val accent: Color,
    val chrome: Color,
    val onAccent: Color = Color.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    tabs: List<MainTab>,
    currentRoute: String?,
    unreadNotifications: Int,
    colors: MainScreenColors,
    onTabSelected: (MainTab) -> Unit,
    onNotificationsClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** No working internet: a strip above the tabs says so, so nothing looks broken. */
    offline: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    val edgeToEdge = tabs.firstOrNull { it.route == currentRoute }?.edgeToEdge == true

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (edgeToEdge) {
            ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        bottomBar = {
            Column {
            AnimatedVisibility(visible = offline, enter = expandVertically(), exit = shrinkVertically()) {
                OfflineStrip()
            }
            MainBottomBar(
                tabs = tabs,
                currentRoute = currentRoute,
                colors = colors,
                onTabSelected = onTabSelected,
                onCreatePostClick = onCreatePostClick
            )
            }
        },
        content = { padding ->
            // Each tab's ScreenHeader shows the bell beside its title.
            CompositionLocalProvider(
                LocalHeaderActions provides {
                    NotificationsBell(unreadNotifications, colors, onNotificationsClick)
                }
            ) {
                content(padding)
            }
        }
    )
}

/** Quiet grey line above the tabs while there's no internet; what's on screen is the saved copy. */
@Composable
private fun OfflineStrip() {
    Surface(color = MaterialTheme.colorScheme.inverseSurface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "You're offline. Showing what's saved on your phone.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    }
}

@Composable
private fun NotificationsBell(
    unreadNotifications: Int,
    colors: MainScreenColors,
    onNotificationsClick: () -> Unit
) {
    IconButton(onClick = onNotificationsClick) {
        BadgedBox(badge = { CountBadge(unreadNotifications, colors) }) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = if (unreadNotifications > 0) {
                    "Notifications, $unreadNotifications unread"
                } else {
                    "Notifications"
                },
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

/**
 * Standard tab bar with "Post" in the middle. Unlike the tabs it opens the composer over
 * everything rather than switching tab, so it's never shown as selected.
 */
@Composable
private fun MainBottomBar(
    tabs: List<MainTab>,
    currentRoute: String?,
    colors: MainScreenColors,
    onTabSelected: (MainTab) -> Unit,
    onCreatePostClick: () -> Unit
) {
    val postIndex = tabs.size / 2
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant

    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            tabs.forEachIndexed { index, tab ->
                if (index == postIndex) {
                    NavigationBarItem(
                        selected = false,
                        onClick = onCreatePostClick,
                        icon = { Icon(Icons.Outlined.AddBox, contentDescription = null) },
                        label = { NavLabel("Post", selected = false) },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = inactive,
                            unselectedTextColor = inactive
                        )
                    )
                }
                MainNavItem(
                    tab = tab,
                    selected = currentRoute == tab.route,
                    colors = colors,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.MainNavItem(
    tab: MainTab,
    selected: Boolean,
    colors: MainScreenColors,
    onClick: () -> Unit
) {
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant

    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            BadgedBox(badge = { CountBadge(tab.badgeCount, colors) }) {
                Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.icon,
                    contentDescription = null // label below already describes the tab
                )
            }
        },
        label = { NavLabel(tab.label, selected) },
        modifier = Modifier.semantics {
            if (tab.badgeCount > 0) contentDescription = "${tab.label}, ${tab.badgeCount} unread"
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.accent,
            selectedTextColor = colors.accent,
            // No pill behind the chosen tab: its filled icon and colour are enough.
            indicatorColor = Color.Transparent,
            unselectedIconColor = inactive,
            unselectedTextColor = inactive
        )
    )
}

/**
 * A tab's name, always on one line. Six tabs share a narrow phone's width, so with a large system
 * font "Messages" used to wrap; the text stops growing past a modest font scale instead.
 */
@Composable
private fun NavLabel(text: String, selected: Boolean) {
    val style = MaterialTheme.typography.labelMedium
    val fontScale = LocalDensity.current.fontScale
    val maxScale = 1.15f
    Text(
        text = text,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        style = style,
        fontSize = if (fontScale > maxScale) style.fontSize * (maxScale / fontScale) else style.fontSize,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip
    )
}

@Composable
private fun CountBadge(count: Int, colors: MainScreenColors) {
    if (count <= 0) return
    Badge(containerColor = colors.accent, contentColor = colors.onAccent) {
        Text(if (count > 99) "99+" else "$count")
    }
}