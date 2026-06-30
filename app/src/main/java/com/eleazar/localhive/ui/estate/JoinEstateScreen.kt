package com.eleazar.localhive.ui.estate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
fun JoinEstateScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: EstateViewModel = hiltViewModel()
) {
    var inviteCode by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.success) {
        if (uiState.success) onSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.backgroundgold))
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colorResource(id = R.color.darkgray))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Join Estate",
                fontSize = 26.sp, fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.darkgray),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Ask your estate admin for the 6-character invite code",
                fontSize = 15.sp, color = colorResource(id = R.color.darkgray).copy(alpha = 0.7f),
                textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 36.dp)
            )

            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it.uppercase().take(6) },
                label = { Text("Invite Code") }, placeholder = { Text("ABC123") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.hivegreen),
                    focusedLabelColor = colorResource(id = R.color.hivegreen),
                    unfocusedBorderColor = colorResource(id = R.color.darkgray).copy(alpha = 0.3f),
                    unfocusedLabelColor = colorResource(id = R.color.darkgray).copy(alpha = 0.6f),
                    cursorColor = colorResource(id = R.color.hivegreen),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            uiState.error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            }

            Button(
                onClick = { viewModel.joinEstate(inviteCode) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = inviteCode.length == 6 && !uiState.isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.hivegreen),
                    disabledContainerColor = colorResource(id = R.color.hivegreen).copy(alpha = 0.5f)
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Join Estate", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}
