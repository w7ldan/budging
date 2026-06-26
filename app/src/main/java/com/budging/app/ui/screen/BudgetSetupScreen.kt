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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.BudgetSetupState
import com.budging.app.ui.format.formatCurrency

@Composable
fun BudgetSetupScreen(
    state: BudgetSetupState,
    onSaveBudget: (name: String, totalAmountMinor: Long, currencyCode: String, startDateText: String, endDateText: String) -> Unit,
    onSaveCategory: (categoryId: Long?, name: String, allocatedAmountMinor: Long) -> Unit,
    onArchiveCategory: (categoryId: Long, isArchived: Boolean) -> Unit,
    onDeleteCategory: (categoryId: Long) -> Unit,
) {
    var budgetName by rememberSaveable(state.activePeriodId) { mutableStateOf(state.periodName) }
    var totalAmountText by rememberSaveable(state.activePeriodId) {
        mutableStateOf(state.totalAmountMinor.takeIf { it > 0 }?.toString().orEmpty())
    }
    var currencyCode by rememberSaveable(state.activePeriodId) { mutableStateOf(state.currencyCode) }
    var startDateText by rememberSaveable(state.activePeriodId) { mutableStateOf(state.startDateText) }
    var endDateText by rememberSaveable(state.activePeriodId) { mutableStateOf(state.endDateText) }

    var editingCategoryId by remember { mutableStateOf<Long?>(null) }
    var categoryName by rememberSaveable(state.activePeriodId, editingCategoryId) { mutableStateOf("") }
    var categoryAmountText by rememberSaveable(state.activePeriodId, editingCategoryId) { mutableStateOf("") }

    val totalForPreview = totalAmountText.toLongOrNull() ?: 0L
    val projectedUnallocated = totalForPreview - state.categories
        .filterNot { it.isArchived }
        .sumOf { item ->
            if (item.id == editingCategoryId) 0L else item.allocatedAmountMinor
        } - (categoryAmountText.toLongOrNull() ?: 0L)

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Set Budget", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = budgetName,
                        onValueChange = { budgetName = it },
                        label = { Text("Budget Period Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = totalAmountText,
                        onValueChange = { totalAmountText = it.filter(Char::isDigit) },
                        label = { Text("Total Budget") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = currencyCode,
                        onValueChange = { currencyCode = it.uppercase().take(3) },
                        label = { Text("Currency Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = startDateText,
                            onValueChange = { startDateText = it },
                            label = { Text("Start Date") },
                            modifier = Modifier.weight(1f),
                            supportingText = { Text("YYYY-MM-DD") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = endDateText,
                            onValueChange = { endDateText = it },
                            label = { Text("End Date") },
                            modifier = Modifier.weight(1f),
                            supportingText = { Text("YYYY-MM-DD") },
                            singleLine = true,
                        )
                    }
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
                    ) {
                        Text("Save Budget Period")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Unallocated", style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatCurrency(projectedUnallocated, currencyCode.ifBlank { "IDR" }),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Keep this at zero or above to avoid over-allocation.")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Category Allocation", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = categoryAmountText,
                        onValueChange = { categoryAmountText = it.filter(Char::isDigit) },
                        label = { Text("Allocated Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                onSaveCategory(
                                    editingCategoryId,
                                    categoryName,
                                    categoryAmountText.toLongOrNull() ?: 0L,
                                )
                                editingCategoryId = null
                                categoryName = ""
                                categoryAmountText = ""
                            },
                            enabled = state.hasActiveBudget,
                        ) {
                            Text(if (editingCategoryId == null) "Add Category" else "Update Category")
                        }
                        if (editingCategoryId != null) {
                            TextButton(
                                onClick = {
                                    editingCategoryId = null
                                    categoryName = ""
                                    categoryAmountText = ""
                                },
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                    if (!state.hasActiveBudget) {
                        Text("Save the budget period first, then add categories.")
                    }
                }
            }
        }
        item {
            Text("Categories", style = MaterialTheme.typography.titleLarge)
        }
        if (state.categories.isEmpty()) {
            item {
                Text("No categories yet.")
            }
        } else {
            items(state.categories) { category ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(category.name, style = MaterialTheme.typography.titleMedium)
                            Text(formatCurrency(category.allocatedAmountMinor, state.currencyCode))
                        }
                        Text(
                            if (category.isArchived) "Archived" else "Active",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (category.spentAmountMinor > 0) {
                            Text("Spent ${formatCurrency(category.spentAmountMinor, state.currencyCode)}")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    editingCategoryId = category.id
                                    categoryName = category.name
                                    categoryAmountText = category.allocatedAmountMinor.toString()
                                },
                            ) {
                                Text("Edit")
                            }
                            TextButton(
                                onClick = { onArchiveCategory(category.id, !category.isArchived) },
                            ) {
                                Text(if (category.isArchived) "Restore" else "Archive")
                            }
                            if (!category.hasTransactions) {
                                TextButton(
                                    onClick = { onDeleteCategory(category.id) },
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
