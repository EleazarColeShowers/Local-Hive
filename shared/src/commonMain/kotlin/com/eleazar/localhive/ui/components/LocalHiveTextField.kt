package com.eleazar.localhive.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.eleazar.localhive.ui.theme.DarkGray
import com.eleazar.localhive.ui.theme.HiveGreen

@Composable
fun LocalHiveTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = modifier,
        isError = isError,
        singleLine = singleLine,
        maxLines = maxLines,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        supportingText = supportingText?.let { { Text(it) } },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HiveGreen,
            focusedLabelColor = HiveGreen,
            unfocusedBorderColor = DarkGray.copy(alpha = 0.3f),
            unfocusedLabelColor = DarkGray.copy(alpha = 0.6f),
            cursorColor = HiveGreen,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            // The field is always white, so its text stays dark in dark mode too.
            focusedTextColor = DarkGray,
            unfocusedTextColor = DarkGray,
            focusedLeadingIconColor = DarkGray.copy(alpha = 0.6f),
            unfocusedLeadingIconColor = DarkGray.copy(alpha = 0.5f),
            focusedPlaceholderColor = DarkGray.copy(alpha = 0.4f),
            unfocusedPlaceholderColor = DarkGray.copy(alpha = 0.4f)
        )
    )
}
