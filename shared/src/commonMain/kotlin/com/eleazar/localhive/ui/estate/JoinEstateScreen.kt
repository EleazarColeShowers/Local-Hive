package com.eleazar.localhive.ui.estate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
 * Bare UI for joining an estate. No ViewModel or Android Context side effects.
 */
@Composable
fun JoinEstateScreen(
    initialCode: String = "",
    searchResults: List<Estate>,
    requestedEstateIds: Set<String>,
    isLoading: Boolean,
    error: String?,
    colors: EstateColors = rememberEstateColors(),
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRequestJoin: (estateId: String) -> Unit,
    onJoinWithCode: (inviteCode: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf(initialCode.uppercase().take(6)) }

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
            Text(
                "Join estate",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = dark
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = green,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = green
                )
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "Search estates",
                        fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedTab == 0) green else dark.copy(alpha = 0.5f)
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "Invite code",
                        fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedTab == 1) green else dark.copy(alpha = 0.5f)
                    )
                }
            )
        }

        HorizontalDivider(color = dark.copy(alpha = 0.08f))

        when (selectedTab) {
            0 -> SearchTab(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    onSearchQueryChange(it)
                },
                results = searchResults,
                isLoading = isLoading,
                requestedEstateIds = requestedEstateIds,
                onRequestJoin = onRequestJoin,
                dark = dark,
                green = green,
                cardBg = cardBg
            )
            1 -> InviteCodeTab(
                code = inviteCode,
                onCodeChange = { inviteCode = it.uppercase().take(6) },
                onJoin = { onJoinWithCode(inviteCode) },
                isLoading = isLoading,
                error = error,
                dark = dark,
                green = green,
                cardBg = cardBg
            )
        }
    }
}

@Composable
private fun SearchTab(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Estate>,
    isLoading: Boolean,
    requestedEstateIds: Set<String>,
    onRequestJoin: (String) -> Unit,
    dark: Color,
    green: Color,
    cardBg: Color
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search for your estate...", color = dark.copy(alpha = 0.4f)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = green,
                unfocusedBorderColor = dark.copy(alpha = 0.2f),
                focusedTextColor = dark,
                unfocusedTextColor = dark,
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg,
                cursorColor = green
            )
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = green, modifier = Modifier.size(32.dp), strokeWidth = 2.5.dp)
            }
        } else if (results.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Home, null, tint = dark.copy(alpha = 0.2f), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (query.isBlank()) "No estates found yet" else "No results for \"$query\"",
                        color = dark.copy(alpha = 0.4f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(results, key = { it.id }) { estate ->
                    EstateSearchCard(
                        estate = estate,
                        hasRequested = estate.id in requestedEstateIds,
                        onRequest = { onRequestJoin(estate.id) },
                        dark = dark,
                        green = green,
                        cardBg = cardBg
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun EstateSearchCard(
    estate: Estate,
    hasRequested: Boolean,
    onRequest: () -> Unit,
    dark: Color,
    green: Color,
    cardBg: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(green.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Home, null, tint = green, modifier = Modifier.size(26.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(estate.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = dark)
                Spacer(Modifier.height(2.dp))
                Text(estate.address, fontSize = 12.sp, color = dark.copy(alpha = 0.55f), maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${estate.memberCount} member${if (estate.memberCount != 1) "s" else ""}",
                    fontSize = 11.sp,
                    color = green,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.width(10.dp))

            if (hasRequested) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(green.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = green, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sent", color = green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = onRequest,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = green),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Request", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InviteCodeTab(
    code: String,
    onCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
    isLoading: Boolean,
    error: String?,
    dark: Color,
    green: Color,
    cardBg: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Enter Invite Code",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = dark,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "Ask your estate admin for the\n6-character invite code",
            fontSize = 14.sp,
            color = dark.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(bottom = 36.dp)
        )

        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            label = { Text("Invite code") },
            placeholder = { Text("ABC123") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = green,
                focusedLabelColor = green,
                unfocusedBorderColor = dark.copy(alpha = 0.25f),
                unfocusedLabelColor = dark.copy(alpha = 0.5f),
                focusedTextColor = dark,
                unfocusedTextColor = dark,
                cursorColor = green,
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg
            )
        )

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onJoin,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = code.length == 6 && !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = green,
                disabledContainerColor = green.copy(alpha = 0.4f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("Join estate", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}
