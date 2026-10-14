package com.eleazar.localhive.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/** Everything the profile setup form collects; only [displayName] is required. */
data class ProfileDetails(
    val displayName: String,
    val bio: String,
    val address: String,
    val occupation: String,
    val phone: String
)

/**
 * Bare profile setup form: photo, name and bio, then the details that help neighbours place
 * you, all on one page.
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
    onContinue: (ProfileDetails) -> Unit,
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var occupation by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Set Up Profile",
                fontSize = 28.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Tell your neighbors a little about yourself",
                fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 24.dp)
            )

            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
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
                        color = MaterialTheme.colorScheme.primary
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            OutlinedTextField(
                value = displayName, onValueChange = { displayName = it },
                label = { Text("Full Name") }, placeholder = { Text("John Doe") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors = fieldColors()
            )
            OutlinedTextField(
                value = bio, onValueChange = { bio = it },
                label = { Text("Bio (optional)") }, placeholder = { Text("A short intro...") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp), singleLine = false, maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = fieldColors()
            )
            DetailField(
                value = address, onValueChange = { address = it },
                label = "Home Address (optional)", placeholder = "e.g. 12 Oak Street, Apt 3B",
                icon = Icons.Default.Home
            )
            DetailField(
                value = occupation, onValueChange = { occupation = it },
                label = "Occupation (optional)", placeholder = "e.g. Teacher, Engineer…",
                icon = Icons.Default.Star
            )
            DetailField(
                value = phone, onValueChange = { phone = it },
                label = "Phone Number (optional)", placeholder = "+1 555 000 0000",
                icon = Icons.Default.Phone,
                keyboardType = KeyboardType.Phone
            )

            Spacer(Modifier.height(16.dp))

            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            }

            Button(
                onClick = { onContinue(ProfileDetails(displayName, bio, address, occupation, phone)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = displayName.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = fieldColors()
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.outline
)
