package com.budging.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.ExpenseEntryState
import com.budging.app.ui.format.formatCurrency
import java.time.LocalDate

@Composable
fun LogExpenseScreen(
    state: ExpenseEntryState,
    onSaveExpense: (amountMinor: Long, categoryId: Long, dateText: String, note: String) -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable(state.budgetName, state.categories.firstOrNull()?.id) {
        mutableStateOf<Long?>(state.categories.firstOrNull()?.id)
    }
    var dateText by rememberSaveable(state.budgetName) { mutableStateOf(LocalDate.now().toString()) }
    var noteText by rememberSaveable { mutableStateOf("") }

    val selectedCategory = state.categories.firstOrNull { it.id == selectedCategoryId }
    val predictedRemaining = selectedCategory?.remainingAmountMinor?.minus(amountText.toLongOrNull() ?: 0L)

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Log Expense", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        if (!state.hasActiveBudget) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("No active budget", style = MaterialTheme.typography.titleLarge)
                        Text("Create a budget period first, then come back to log expenses.")
                    }
                }
            }
            return@LazyColumn
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(state.budgetName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatCurrency(amountText.toLongOrNull() ?: 0L, state.currencyCode),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(state.dateRangeLabel, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Text("Choose Category", style = MaterialTheme.typography.titleMedium)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { selectedCategoryId = category.id },
                        label = { Text(category.name) },
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text("Expense Date") },
                supportingText = { Text("YYYY-MM-DD") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
        }
        item {
            Keypad(
                onDigit = { digit -> amountText += digit },
                onDelete = {
                    amountText = if (amountText.isNotEmpty()) amountText.dropLast(1) else ""
                },
                onClear = { amountText = "" },
            )
        }
        item {
            Button(
                onClick = {
                    onSaveExpense(
                        amountText.toLongOrNull() ?: 0L,
                        selectedCategoryId ?: 0L,
                        dateText,
                        noteText,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                val label = if (predictedRemaining != null) {
                    "Confirm - ${selectedCategory?.name.orEmpty()} left ${formatCurrency(predictedRemaining, state.currencyCode)}"
                } else {
                    "Confirm Expense"
                }
                Text(label, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("000", "0", "Del"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    Button(
                        onClick = {
                            when (key) {
                                "Del" -> onDelete()
                                else -> onDigit(key)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(key)
                    }
                }
            }
        }
        Button(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
            Text("Clear")
        }
    }
}
