package com.eleazar.localhive.ui.util

// Same shape android.util.Patterns.EMAIL_ADDRESS accepts, without the Android dependency.
private val EmailPattern = Regex(
    "[a-zA-Z0-9+._%\\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"
)

fun isValidEmail(email: String): Boolean = EmailPattern.matches(email)
