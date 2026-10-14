package com.eleazar.localhive.ui.polls

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.ui.components.ScreenGutter

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

/** How long voting stays open; null runs until an admin closes it. */
private enum class PollLength(val label: String, val millis: Long?) {
    ONE_DAY("1 day", DAY_MILLIS),
    THREE_DAYS("3 days", 3 * DAY_MILLIS),
    ONE_WEEK("1 week", 7 * DAY_MILLIS),
    NO_END("No end date", null)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePollScreen(
    uiState: PollsUiState,
    onCreate: (question: String, options: List<String>, durationMillis: Long?) -> Unit,
    onBack: () -> Unit
) {
    var question by rememberSaveable { mutableStateOf("") }
    var options by rememberSaveable { mutableStateOf(listOf("", "")) }
    var length by rememberSaveable { mutableStateOf(PollLength.THREE_DAYS) }
    val filled = options.map { it.trim() }.filter { it.isNotEmpty() }
    val canCreate = question.isNotBlank() && filled.distinct().size >= 2 && !uiState.isSaving

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("New poll", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !uiState.isSaving) {
                        Icon(Icons.Filled.Close, contentDescription = "Discard")
                    }
                },
                actions = {
                    Button(
                        onClick = { onCreate(question, options, length.millis) },
                        enabled = canCreate,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 8.dp).height(40.dp)
                    ) {
                        Text(if (uiState.isSaving) "Posting" else "Post", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter, vertical = 8.dp)
        ) {
            Text(
                "Everyone in the estate is notified and can vote once. Votes are anonymous; only the totals are shown.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = question,
                onValueChange = { question = it.take(200) },
                label = { Text("Question") },
                placeholder = { Text("e.g. Should we raise the security levy to ₦10,000?") },
                minLines = 2,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            Text("Choices", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            options.forEachIndexed { index, option ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    OutlinedTextField(
                        value = option,
                        onValueChange = { text -> options = options.toMutableList().also { it[index] = text.take(80) } },
                        placeholder = { Text("Choice ${index + 1}") },
                        singleLine = true,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.weight(1f)
                    )
                    // Two choices is the least a poll can have.
                    if (options.size > 2) {
                        IconButton(onClick = { options = options.filterIndexed { i, _ -> i != index } }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove choice ${index + 1}")
                        }
                    }
                }
            }
            if (options.size < MAX_POLL_OPTIONS) {
                TextButton(onClick = { options = options + "" }, enabled = !uiState.isSaving) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add a choice")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Voting closes after", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PollLength.entries.forEach { option ->
                    FilterChip(
                        selected = length == option,
                        onClick = { length = option },
                        label = { Text(option.label) }
                    )
                }
            }

            uiState.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(error, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
