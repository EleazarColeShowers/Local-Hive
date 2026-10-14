package com.eleazar.localhive.ui.finance

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eleazar.localhive.ui.util.formatDate
import com.eleazar.localhive.ui.util.nowMillis
import com.eleazar.localhive.ui.util.parseNaira

/** The treasurer setting a new charge for every resident. */
@Composable
fun CreateLevyScreen(
    uiState: FinanceUiState,
    onBack: () -> Unit,
    onCreate: (title: String, amount: String, dueDate: Long, note: String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    // A fortnight out by default, a common window for estate dues.
    var dueDate by rememberSaveable { mutableLongStateOf(nowMillis() + 14L * 24 * 60 * 60 * 1000) }
    var note by rememberSaveable { mutableStateOf("") }
    val valid = title.isNotBlank() && (parseNaira(amount) ?: 0) > 0

    FinanceForm(
        title = "New levy",
        actionLabel = "Create",
        canSave = valid && !uiState.isSaving,
        isSaving = uiState.isSaving,
        error = uiState.error,
        onBack = onBack,
        onSave = { onCreate(title, amount, dueDate, note) }
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(80) },
            label = { Text("What it's for") },
            placeholder = { Text("e.g. Security levy, October") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )
        AmountField(amount, onChange = { amount = it }, label = "Amount each resident pays")
        DateField(label = "Due date", millis = dueDate, onChange = { dueDate = it })
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(500) },
            label = { Text("Note (optional)") },
            placeholder = { Text("Account to pay into, what it covers…") },
            minLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Every member will see this levy and what they owe. You can remind anyone who hasn't paid.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The treasurer logging money spent for the estate.
 *
 * @param pickedReceipt platform image model chosen by the route's picker, previewed here
 */
@Composable
fun AddExpenseScreen(
    uiState: FinanceUiState,
    pickedReceipt: Any?,
    onPickReceipt: () -> Unit,
    onRemoveReceipt: () -> Unit,
    onBack: () -> Unit,
    onSave: (title: String, amount: String, spentAt: Long, note: String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var spentAt by rememberSaveable { mutableLongStateOf(nowMillis()) }
    var note by rememberSaveable { mutableStateOf("") }
    val valid = title.isNotBlank() && (parseNaira(amount) ?: 0) > 0

    FinanceForm(
        title = "Add expense",
        actionLabel = "Save",
        canSave = valid && !uiState.isSaving,
        isSaving = uiState.isSaving,
        error = uiState.error,
        onBack = onBack,
        onSave = { onSave(title, amount, spentAt, note) }
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(80) },
            label = { Text("What was it for?") },
            placeholder = { Text("e.g. Generator diesel") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )
        AmountField(amount, onChange = { amount = it }, label = "Amount")
        DateField(label = "Date", millis = spentAt, onChange = { spentAt = it })
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(500) },
            label = { Text("Note (optional)") },
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )
        if (pickedReceipt == null) {
            OutlinedButton(onClick = onPickReceipt, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Add receipt photo")
            }
        } else {
            Box {
                ProofImage(url = pickedReceipt, contentDescription = "Receipt")
                FilledIconButton(
                    onClick = onRemoveReceipt,
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    )
                ) { Icon(Icons.Default.Close, contentDescription = "Remove receipt", modifier = Modifier.size(18.dp)) }
            }
        }
        Text(
            "Every member can see expenses, so the estate knows where its money goes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinanceForm(
    title: String,
    actionLabel: String,
    canSave: Boolean,
    isSaving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSaving) { Icon(Icons.Default.Close, contentDescription = "Cancel") }
                },
                actions = {
                    Button(
                        onClick = onSave,
                        enabled = canSave,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isSaving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text(actionLabel, fontWeight = FontWeight.SemiBold)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun AmountField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        // Digits only, grouped as they're typed so big amounts are easy to check.
        onValueChange = { typed -> onChange(groupThousands(typed.filter { it.isDigit() }.take(12))) },
        label = { Text(label) },
        prefix = { Text("₦") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun groupThousands(digits: String): String =
    digits.trimStart('0').ifEmpty { if (digits.isEmpty()) "" else "0" }
        .reversed().chunked(3).joinToString(",").reversed()

/** A read-only field that opens a date picker, the usual way to ask for a date. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, millis: Long, onChange: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    LaunchedEffect(interaction) {
        interaction.interactions.collect { if (it is PressInteraction.Release) open = true }
    }
    OutlinedTextField(
        value = formatDate(millis, "EEE, d MMM yyyy"),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Pick a date") },
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth()
    )
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let(onChange)
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}
