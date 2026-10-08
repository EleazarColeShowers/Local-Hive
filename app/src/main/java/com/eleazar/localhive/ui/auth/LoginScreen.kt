package com.eleazar.localhive.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.R
import com.eleazar.localhive.ui.components.LocalHiveButton
import com.eleazar.localhive.ui.components.LocalHiveTextField

@Composable
fun LoginScreen(
    onNavigateToSignup: () -> Unit,
    onLoginSuccess: (hasEstate: Boolean) -> Unit,
    onForgotPassword: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEmailValid = email.isEmpty() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val darkGray = colorResource(id = R.color.darkgray)
    val hiveGreen = colorResource(id = R.color.hivegreen)

    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            viewModel.clearUser()
            onLoginSuccess(user.estateId != null)
        }
    }

    AuthScreenScaffold {
        GlassCard {
            Text(
                text = "Welcome Back",
                fontSize = 26.sp, fontWeight = FontWeight.Bold,
                color = darkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            )
            Text(
                text = "Sign in to continue to LocalHive",
                fontSize = 14.sp,
                color = darkGray.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)
            )

            LocalHiveTextField(
                value = email, onValueChange = { email = it },
                label = "Email", placeholder = "you@example.com",
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = darkGray.copy(alpha = 0.5f)) },
                isError = email.isNotEmpty() && !isEmailValid,
                supportingText = if (email.isNotEmpty() && !isEmailValid) "Please enter a valid email" else null
            )

            LocalHiveTextField(
                value = password, onValueChange = { password = it },
                label = "Password", placeholder = "Enter your password",
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = darkGray.copy(alpha = 0.5f)) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = darkGray.copy(alpha = if (passwordVisible) 0.8f else 0.4f)
                        )
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Forgot Password?",
                    color = hiveGreen,
                    fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onForgotPassword() }
                )
            }

            uiState.error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            }

            LocalHiveButton(
                text = "Log In",
                onClick = { viewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = isEmailValid && email.isNotBlank() && password.isNotBlank() && !uiState.isLoading,
                isLoading = uiState.isLoading
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = darkGray.copy(alpha = 0.6f), fontSize = 14.sp)) { append("Don't have an account? ") }
                    withStyle(SpanStyle(color = hiveGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)) { append("Sign Up") }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp).clickable { onNavigateToSignup() },
                textAlign = TextAlign.Center
            )
        }
    }
}
