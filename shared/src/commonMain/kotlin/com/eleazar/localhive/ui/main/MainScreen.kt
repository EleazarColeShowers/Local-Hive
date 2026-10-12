package com.eleazar.localhive.ui.main

// ───────────────────────────────────────────────────────────────────────────
// SHARED MODULE  (bare UI: no Hilt, no NavController, no R.* resources)
// ───────────────────────────────────────────────────────────────────────────

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    content: @Composable (PaddingValues) -> Unit
) {
    // Slim bar slides away when the content scrolls, giving content more room.
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val edgeToEdge = tabs.firstOrNull { it.route == currentRoute }?.edgeToEdge == true

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (edgeToEdge) {
            ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        topBar = {
            if (!edgeToEdge) {
                MainTopBar(
                    unreadNotifications = unreadNotifications,
                    colors = colors,
                    onNotificationsClick = onNotificationsClick,
                    scrollBehavior = scrollBehavior
                )
            }
        },
        bottomBar = {
            MainBottomBar(
                tabs = tabs,
                currentRoute = currentRoute,
                colors = colors,
                onTabSelected = onTabSelected,
                onCreatePostClick = onCreatePostClick
            )
        },
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    unreadNotifications: Int,
    colors: MainScreenColors,
    onNotificationsClick: () -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior
) {
    TopAppBar(
        title = {}, // no title: each screen owns its own heading
        actions = {
            BadgedBox(
                badge = { CountBadge(unreadNotifications, colors) },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                IconButton(onClick = onNotificationsClick) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = if (unreadNotifications > 0) {
                            "Notifications, $unreadNotifications unread"
                        } else {
                            "Notifications"
                        },
                        tint = colors.chrome
                    )
                }
            }
        },
        // Same colour as the page so the bar blends in instead of showing as a white strip.
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        ),
        scrollBehavior = scrollBehavior
    )
}

/**
 * Bottom bar with the "create post" button docked in the centre gap,
 * instead of floating over the content as a separate FAB.
 */
@Composable
private fun MainBottomBar(
    tabs: List<MainTab>,
    currentRoute: String?,
    colors: MainScreenColors,
    onTabSelected: (MainTab) -> Unit,
    onCreatePostClick: () -> Unit
) {
    val gapIndex = tabs.size / 2

    Box(contentAlignment = Alignment.Center) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            tabs.forEachIndexed { index, tab ->
                if (index == gapIndex) Spacer(Modifier.weight(1f))
                MainNavItem(
                    tab = tab,
                    selected = currentRoute == tab.route,
                    colors = colors,
                    onClick = { onTabSelected(tab) }
                )
            }
        }

        FloatingActionButton(
            onClick = onCreatePostClick,
            shape = CircleShape,
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            ),
            modifier = Modifier.size(52.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create post")
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
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

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
        label = {
            Text(
                text = tab.label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = Modifier.semantics {
            if (tab.badgeCount > 0) contentDescription = "${tab.label}, ${tab.badgeCount} unread"
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.accent,
            selectedTextColor = colors.accent,
            indicatorColor = colors.accent.copy(alpha = 0.12f),
            unselectedIconColor = inactive,
            unselectedTextColor = inactive
        )
    )
}

@Composable
private fun CountBadge(count: Int, colors: MainScreenColors) {
    if (count <= 0) return
    Badge(containerColor = colors.accent, contentColor = colors.onAccent) {
        Text(if (count > 99) "99+" else "$count")
    }
}