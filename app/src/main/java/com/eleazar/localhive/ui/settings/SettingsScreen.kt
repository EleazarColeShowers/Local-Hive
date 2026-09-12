package com.eleazar.localhive.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eleazar.localhive.R
import com.eleazar.localhive.domain.ThemeMode

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onLeftEstate: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showLeaveConfirm by remember { mutableStateOf(false) }

    val hiveGreen = colorResource(id = R.color.hivegreen)
    val backgroundGold = colorResource(id = R.color.backgroundgold)
    val lightGold = colorResource(id = R.color.lightgold)
    val darkGray = colorResource(id = R.color.darkgray)

    LaunchedEffect(uiState.isLoggedOut) { if (uiState.isLoggedOut) onLoggedOut() }
    LaunchedEffect(uiState.hasLeftEstate) { if (uiState.hasLeftEstate) onLeftEstate() }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave Estate?", fontWeight = FontWeight.Bold) },
            text = { Text("You'll need a new invite code to re-join ${uiState.estate?.name ?: "this estate"}.") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveConfirm = false
                    viewModel.leaveEstate()
                }) {
                    Text("Leave", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGold)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = darkGray)
            }
            Text(
                "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = darkGray,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            SettingsSection(title = "Appearance", lightGold = lightGold, darkGray = darkGray) {
                ThemeModeRow(
                    label = "System default",
                    selected = uiState.themeMode == ThemeMode.SYSTEM,
                    hiveGreen = hiveGreen,
                    darkGray = darkGray,
                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                )
                SettingsDivider(darkGray)
                ThemeModeRow(
                    label = "Light",
                    selected = uiState.themeMode == ThemeMode.LIGHT,
                    hiveGreen = hiveGreen,
                    darkGray = darkGray,
                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                )
                SettingsDivider(darkGray)
                ThemeModeRow(
                    label = "Dark",
                    selected = uiState.themeMode == ThemeMode.DARK,
                    hiveGreen = hiveGreen,
                    darkGray = darkGray,
                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                )
            }

            Spacer(Modifier.height(16.dp))

            SettingsSection(title = "Notifications", lightGold = lightGold, darkGray = darkGray) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Notifications, null, tint = hiveGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "Push notifications",
                        fontSize = 14.sp,
                        color = darkGray,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = uiState.pushNotificationsEnabled,
                        onCheckedChange = { viewModel.setPushNotificationsEnabled(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = hiveGreen)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsSection(title = "Account", lightGold = lightGold, darkGray = darkGray) {
                uiState.user?.email?.let { email ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Email, null, tint = hiveGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(email, fontSize = 14.sp, color = darkGray)
                    }
                    if (uiState.estate != null) SettingsDivider(darkGray)
                }
                if (uiState.estate != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLeaveConfirm = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "Leave Estate",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsSection(title = "About", lightGold = lightGold, darkGray = darkGray) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = hiveGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Text("App version", fontSize = 14.sp, color = darkGray, modifier = Modifier.weight(1f))
                    Text(versionName, fontSize = 13.sp, color = darkGray.copy(alpha = 0.5f))
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = lightGold),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.logout() }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ExitToApp, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Log Out", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error, fontSize = 15.sp)
                }
            }

            uiState.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    lightGold: Color,
    darkGray: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = darkGray.copy(alpha = 0.5f),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = lightGold),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsDivider(darkGray: Color) {
    HorizontalDivider(color = darkGray.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun ThemeModeRow(
    label: String,
    selected: Boolean,
    hiveGreen: Color,
    darkGray: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = darkGray, modifier = Modifier.weight(1f))
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = hiveGreen)
        )
    }
}
