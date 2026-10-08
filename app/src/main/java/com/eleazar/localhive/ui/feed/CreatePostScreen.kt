package com.eleazar.localhive.ui.feed

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eleazar.localhive.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onNavigateBack: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var content by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { imageUri = it } }

    val canPost = content.isNotBlank() || imageUri != null

    LaunchedEffect(uiState.postCreated) {
        if (uiState.postCreated) {
            viewModel.consumePostCreated()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Post", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (canPost) viewModel.createPost(content, imageUri)
                        },
                        enabled = canPost && !uiState.isPosting
                    ) {
                        Text(
                            if (uiState.isPosting) "Posting…" else "Post",
                            color = if (canPost && !uiState.isPosting) colorResource(id = R.color.hivegreen) else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("What's happening in the estate?", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = false,
                maxLines = Int.MAX_VALUE,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.hivegreen),
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    cursorColor = colorResource(id = R.color.hivegreen)
                )
            )

            imageUri?.let { uri ->
                Spacer(Modifier.height(12.dp))
                Box {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    FilledIconButton(
                        onClick = { imageUri = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove photo", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = !uiState.isPosting
            ) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = colorResource(id = R.color.hivegreen),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (imageUri == null) "Add photo" else "Change photo",
                    color = colorResource(id = R.color.hivegreen),
                    fontSize = 14.sp
                )
            }

            if (uiState.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(uiState.error ?: "", color = Color(0xFFE53935), fontSize = 13.sp)
            }
        }
    }
}
