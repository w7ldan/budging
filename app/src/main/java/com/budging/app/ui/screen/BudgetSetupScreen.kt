package com.budging.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.BudgetSetupState
import com.budging.app.ui.component.BudgetMetricRow
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.CategoryIconBubble
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.format.formatCurrency
import com.budging.app.ui.theme.BudgingTheme

@Composable
fun BudgetSetupScreen(
    state: BudgetSetupState,
    onSaveBudget: (name: String, totalAmountMinor: Long, currencyCode: String, startDateText: String, endDateText: String) -> Unit,
    onSaveCategory: (categoryId: Long?, name: String, allocatedAmountMinor: Long) -> Unit,
    onArchiveCategory: (categoryId: Long, isArchived: Boolean) -> Unit,
    onDeleteCategory: (categoryId: Long) -> Unit,
) {
    val spacing = BudgingTheme.spacing
    val darkFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
        focusedContainerColor = MaterialTheme.colorScheme.primary,
        unfocusedContainerColor = MaterialTheme.colorScheme.primary,
        focusedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f),
        unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
        focusedLabelColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
        unfocusedLabelColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
        cursorColor = MaterialTheme.colorScheme.onPrimary,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.45f),
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.45f),
        focusedPrefixColor = MaterialTheme.colorScheme.onPrimary,
        unfocusedPrefixColor = MaterialTheme.colorScheme.onPrimary,
        focusedSupportingTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
        unfocusedSupportingTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
    )
    var budgetName by rememberSaveable(state.activePeriodId) { mutableStateOf(state.periodName) }
    var totalAmountText by rememberSaveable(state.activePeriodId) { mutableStateOf(state.totalAmountMinor.takeIf { it > 0 }?.toString().orEmpty()) }
    var currencyCode by rememberSaveable(state.activePeriodId) { mutableStateOf(state.currencyCode) }
    var startDateText by rememberSaveable(state.activePeriodId) { mutableStateOf(state.startDateText) }
    var endDateText by rememberSaveable(state.activePeriodId) { mutableStateOf(state.endDateText) }
    var editingCategoryId by remember { mutableStateOf<Long?>(null) }
    var categoryName by rememberSaveable(state.activePeriodId, editingCategoryId) { mutableStateOf("") }
    var categoryAmountText by rememberSaveable(state.activePeriodId, editingCategoryId) { mutableStateOf("") }

    val totalForPreview = totalAmountText.toLongOrNull() ?: 0L
    val projectedUnallocated = totalForPreview - state.categories
        .filterNot { it.isArchived }
        .sumOf { if (it.id == editingCategoryId) 0L else it.allocatedAmountMinor } - (categoryAmountText.toLongOrNull() ?: 0L)

    LazyColumn(
        modifier = Modifier.padding(horizontal = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item {
            SectionHeader(eyebrow = "Budget Period", title = "Set Budget")
        }
        item {
            BudgetScaffoldCard(dark = true) {
                Text("How much should this budget period last?", style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)))
                OutlinedTextField(
                    value = totalAmountText,
                    onValueChange = { totalAmountText = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text(currencyCode.ifBlank { "IDR" }) },
                    textStyle = MaterialTheme.typography.displayLarge,
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = darkFieldColors,
                )
                OutlinedTextField(
                    value = budgetName,
                    onValueChange = { budgetName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Budget Period Name") },
                    singleLine = true,
                    colors = darkFieldColors,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    OutlinedTextField(
                        value = startDateText,
                        onValueChange = { startDateText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Start Date") },
                        supportingText = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        colors = darkFieldColors,
                    )
                    OutlinedTextField(
                        value = endDateText,
                        onValueChange = { endDateText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("End Date") },
                        supportingText = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        colors = darkFieldColors,
                    )
                }
                OutlinedTextField(
                    value = currencyCode,
                    onValueChange = { currencyCode = it.uppercase().take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Currency") },
                    singleLine = true,
                    colors = darkFieldColors,
                )
                Button(
                    onClick = {
                        onSaveBudget(
                            budgetName,
                            totalAmountText.toLongOrNull() ?: 0L,
                            currencyCode.ifBlank { "IDR" },
                            startDateText,
                            endDateText,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("Save Budget Period")
                }
            }
        }
        item {
            BudgetScaffoldCard {
                SectionHeader(title = "Unallocated Balance")
                Text(
                    formatCurrency(projectedUnallocated, currencyCode.ifBlank { "IDR" }),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text("Keep this zero or above so category allocations stay inside the budget.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            BudgetScaffoldCard {
                SectionHeader(title = "Add Category")
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Category Name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = categoryAmountText,
                    onValueChange = { categoryAmountText = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Allocated Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    Button(
                        onClick = {
                            onSaveCategory(editingCategoryId, categoryName, categoryAmountText.toLongOrNull() ?: 0L)
                            editingCategoryId = null
                            categoryName = ""
                            categoryAmountText = ""
                        },
                        enabled = state.hasActiveBudget,
                    ) {
                        Text(if (editingCategoryId == null) "Add Category" else "Update Category")
                    }
                    if (editingCategoryId != null) {
                        TextButton(onClick = {
                            editingCategoryId = null
                            categoryName = ""
                            categoryAmountText = ""
                        }) {
                            Text("Cancel")
                        }
                    }
                }
                if (!state.hasActiveBudget) {
                    Text("Save the budget period first, then add categories.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SectionHeader(title = "Category Allocation")
        }
        if (state.categories.isEmpty()) {
            item {
                BudgetScaffoldCard {
                    Text("No categories yet.", style = MaterialTheme.typography.titleMedium)
                    Text("Add categories like Food, Transport, Gym, or Fun.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(state.categories) { category ->
                BudgetScaffoldCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                            CategoryIconBubble(category.name)
                            Column {
                                Text(category.name, style = MaterialTheme.typography.titleMedium)
                                Text(if (category.isArchived) "Archived" else "Active", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(formatCurrency(category.allocatedAmountMinor, state.currencyCode), style = MaterialTheme.typography.titleMedium)
                    }
                    if (category.spentAmountMinor > 0) {
                        BudgetMetricRow("Spent", formatCurrency(category.spentAmountMinor, state.currencyCode))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        TextButton(onClick = {
                            editingCategoryId = category.id
                            categoryName = category.name
                            categoryAmountText = category.allocatedAmountMinor.toString()
                        }) { Text("Edit") }
                        TextButton(onClick = { onArchiveCategory(category.id, !category.isArchived) }) {
                            Text(if (category.isArchived) "Restore" else "Archive")
                        }
                        if (!category.hasTransactions) {
                            TextButton(onClick = { onDeleteCategory(category.id) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}
