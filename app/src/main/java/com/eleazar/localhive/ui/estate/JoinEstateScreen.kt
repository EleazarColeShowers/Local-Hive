package com.eleazar.localhive.ui.estate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eleazar.localhive.R
import com.eleazar.localhive.domain.model.Estate

@Composable
fun JoinEstateScreen(
    initialCode: String = "",
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: EstateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf(initialCode.uppercase().take(6)) }

    val green = colorResource(id = R.color.hivegreen)
    val gold = colorResource(id = R.color.backgroundgold)
    val dark = colorResource(id = R.color.darkgray)

    LaunchedEffect(uiState.success) {
        if (uiState.success) onSuccess()
    }

    LaunchedEffect(Unit) {
        viewModel.searchEstates("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(gold, Color(0xFFFFF8E7))))
    ) {
        Row(
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = dark)
            }
            Text(
                "Join Estate",
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
                        "Search Estates",
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
                        "Invite Code",
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
                    viewModel.searchEstates(it)
                },
                results = uiState.searchResults,
                isLoading = uiState.isLoading,
                requestedEstateIds = uiState.requestedEstateIds,
                onRequestJoin = { viewModel.requestToJoin(it) },
                dark = dark,
                green = green
            )
            1 -> InviteCodeTab(
                code = inviteCode,
                onCodeChange = { inviteCode = it.uppercase().take(6) },
                onJoin = { viewModel.joinEstate(inviteCode) },
                isLoading = uiState.isLoading,
                error = uiState.error,
                dark = dark,
                green = green
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
    green: Color
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search for your estate...", color = dark.copy(alpha = 0.4f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = green) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = green,
                unfocusedBorderColor = dark.copy(alpha = 0.2f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
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
                        green = green
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
    green: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
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
    green: Color
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
            label = { Text("Invite Code") },
            placeholder = { Text("ABC123") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = green,
                focusedLabelColor = green,
                unfocusedBorderColor = dark.copy(alpha = 0.25f),
                unfocusedLabelColor = dark.copy(alpha = 0.5f),
                cursorColor = green,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
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
                Text("Join Estate", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}
