package com.eleazar.localhive.ui.auth

import com.eleazar.localhive.ui.theme.DarkGray
import com.eleazar.localhive.ui.theme.HiveGreen
import com.eleazar.localhive.ui.util.isValidEmail
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.eleazar.localhive.ui.components.LocalHiveButton
import com.eleazar.localhive.ui.components.LocalHiveTextField

/** Bare signup form. The route owns the ViewModel and navigates once the account exists. */
@Composable
fun SignupScreen(
    isLoading: Boolean,
    error: String?,
    onSignup: (email: String, username: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isEmailValid = email.isEmpty() || isValidEmail(email)
    val isUsernameValid = username.isEmpty() || username.matches(Regex("^[a-zA-Z0-9_.]{3,20}$"))
    val darkGray = DarkGray
    val hiveGreen = HiveGreen

    AuthScreenScaffold {
        GlassCard {
            Text(
                text = "Create Account",
                fontSize = 26.sp, fontWeight = FontWeight.Bold,
                color = darkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            )
            Text(
                text = "Join your neighborhood community",
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
                value = username, onValueChange = { username = it.lowercase().replace(" ", "") },
                label = "Username", placeholder = "johndoe",
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = darkGray.copy(alpha = 0.5f)) },
                isError = username.isNotEmpty() && !isUsernameValid,
                supportingText = if (username.isNotEmpty() && !isUsernameValid) "3–20 characters, letters, numbers, _ or . only" else null
            )

            LocalHiveTextField(
                value = password, onValueChange = { password = it },
                label = "Password", placeholder = "At least 6 characters",
                modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
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

            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            }

            LocalHiveButton(
                text = "Sign Up",
                onClick = { onSignup(email, username, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = isEmailValid && email.isNotBlank() && username.isNotBlank() && isUsernameValid && password.length >= 6 && !isLoading,
                isLoading = isLoading
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = darkGray.copy(alpha = 0.6f), fontSize = 14.sp)) { append("Already have an account? ") }
                    withStyle(SpanStyle(color = hiveGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)) { append("Log In") }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp).clickable { onNavigateToLogin() },
                textAlign = TextAlign.Center
            )
        }
    }
}
