package com.budging.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.budging.app.data.model.ExpenseEntryState
import com.budging.app.ui.component.BudgetChip
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.component.categoryAccent
import com.budging.app.ui.format.formatCurrency
import com.budging.app.ui.theme.BudgingTheme
import java.time.LocalDate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun LogExpenseScreen(
    state: ExpenseEntryState,
    onSaveExpense: (amountMinor: Long, categoryId: Long, dateText: String, note: String) -> Unit,
) {
    val spacing = BudgingTheme.spacing
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable(state.budgetName, state.categories.firstOrNull()?.id) {
        mutableStateOf<Long?>(state.categories.firstOrNull()?.id)
    }
    var dateText by rememberSaveable(state.budgetName) { mutableStateOf(LocalDate.now().toString()) }
    var noteText by rememberSaveable { mutableStateOf("") }

    val selectedCategory = state.categories.firstOrNull { it.id == selectedCategoryId }
    val predictedRemaining = selectedCategory?.remainingAmountMinor?.minus(amountText.toLongOrNull() ?: 0L)

    LazyColumn(
        modifier = Modifier.padding(horizontal = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item { SectionHeader(eyebrow = "Expense", title = "Log Expense") }
        if (!state.hasActiveBudget) {
            item {
                BudgetScaffoldCard {
                    Text("No active budget", style = MaterialTheme.typography.titleLarge)
                    Text("Create a budget period first, then come back to log expenses.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@LazyColumn
        }
        item {
            BudgetScaffoldCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    Text("Enter Amount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCurrency(amountText.toLongOrNull() ?: 0L, state.currencyCode),
                        style = MaterialTheme.typography.displayLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Text(state.dateRangeLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                Text("Choose Category", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    items(state.categories) { category ->
                        BudgetChip(
                            selected = selectedCategoryId == category.id,
                            label = category.name,
                            icon = categoryAccent(category.name).icon,
                            onClick = { selectedCategoryId = category.id },
                        )
                    }
                }
            }
        }
        item {
            BudgetScaffoldCard {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = spacing.md, vertical = spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Date", style = MaterialTheme.typography.labelMedium)
                            Text(dateText, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = spacing.md, vertical = spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Note", style = MaterialTheme.typography.labelMedium)
                            Text(if (noteText.isBlank()) "Optional" else noteText, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        }
                    }
                }
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Expense Date") },
                    supportingText = { Text("YYYY-MM-DD") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note") },
                    singleLine = true,
                )
            }
        }
        item {
            BudgetScaffoldCard {
                Keypad(
                    onDigit = { digit -> amountText += digit },
                    onDelete = { amountText = if (amountText.isNotEmpty()) amountText.dropLast(1) else "" },
                    onClear = { amountText = "" },
                )
            }
        }
        item {
            Button(
                onClick = {
                    onSaveExpense(amountText.toLongOrNull() ?: 0L, selectedCategoryId ?: 0L, dateText, noteText)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Confirm", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (predictedRemaining != null) {
                            "${selectedCategory?.name.orEmpty()} left ${formatCurrency(predictedRemaining, state.currencyCode)}"
                        } else {
                            "Choose a category to continue"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                    )
                }
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
    val spacing = BudgingTheme.spacing
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("000", "0", "Del"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = spacing.md)
                            .clickable {
                                when (key) {
                                    "Del" -> onDelete()
                                    else -> onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(key, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Text(
            "Clear",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onClear)
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
