package com.eleazar.localhive.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.eleazar.localhive.ui.components.LeaveRole
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.R

/**
 * App-module entry point: owns the ViewModel, photo picker, clipboard and share intent,
 * and feeds the shared, platform-free [ProfileScreen].
 */
@Composable
fun ProfileRoute(
    onSettings: () -> Unit = {},
    onAdminPanel: (estateId: String) -> Unit = {},
    onLeftEstate: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.hasLeftEstate) { if (uiState.hasLeftEstate) onLeftEstate() }
    val context = LocalContext.current
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { pickedImageUri = it } }

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
            leaveRole = LeaveRole.of(it, user?.id)
        )
    }

    ProfileScreen(
        user = userUi,
        estate = estateUi,
        isLoading = uiState.isLoading,
        error = uiState.error,
        pickedImage = pickedImageUri,
        colors = colors,
        onPickPhoto = {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onEditModeChange = { editing -> if (!editing) pickedImageUri = null },
        onSave = { name, username, bio ->
            viewModel.updateProfile(name, username, bio, pickedImageUri)
        },
        onCopyInviteCode = { code ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", code))
            Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
        },
        onShareInviteCode = { code, estateName ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Join $estateName on LocalHive! Use code: $code")
            }
            context.startActivity(Intent.createChooser(intent, "Share Invite Code"))
        },
        onSettings = onSettings,
        onAdminPanel = onAdminPanel,
        onLeaveEstate = viewModel::leaveEstate
    )
}