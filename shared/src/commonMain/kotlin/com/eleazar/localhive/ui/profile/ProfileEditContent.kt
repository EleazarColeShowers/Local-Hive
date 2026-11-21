package com.eleazar.localhive.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ───────────────────────── Edit mode ─────────────────────────

@Composable
internal fun ProfileEditContent(
    name: String,
    username: String,
    bio: String,
    occupation: String,
    isLoading: Boolean,
    colors: ProfileColors,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onOccupationChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tap your photo to change it",
            style = MaterialTheme.typography.bodySmall,
            color = colors.darkGray.copy(alpha = 0.55f)
        )
        Spacer(Modifier.height(16.dp))

        SectionCard(colors) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Edit profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.darkGray
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Full name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = fieldColors(colors)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") },
                    prefix = { Text("@") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = fieldColors(colors)
                )
                OutlinedTextField(
                    value = occupation,
                    onValueChange = onOccupationChange,
                    label = { Text("Occupation (optional)") },
                    placeholder = { Text("e.g. Teacher, Engineer…") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = fieldColors(colors)
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = onBioChange,
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 3,
                    maxLines = 5,
                    colors = fieldColors(colors)
                )

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold, color = colors.darkGray)
                    }
                    Button(
                        onClick = onSave,
                        enabled = name.isNotBlank() && username.isNotBlank() && !isLoading,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.hiveGreen,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
