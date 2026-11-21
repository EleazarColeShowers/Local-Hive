package com.eleazar.localhive.ui.feed

import com.eleazar.localhive.data.repository.PostDraft
import com.eleazar.localhive.data.repository.PostDraftStore
import com.eleazar.localhive.domain.AuthRepository
import org.koin.compose.koinInject
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.ui.platform.MediaKind
import com.eleazar.localhive.ui.platform.rememberMediaPicker
import com.eleazar.localhive.ui.platform.rememberVideoFrame
import com.eleazar.localhive.ui.share.LocalShareInbox
import org.koin.compose.viewmodel.koinViewModel

/** Entry point: owns the ViewModel, the system media picker, and incoming shares. */
@Composable
fun CreatePostRoute(
    onNavigateBack: () -> Unit,
    viewModel: FeedViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val inbox = LocalShareInbox.current
    // Taken once; after that the saved state below carries it across rotation.
    val share = remember { inbox?.take() }
    val drafts: PostDraftStore = koinInject()
    val auth: AuthRepository = koinInject()
    val userId = remember { auth.getCurrentUserId() }
    // Something shared into the app starts a fresh post; otherwise pick up an unfinished one.
    val draft = remember { if (share == null) userId?.let(drafts::load) else null }
    var mediaUri by rememberSaveable { mutableStateOf(share?.media) }
    var isVideo by rememberSaveable { mutableStateOf(share?.isVideo == true) }

    val pickMedia = rememberMediaPicker(MediaKind.IMAGE_OR_VIDEO) { picked ->
        mediaUri = picked.uri
        isVideo = picked.isVideo
    }

    // Coil only reads photos, so a video is previewed by its first frame.
    val videoFrame = rememberVideoFrame(mediaUri?.takeIf { isVideo })

    LaunchedEffect(uiState.postCreated) {
        if (uiState.postCreated) {
            userId?.let(drafts::clear)
            viewModel.consumePostCreated()
            onNavigateBack()
        }
    }

    CreatePostScreen(
        uiState = uiState,
        pickedMedia = if (isVideo) videoFrame else mediaUri,
        pickedIsVideo = isVideo && mediaUri != null,
        initialContent = share?.text ?: draft?.text.orEmpty(),
        initialCategoryKey = draft?.categoryKey,
        initialLocation = draft?.location.orEmpty(),
        restoredDraft = draft != null,
        onDraftChanged = userId?.let { id -> { text, categoryKey, location -> drafts.save(id, PostDraft(text, categoryKey, location)) } },
        onDiscardDraft = { userId?.let(drafts::clear) },
        onPickMedia = pickMedia,
        onRemoveMedia = {
            mediaUri = null
            isVideo = false
        },
        onPost = { content, category, mentions, location ->
            viewModel.createPost(content, mediaUri, isVideo, category, mentions, location)
        },
        onNavigateBack = onNavigateBack,
        onMentionQuery = viewModel::searchMentions
    )
}
