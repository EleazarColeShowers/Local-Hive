package com.eleazar.localhive.ui.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.eleazar.localhive.domain.model.PostCategory
import com.eleazar.localhive.ui.components.Avatar
import com.eleazar.localhive.ui.components.CategoryTag
import com.eleazar.localhive.ui.components.ScreenGutter

/** Long enough for a proper notice, short enough to read in the feed. */
private const val MAX_POST_LENGTH = 2000

/**
 * Bare post composer.
 *
 * @param pickedImage platform image model (e.g. an Android Uri) from the route's picker, previewed here
 * @param onPickPhoto the route launches its photo picker
 * @param onRemovePhoto the route forgets the picked image
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    uiState: FeedUiState,
    pickedImage: Any?,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onPost: (content: String, category: PostCategory?) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var content by rememberSaveable { mutableStateOf("") }
    // Saved by key: a plain string survives process death on every platform.
    var categoryKey by rememberSaveable { mutableStateOf<String?>(null) }
    val category = PostCategory.fromKey(categoryKey)
    val focusRequester = remember { FocusRequester() }
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
    val pickPhoto = onPickPhoto
    val imageUri = pickedImage

    val canPost = (content.isNotBlank() || imageUri != null) && !uiState.isPosting

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("New post", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !uiState.isPosting) {
                        Icon(Icons.Default.Close, contentDescription = "Discard")
                    }
                },
                actions = {
                    Button(
                        onClick = { onPost(content.trim(), category) },
                        enabled = canPost,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        modifier = Modifier.padding(end = 8.dp).height(40.dp)
                    ) {
                        if (uiState.isPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Posting", fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("Post", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = pickPhoto, enabled = !uiState.isPosting) {
                            Icon(Icons.Outlined.Image, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (imageUri == null) "Add photo" else "Change photo", color = accent, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.weight(1f))
                        val remaining = MAX_POST_LENGTH - content.length
                        if (remaining <= 200) {
                            Text(
                                "$remaining",
                                fontSize = 13.sp,
                                color = if (remaining <= 20) MaterialTheme.colorScheme.error else muted,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(imageUrl = uiState.currentUserImageUrl, name = uiState.currentUserName, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        uiState.currentUserName ?: "You",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        if (category == PostCategory.ANNOUNCEMENT) "Pinned to the top of everyone's feed"
                        else "Visible to everyone in your estate",
                        fontSize = 12.sp,
                        color = muted
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            // Optional; tapping the selected one again clears it.
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Announcements are offered to admins only; the rules refuse them from anyone else.
                PostCategory.entries.filter { !it.adminOnly || uiState.isAdmin }.forEach { option ->
                    CategoryTag(
                        category = option,
                        selected = category == option,
                        onClick = { categoryKey = if (category == option) null else option.key }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            // Bare field so the text starts on the same edge as the author row above.
            BasicTextField(
                value = content,
                onValueChange = { content = it.take(MAX_POST_LENGTH) },
                enabled = !uiState.isPosting,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    Box {
                        if (content.isEmpty()) {
                            Text("What's happening in the estate?", fontSize = 18.sp, lineHeight = 26.sp, color = muted)
                        }
                        inner()
                    }
                }
            )

            imageUri?.let { uri ->
                Spacer(Modifier.height(8.dp))
                Box {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    FilledIconButton(
                        onClick = onRemovePhoto,
                        enabled = !uiState.isPosting,
                        shape = CircleShape,
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(34.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove photo", modifier = Modifier.size(18.dp))
                    }
                }
            }

            uiState.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(10.dp))
                        Text(error, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
