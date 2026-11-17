package com.eleazar.localhive.ui.finance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.Building
import com.eleazar.localhive.domain.model.Residence
import com.eleazar.localhive.ui.components.EmptyState

/**
 * The estate's buildings and who lives in each, for dues charged per flat. The treasurer and
 * admins add buildings, and correct a member who picked the wrong flat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingsScreen(
    uiState: FinanceUiState,
    onBack: () -> Unit,
    onSave: (Building) -> Unit,
    onDelete: (Building) -> Unit,
    onMove: (Residence, Building, Int) -> Unit,
    onClear: (Residence) -> Unit,
    onMessageShown: () -> Unit,
    onErrorShown: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    /** The building being added (a blank one) or edited. */
    var editing by remember { mutableStateOf<Building?>(null) }
    var moving by remember { mutableStateOf<Residence?>(null) }
    var open by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.message) { uiState.message?.let { snackbar.showSnackbar(it); onMessageShown() } }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbar.showSnackbar(it); onErrorShown() } }

    editing?.let { building ->
        BuildingDialog(
            building = building,
            onSave = { onSave(it); editing = null },
            onDelete = if (building.id.isBlank()) null else ({ onDelete(building); editing = null }),
            onDismiss = { editing = null }
        )
    }
    moving?.let { resident ->
        HomePickerDialog(
            buildings = uiState.buildings,
            current = resident,
            title = "Where does ${resident.userName.ifBlank { "this member" }} live?",
            onPick = { building, flat -> onMove(resident, building, flat); moving = null },
            onDismiss = { moving = null }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Buildings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (uiState.canManageBuildings) {
                ExtendedFloatingActionButton(
                    onClick = { editing = Building() },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add building") }
                )
            }
        }
    ) { padding ->
        if (uiState.buildings.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Apartment,
                title = "No buildings yet",
                message = "Add each building in the estate and how many flats it has. Then you can charge dues per flat, " +
                    "and members can say which flat is theirs.",
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }
        val unpicked = (uiState.memberCount - uiState.residents.size).coerceAtLeast(0)
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Text(
                    "${uiState.buildings.size} buildings · ${uiState.totalFlats} flats" +
                        if (unpicked > 0) " · $unpicked members haven't picked a flat yet" else "",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(uiState.buildings, key = { it.id }) { building ->
                val living = uiState.residents.filter { it.buildingId == building.id }.sortedBy { it.flat }
                val expanded = open == building.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { open = if (expanded) null else building.id }
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(building.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(
                            "${building.flats} ${if (building.flats == 1) "flat" else "flats"} · ${living.size} ${if (living.size == 1) "member" else "members"} living here",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { editing = building }) { Text("Edit") }
                }
                if (expanded) {
                    if (living.isEmpty()) {
                        Text(
                            "Nobody has picked a flat here yet.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                        )
                    }
                    living.forEach { resident ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Flat ${resident.flat} · ${resident.userName.ifBlank { "A neighbour" }}",
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp
                            )
                            TextButton(onClick = { moving = resident }) { Text("Move") }
                            TextButton(
                                onClick = { onClear(resident) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text("Clear") }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun BuildingDialog(
    building: Building,
    onSave: (Building) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(building.name) }
    var flats by remember { mutableStateOf(if (building.id.isBlank()) "" else building.flats.toString()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val count = flats.toIntOrNull()
    val valid = name.isNotBlank() && count != null && count in 1..200
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (building.id.isBlank()) "Add building" else "Edit ${building.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text("Name or number") },
                    placeholder = { Text("e.g. No. 12, Adeola Street") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = flats,
                    onValueChange = { flats = it.filter(Char::isDigit).take(3) },
                    label = { Text("Number of flats") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (confirmDelete) {
                    Text("Remove ${building.name}? Payments already made for its flats stay on the books.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(building.copy(name = name, flats = count ?: 1)) }, enabled = valid) { Text("Save") }
        },
        dismissButton = {
            Row {
                onDelete?.let {
                    TextButton(
                        onClick = { if (confirmDelete) it() else confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(if (confirmDelete) "Yes, remove" else "Remove") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
