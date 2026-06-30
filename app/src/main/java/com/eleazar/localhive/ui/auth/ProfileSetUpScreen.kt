package com.eleazar.localhive.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eleazar.localhive.R

@Composable
fun ProfileSetUpScreen(
    onProfileComplete: () -> Unit,
    viewModel: ProfileSetUpViewModel = hiltViewModel()
) {
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onProfileComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.backgroundgold))
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
                color = colorResource(id = R.color.darkgray),
                textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Tell your neighbors a little about yourself",
                fontSize = 15.sp, color = colorResource(id = R.color.darkgray).copy(alpha = 0.7f),
                textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 36.dp)
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

            uiState.error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            }

            Button(
                onClick = { viewModel.saveProfile(displayName, bio) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = displayName.isNotBlank() && !uiState.isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.hivegreen),
                    disabledContainerColor = colorResource(id = R.color.hivegreen).copy(alpha = 0.5f)
                )
            ) {
                if (uiState.isLoading) {
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
    focusedBorderColor = colorResource(id = R.color.hivegreen),
    focusedLabelColor = colorResource(id = R.color.hivegreen),
    unfocusedBorderColor = colorResource(id = R.color.darkgray).copy(alpha = 0.3f),
    unfocusedLabelColor = colorResource(id = R.color.darkgray).copy(alpha = 0.6f),
    cursorColor = colorResource(id = R.color.hivegreen),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)
