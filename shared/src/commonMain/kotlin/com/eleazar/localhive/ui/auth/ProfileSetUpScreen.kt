package com.eleazar.localhive.ui.auth

import com.eleazar.localhive.ui.theme.BackgroundGold
import com.eleazar.localhive.ui.theme.DarkGray
import com.eleazar.localhive.ui.theme.HiveGreen
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/**
 * Bare profile setup form.
 *
 * @param pickedImage platform image model (e.g. an Android Uri) chosen by the route's picker, previewed here
 * @param onPickPhoto the route launches its photo picker
 */
@Composable
fun ProfileSetUpScreen(
    isLoading: Boolean,
    error: String?,
    pickedImage: Any?,
    onPickPhoto: () -> Unit,
    onContinue: (displayName: String, bio: String) -> Unit,
) {
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGold)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Set Up Profile",
                fontSize = 28.sp, fontWeight = FontWeight.Bold,
                color = DarkGray,
                textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Tell your neighbors a little about yourself",
                fontSize = 15.sp, color = DarkGray.copy(alpha = 0.7f),
                textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 24.dp)
            )

            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .border(2.dp, HiveGreen.copy(alpha = 0.4f), CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onPickPhoto),
                contentAlignment = Alignment.Center
            ) {
                val uri = pickedImage
                if (uri == null) {
                    Text(
                        "Add\nphoto",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = HiveGreen
                    )
                } else {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Profile photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Text(
                text = "Optional",
                fontSize = 12.sp,
                color = DarkGray.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            OutlinedTextField(
                value = displayName, onValueChange = { displayName = it },
                label = { Text("Full Name") }, placeholder = { Text("John Doe") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                colors = fieldColors()
            )
            OutlinedTextField(
                value = bio, onValueChange = { bio = it },
                label = { Text("Bio (optional)") }, placeholder = { Text("A short intro...") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                shape = RoundedCornerShape(12.dp), singleLine = false, maxLines = 3,
                colors = fieldColors()
            )

            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            }

            Button(
                onClick = { onContinue(displayName, bio) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = displayName.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HiveGreen,
                    disabledContainerColor = HiveGreen.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = HiveGreen,
    focusedLabelColor = HiveGreen,
    unfocusedBorderColor = DarkGray.copy(alpha = 0.3f),
    unfocusedLabelColor = DarkGray.copy(alpha = 0.6f),
    cursorColor = HiveGreen,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)
