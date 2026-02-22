package com.eleazar.localhive.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.R

@Composable
fun SignupScreen(
    onNavigateToLogin: () -> Unit,
    onSignupSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Email validation
    val isEmailValid = email.isEmpty() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

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
            // Title Section
            Text(
                text = "Create Account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.darkgray),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Join your neighborhood community",
                fontSize = 16.sp,
                color = colorResource(id = R.color.darkgray).copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // Username Field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                placeholder = { Text("johndoe") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.hivegreen),
                    focusedLabelColor = colorResource(id = R.color.hivegreen),
                    unfocusedBorderColor = colorResource(id = R.color.darkgray).copy(alpha = 0.3f),
                    unfocusedLabelColor = colorResource(id = R.color.darkgray).copy(alpha = 0.6f),
                    cursorColor = colorResource(id = R.color.hivegreen),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                placeholder = { Text("you@example.com") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.hivegreen),
                    focusedLabelColor = colorResource(id = R.color.hivegreen),
                    unfocusedBorderColor = colorResource(id = R.color.darkgray).copy(alpha = 0.3f),
                    unfocusedLabelColor = colorResource(id = R.color.darkgray).copy(alpha = 0.6f),
                    cursorColor = colorResource(id = R.color.hivegreen),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    errorBorderColor = colorResource(id = R.color.deephoney),
                    errorLabelColor = colorResource(id = R.color.deephoney)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = email.isNotEmpty() && !isEmailValid,
                supportingText = {
                    if (email.isNotEmpty() && !isEmailValid) {
                        Text(
                            text = "Please enter a valid email",
                            color = colorResource(id = R.color.deephoney),
                            fontSize = 12.sp
                        )
                    }
                }
            )

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("At least 6 characters") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.hivegreen),
                    focusedLabelColor = colorResource(id = R.color.hivegreen),
                    unfocusedBorderColor = colorResource(id = R.color.darkgray).copy(alpha = 0.3f),
                    unfocusedLabelColor = colorResource(id = R.color.darkgray).copy(alpha = 0.6f),
                    cursorColor = colorResource(id = R.color.hivegreen),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible)
                                Icons.Default.Face
                            else
                                Icons.Default.Lock,
                            contentDescription = if (passwordVisible)
                                "Hide password"
                            else
                                "Show password",
                            tint = colorResource(id = R.color.darkgray).copy(alpha = 0.6f)
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Signup Button
            Button(
                onClick = {
                    // TODO: Implement signup logic
                    isLoading = true
                    // onSignupSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.hivegreen),
                    disabledContainerColor = colorResource(id = R.color.hivegreen).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = username.isNotBlank() &&
                        isEmailValid &&
                        email.isNotBlank() &&
                        password.length >= 6 &&
                        !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign Up",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Link
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = colorResource(id = R.color.darkgray).copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    ) {
                        append("Already have an account? ")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = colorResource(id = R.color.hivegreen),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("Log In")
                    }
                },
                modifier = Modifier.clickable { onNavigateToLogin() },
                textAlign = TextAlign.Center
            )
        }
    }
}