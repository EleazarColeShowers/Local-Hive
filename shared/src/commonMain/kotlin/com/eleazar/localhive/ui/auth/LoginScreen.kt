package com.eleazar.localhive.ui.auth

import com.eleazar.localhive.ui.theme.DarkGray
import com.eleazar.localhive.ui.theme.HiveGreen
import com.eleazar.localhive.ui.util.isValidEmail
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.ui.components.LocalHiveButton
import com.eleazar.localhive.ui.components.LocalHiveTextField

/** Bare login form. The route owns the ViewModel and decides what success means. */
@Composable
fun LoginScreen(
    isLoading: Boolean,
    error: String?,
    onLoginClick: (email: String, password: String) -> Unit,
    onNavigateToSignup: () -> Unit,
    passwordReset: PasswordResetState,
    onSendPasswordReset: (email: String) -> Unit,
    onDismissPasswordReset: () -> Unit,
    onInputChanged: () -> Unit = {},
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(value = false) }
    var showForgotPassword by rememberSaveable { mutableStateOf(false) }

    val isEmailValid = remember(email) {
        email.isEmpty() || isValidEmail(email)
    }
    val darkGray = DarkGray
    val hiveGreen = HiveGreen
    val keyboardController = LocalSoftwareKeyboardController.current

    val canSubmit = isEmailValid && email.isNotBlank() && password.isNotBlank() && !isLoading

    val handleLogin = {
        if (canSubmit) {
            keyboardController?.hide()
            onLoginClick(email, password)
        }
    }

    AuthScreenScaffold {
        GlassCard {
            Text(
                text = "Welcome Back",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = darkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            )
            Text(
                text = "Sign in to continue to LocalHive",
                fontSize = 14.sp,
                color = darkGray.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
            )

            LocalHiveTextField(
                value = email,
                onValueChange = {
                    email = it
                    onInputChanged()
                },
                label = "Email",
                placeholder = "you@example.com",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = darkGray.copy(alpha = 0.5f)
                    )
                },
                isError = email.isNotEmpty() && !isEmailValid,
                supportingText = if (email.isNotEmpty() && !isEmailValid) "Please enter a valid email" else null
            )

            LocalHiveTextField(
                value = password,
                onValueChange = {
                    password = it
                    onInputChanged()
                },
                label = "Password",
                placeholder = "Enter your password",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = darkGray.copy(alpha = 0.5f)
                    )
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = darkGray.copy(alpha = if (passwordVisible) 0.8f else 0.4f)
                        )
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Forgot Password?",
                    color = hiveGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(role = Role.Button) { showForgotPassword = true }
                )
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            LocalHiveButton(
                text = "Log In",
                onClick = handleLogin,
                modifier = Modifier.fillMaxWidth(),
                enabled = canSubmit,
                isLoading = isLoading
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = darkGray.copy(alpha = 0.6f), fontSize = 14.sp)) { append("Don't have an account? ") }
                    withStyle(SpanStyle(color = hiveGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)) { append("Sign Up") }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
                    .clickable(role = Role.Button) { onNavigateToSignup() },
                textAlign = TextAlign.Center
            )
        }
    }

    if (showForgotPassword) {
        ForgotPasswordDialog(
            initialEmail = email.trim(),
            state = passwordReset,
            onSend = onSendPasswordReset,
            onDismiss = {
                showForgotPassword = false
                onDismissPasswordReset()
            }
        )
    }
}
