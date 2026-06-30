package com.eleazar.localhive.ui.auth

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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eleazar.localhive.R

@Composable
fun DetailsSetUpScreen(
    onDetailsComplete: () -> Unit,
    viewModel: DetailsSetUpViewModel = hiltViewModel()
) {
    var address by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onDetailsComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.backgroundgold))
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
                color = colorResource(id = R.color.darkgray),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "A few more details help your neighbors know you",
                fontSize = 15.sp,
                color = colorResource(id = R.color.darkgray).copy(alpha = 0.7f),
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
                    Icon(Icons.Default.Home, contentDescription = null, tint = colorResource(id = R.color.darkgray).copy(alpha = 0.6f))
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
                    Icon(Icons.Default.Star, contentDescription = null, tint = colorResource(id = R.color.darkgray).copy(alpha = 0.6f))
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
                    Icon(Icons.Default.Phone, contentDescription = null, tint = colorResource(id = R.color.darkgray).copy(alpha = 0.6f))
                },
                colors = fieldColors()
            )

            uiState.error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            }

            Button(
                onClick = { viewModel.saveDetails(address, occupation, phone) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = address.isNotBlank() && !uiState.isLoading,
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

            Spacer(Modifier.height(16.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = colorResource(id = R.color.hivegreen), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)) {
                        append("Skip for now")
                    }
                },
                modifier = Modifier.clickable { viewModel.skip() },
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))
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
