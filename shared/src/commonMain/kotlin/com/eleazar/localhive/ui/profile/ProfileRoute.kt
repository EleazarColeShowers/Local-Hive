package com.eleazar.localhive.ui.profile

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.ui.platform.MediaKind
import com.eleazar.localhive.ui.platform.rememberMediaPicker
import com.eleazar.localhive.ui.platform.rememberPlatformActions
import org.koin.compose.viewmodel.koinViewModel

/**
 * Entry point: owns the ViewModel, photo picker, clipboard and share sheet, and feeds the
 * platform-free [ProfileScreen].
 */
@Composable
fun ProfileRoute(
    onSettings: () -> Unit = {},
    onAdminPanel: (estateId: String) -> Unit = {},
    onFinances: () -> Unit = {},
    onPolls: () -> Unit = {},
    onUtilities: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val platform = rememberPlatformActions()
    var pickedImageUri by remember { mutableStateOf<String?>(null) }

    val pickPhoto = rememberMediaPicker(MediaKind.IMAGE) { pickedImageUri = it.uri }

    val colors = com.eleazar.localhive.ui.profile.rememberProfileColors()

    val user = uiState.user
    val estate = uiState.estate

    val userUi = user?.let {
        ProfileUserUi(
            displayName = it.displayName,
            username = it.username,
            bio = it.bio,
            profileImageUrl = it.profileImageUrl,
            email = it.email,
            address = it.address,
            phone = it.phone,
            occupation = it.occupation
        )
    }

    val estateUi = estate?.let {
        ProfileEstateUi(
            id = it.id,
            name = it.name,
            address = it.address,
            inviteCode = it.inviteCode,
            memberCount = it.memberCount,
            isAdmin = it.isAdmin(user?.id),
            isOwner = it.isOwner(user?.id),
            isTreasurer = it.isTreasurer(user?.id)
        )
    }

    ProfileScreen(
        user = userUi,
        estate = estateUi,
        isLoading = uiState.isLoading,
        error = uiState.error,
        pickedImage = pickedImageUri,
        colors = colors,
        onPickPhoto = pickPhoto,
        onEditModeChange = { editing -> if (!editing) pickedImageUri = null },
        onSave = { name, username, bio ->
            viewModel.updateProfile(name, username, bio, pickedImageUri)
        },
        onCopyInviteCode = { code -> platform.copyToClipboard("Invite Code", code, "Code copied!") },
        onShareInviteCode = { code, estateName ->
            platform.shareText("Join $estateName on LocalHive! Use code: $code", "Share Invite Code")
        },
        onSettings = onSettings,
        onAdminPanel = onAdminPanel,
        onFinances = onFinances,
        onPolls = onPolls,
        onUtilities = onUtilities
    )
}