package com.eleazar.localhive.ui.estate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.Estate

/**
 * Bare UI for creating an estate. No ViewModel or Android Context side effects.
 */
@Composable
fun CreateEstateScreen(
    isLoading: Boolean,
    error: String?,
    colors: EstateColors = rememberEstateColors(),
    onCreateEstate: (name: String, address: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** An existing estate matching the name and address entered; shows the "already exists" prompt. */
    similarEstate: Estate? = null,
    onAskToJoin: (Estate) -> Unit = {},
    onCreateAnyway: (name: String, address: String) -> Unit = { _, _ -> },
    onDismissSimilar: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    val green = colors.hiveGreen
    val gold = colors.backgroundGold
    val dark = colors.darkGray
    val cardBg = colors.cardBackground

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(Brush.verticalGradient(listOf(gold, colors.surfaceGradientEnd)))
    ) {
        Row(
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = dark)
            }
            Text("Create estate", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = dark)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(green.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = green, modifier = Modifier.size(38.dp))
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Start Your Community",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = dark,
                textAlign = TextAlign.Center
            )
            Text(
                "Create an estate and invite your\nneighbours to join",
                fontSize = 14.sp,
                color = dark.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            Text(
                "Free during early access",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = green,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(green.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Spacer(Modifier.height(32.dp))

            Text(
                "Estate name",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = dark.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("e.g. Green Valley Estate", color = dark.copy(alpha = 0.35f)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = fieldColors(green, dark, cardBg)
            )

            Text(
                "Address",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = dark.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                placeholder = { Text("Street, City, State", color = dark.copy(alpha = 0.35f)) },
                leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = green.copy(alpha = 0.7f)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = false,
                maxLines = 2,
                colors = fieldColors(green, dark, cardBg)
            )

            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { onCreateEstate(name.trim(), address.trim()) },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                enabled = name.isNotBlank() && address.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = green,
                    disabledContainerColor = green.copy(alpha = 0.4f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create estate", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    similarEstate?.let { existing ->
        val canAsk = existing.joinMode.acceptsRequests
        AlertDialog(
            onDismissRequest = onDismissSimilar,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("This estate may already exist", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (canAsk) "An estate called ${existing.name} at ${existing.address} already exists. Ask to join it instead?"
                    else "An estate called ${existing.name} at ${existing.address} already exists. It's invite only, " +
                        "so ask one of its members for the invite code."
                )
            },
            confirmButton = {
                if (canAsk) {
                    TextButton(onClick = { onAskToJoin(existing) }) {
                        Text("Ask to join", color = green, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = onDismissSimilar) {
                        Text("OK", color = green, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { onCreateAnyway(name.trim(), address.trim()) }) { Text("Create anyway") }
            }
        )
    }
}

@Composable
private fun fieldColors(green: Color, dark: Color, cardBg: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = green,
    focusedLabelColor = green,
    unfocusedBorderColor = dark.copy(alpha = 0.2f),
    unfocusedLabelColor = dark.copy(alpha = 0.5f),
    focusedTextColor = dark,
    unfocusedTextColor = dark,
    cursorColor = green,
    focusedContainerColor = cardBg,
    unfocusedContainerColor = cardBg
)
