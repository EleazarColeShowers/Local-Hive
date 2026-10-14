package com.eleazar.localhive.ui.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
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
import com.eleazar.localhive.ui.components.MentionSuggestionList
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.components.activeMentionQuery
import com.eleazar.localhive.ui.components.insertMention
import com.eleazar.localhive.ui.components.mentionName
import com.eleazar.localhive.ui.components.mentionSuggestions
import com.eleazar.localhive.ui.components.mentionsIn
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Long enough for a proper notice, short enough to read in the feed. */
private const val MAX_POST_LENGTH = 2000

/**
 * Bare post composer.
 *
 * @param pickedMedia platform image model (e.g. an Android Uri, or a video's first frame) previewed here
 * @param pickedIsVideo the picked media is a video, so its preview gets a play badge
 * @param initialContent text to start from, e.g. something shared from another app
 * @param onPickMedia the route launches its photo and video picker
 * @param onRemoveMedia the route forgets the picked photo or video
 * @param editing changing an existing post: its text, tags and location can change, but not its
 *   category or media
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    uiState: FeedUiState,
    pickedMedia: Any?,
    pickedIsVideo: Boolean,
    onPickMedia: () -> Unit,
    onRemoveMedia: () -> Unit,
    onPost: (content: String, category: PostCategory?, mentions: Map<String, String>, location: String) -> Unit,
    onNavigateBack: () -> Unit,
    initialContent: String = "",
    editing: Boolean = false,
    initialCategoryKey: String? = null,
    initialLocation: String = "",
    initialMentions: Map<String, String> = emptyMap(),
) {
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val start = initialContent.take(MAX_POST_LENGTH)
        mutableStateOf(TextFieldValue(start, TextRange(start.length)))
    }
    val content = field.text
    // Everyone picked from the suggestions; trimmed to those still named in the text on posting.
    var picked by rememberSaveable { mutableStateOf(initialMentions) }
    val mentionQuery = activeMentionQuery(field)
    val suggestions = mentionQuery?.let { mentionSuggestions(uiState.members, it.query) }.orEmpty()
    // Saved by key: a plain string survives process death on every platform.
    var categoryKey by rememberSaveable { mutableStateOf(initialCategoryKey) }
    val category = PostCategory.fromKey(categoryKey)
    var location by rememberSaveable { mutableStateOf(initialLocation) }
    val focusRequester = remember { FocusRequester() }
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
    val hasMedia = pickedMedia != null || pickedIsVideo

    val canPost = (content.isNotBlank() || hasMedia) && !uiState.isPosting

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "Edit post" else "New post", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !uiState.isPosting) {
                        Icon(Icons.Default.Close, contentDescription = "Discard")
                    }
                },
                actions = {
                    Button(
                        onClick = { onPost(content.trim(), category, mentionsIn(content, picked), location.trim()) },
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
                            val progress = uiState.uploadProgress
                            Text(
                                when {
                                    progress != null -> "Uploading ${(progress * 100).toInt()}%"
                                    editing -> "Saving"
                                    else -> "Posting"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(if (editing) "Save" else "Post", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.navigationBarsPadding().imePadding()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    // Just above the keyboard, where the person typing will see it.
                    if (mentionQuery != null && suggestions.isNotEmpty()) {
                        MentionSuggestionList(suggestions, onPick = { member ->
                            field = insertMention(field, mentionQuery, member)
                            picked = picked + (member.id to member.mentionName)
                        })
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // A post's photo or video stays as it was posted.
                        if (!editing) {
                            TextButton(onClick = onPickMedia, enabled = !uiState.isPosting) {
                                Icon(Icons.Outlined.Image, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (hasMedia) "Change photo or video" else "Add photo or video",
                                    color = accent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
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
                        when (category) {
                            PostCategory.ANNOUNCEMENT -> "Pinned to the top of everyone's feed"
                            PostCategory.SECURITY -> "Visible to your estate; admins are notified"
                            else -> "Visible to everyone in your estate"
                        },
                        fontSize = 12.sp,
                        color = muted
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            // A post keeps its category: changing it could turn an old post into an announcement.
            if (editing) {
                category?.let { CategoryTag(it) }
            } else FlowRow(
                // Optional; tapping the selected one again clears it. Wrapped rather than scrolled,
                // so every category is in view.
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Announcements are offered to admins only; the rules refuse them from anyone else.
                PostCategory.entries.filter { it.inComposer && (!it.adminOnly || uiState.isAdmin) }.forEach { option ->
                    CategoryTag(
                        category = option,
                        selected = category == option,
                        onClick = { categoryKey = if (category == option) null else option.key }
                    )
                }
            }

            if (category?.isIncident == true) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it.take(120) },
                    label = { Text("Where in the estate?") },
                    placeholder = { Text("e.g. Back fence by Block C") },
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
                    singleLine = true,
                    enabled = !uiState.isPosting,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))
            // Bare field so the text starts on the same edge as the author row above.
            BasicTextField(
                value = field,
                onValueChange = {
                    field = if (it.text.length <= MAX_POST_LENGTH) it
                    else TextFieldValue(it.text.take(MAX_POST_LENGTH), TextRange(MAX_POST_LENGTH))
                },
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
                            Text(
                                when {
                                    pickedIsVideo -> "Add a caption"
                                    category == PostCategory.SECURITY -> "What did you see? When did it happen?"
                                    else -> "What's happening in the estate?"
                                },
                                fontSize = 18.sp,
                                lineHeight = 26.sp,
                                color = muted
                            )
                        }
                        inner()
                    }
                }
            )

            if (hasMedia) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (pickedIsVideo) Color.Black else Color.Transparent)
                ) {
                    // A video's still may still be loading, so the black box stands in until then.
                    pickedMedia?.let { model ->
                        AsyncImage(
                            model = model,
                            contentDescription = if (pickedIsVideo) "Selected video" else "Selected photo",
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (pickedIsVideo) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f),
                            modifier = Modifier.align(Alignment.Center).size(56.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.padding(12.dp))
                        }
                    }
                    if (!editing) FilledIconButton(
                        onClick = onRemoveMedia,
                        enabled = !uiState.isPosting,
                        shape = CircleShape,
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(34.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = if (pickedIsVideo) "Remove video" else "Remove photo", modifier = Modifier.size(18.dp))
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
