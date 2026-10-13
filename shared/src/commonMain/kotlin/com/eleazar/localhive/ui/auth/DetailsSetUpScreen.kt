package com.eleazar.localhive.ui.auth

import com.eleazar.localhive.ui.theme.BackgroundGold
import com.eleazar.localhive.ui.theme.DarkGray
import com.eleazar.localhive.ui.theme.HiveGreen
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Bare "about you" form shown after profile setup; the route saves and navigates. */
@Composable
fun DetailsSetUpScreen(
    isLoading: Boolean,
    error: String?,
    onSave: (address: String, occupation: String, phone: String) -> Unit,
    onSkip: () -> Unit,
) {
    var address by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGold)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                text = "Almost there!",
                fontSize = 28.sp, fontWeight = FontWeight.Bold,
                color = DarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "A few more details help your neighbors know you",
                fontSize = 15.sp,
                color = DarkGray.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 36.dp)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Home Address") },
                placeholder = { Text("e.g. 12 Oak Street, Apt 3B") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Home, contentDescription = null, tint = DarkGray.copy(alpha = 0.6f))
                },
                colors = fieldColors()
            )

            OutlinedTextField(
                value = occupation,
                onValueChange = { occupation = it },
                label = { Text("Occupation (optional)") },
                placeholder = { Text("e.g. Teacher, Engineer…") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Star, contentDescription = null, tint = DarkGray.copy(alpha = 0.6f))
                },
                colors = fieldColors()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number (optional)") },
                placeholder = { Text("+1 555 000 0000") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 36.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = DarkGray.copy(alpha = 0.6f))
                },
                colors = fieldColors()
            )

            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            }

            Button(
                onClick = { onSave(address, occupation, phone) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = address.isNotBlank() && !isLoading,
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

            Spacer(Modifier.height(16.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = HiveGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)) {
                        append("Skip for now")
                    }
                },
                modifier = Modifier.clickable { onSkip() },
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))
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
