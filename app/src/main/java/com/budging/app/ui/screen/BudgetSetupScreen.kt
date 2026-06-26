package com.budging.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.BudgetSetupState
import com.budging.app.ui.component.BudgetMetricRow
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.CategoryIconBubble
import com.budging.app.ui.component.DateInputRow
import com.budging.app.ui.component.IconDropdownField
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.component.resolveCategoryIconKey
import com.budging.app.ui.format.DigitGroupingVisualTransformation
import com.budging.app.ui.format.formatCurrency
import com.budging.app.ui.theme.BudgingTheme

@Composable
fun BudgetSetupScreen(
    state: BudgetSetupState,
    onSaveBudget: (name: String, totalAmountMinor: Long, currencyCode: String, startDateText: String, endDateText: String) -> Unit,
    onSaveCategory: (categoryId: Long?, name: String, allocatedAmountMinor: Long, iconKey: String) -> Unit,
    onArchiveCategory: (categoryId: Long, isArchived: Boolean) -> Unit,
    onDeleteCategory: (categoryId: Long) -> Unit,
    onDeleteBudget: (periodId: Long, wasActive: Boolean) -> Unit,
    onTopUpBudget: (amountMinor: Long) -> Unit = {},
) {
    val spacing = BudgingTheme.spacing
    var budgetName by rememberSaveable(state.activePeriodId) { mutableStateOf(state.periodName) }
    var totalAmountText by rememberSaveable(state.activePeriodId) { mutableStateOf(state.totalAmountMinor.takeIf { it > 0 }?.toString().orEmpty()) }
    var currencyCode by rememberSaveable(state.activePeriodId) { mutableStateOf(state.currencyCode) }
    var startDateText by rememberSaveable(state.activePeriodId) { mutableStateOf(state.startDateText) }
    var endDateText by rememberSaveable(state.activePeriodId) { mutableStateOf(state.endDateText) }
    var editingCategoryId by remember { mutableStateOf<Long?>(null) }
    var categoryName by rememberSaveable(state.activePeriodId) { mutableStateOf("") }
    var categoryAmountText by rememberSaveable(state.activePeriodId) { mutableStateOf("") }
    var categoryIconKey by rememberSaveable(state.activePeriodId) { mutableStateOf("other") }
    var editCategoryName by rememberSaveable(state.activePeriodId, editingCategoryId) { mutableStateOf("") }
    var editCategoryAmountText by rememberSaveable(state.activePeriodId, editingCategoryId) { mutableStateOf("") }
    var editCategoryIconKey by rememberSaveable(state.activePeriodId, editingCategoryId) { mutableStateOf("other") }
    var showDeleteBudgetConfirm by remember { mutableStateOf(false) }
    var showTopUpDialog by remember { mutableStateOf(false) }
    var topUpAmountText by remember { mutableStateOf("") }

    val totalForPreview = totalAmountText.toLongOrNull() ?: 0L
    val projectedUnallocated = totalForPreview - state.categories
        .filterNot { it.isArchived }
        .sumOf { it.allocatedAmountMinor } - (categoryAmountText.toLongOrNull() ?: 0L)

    fun dismissEditDialog() {
        editingCategoryId = null
        editCategoryName = ""
        editCategoryAmountText = ""
        editCategoryIconKey = "other"
    }

    if (editingCategoryId != null) {
        CategoryEditDialog(
            name = editCategoryName,
            amountText = editCategoryAmountText,
            iconKey = editCategoryIconKey,
            onNameChange = { editCategoryName = it },
            onAmountChange = { editCategoryAmountText = it.filter(Char::isDigit) },
            onIconChange = { editCategoryIconKey = it },
            onDismiss = ::dismissEditDialog,
            onSave = {
                onSaveCategory(
                    editingCategoryId,
                    editCategoryName,
                    editCategoryAmountText.toLongOrNull() ?: 0L,
                    editCategoryIconKey.ifBlank { resolveCategoryIconKey(editCategoryName) },
                )
                dismissEditDialog()
            },
        )
    }
    if (showDeleteBudgetConfirm && state.activePeriodId != null) {
        DeleteBudgetDialog(
            title = "Cancel current budget?",
            body = "This deletes the current budget period and its transactions. This cannot be undone.",
            confirmLabel = "Delete Budget",
            onDismiss = { showDeleteBudgetConfirm = false },
            onConfirm = {
                onDeleteBudget(state.activePeriodId, true)
                showDeleteBudgetConfirm = false
            },
        )
    }
    if (showTopUpDialog) {
        TopUpDialog(
            currencyCode = currencyCode.ifBlank { "IDR" },
            amountText = topUpAmountText,
            onAmountChange = { topUpAmountText = it.filter(Char::isDigit) },
            onDismiss = {
                showTopUpDialog = false
                topUpAmountText = ""
            },
            onConfirm = {
                val amount = topUpAmountText.toLongOrNull() ?: 0L
                if (amount > 0) {
                    onTopUpBudget(amount)
                }
                showTopUpDialog = false
                topUpAmountText = ""
            },
        )
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = spacing.xl),
        contentPadding = PaddingValues(bottom = spacing.xxl + 88.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item { SectionHeader(eyebrow = "Budget Period", title = "Set Budget") }
        item {
            BudgetScaffoldCard {
                Text("How much should this budget period last?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MinimalAmountInput(
                    currencyCode = currencyCode.ifBlank { "IDR" },
                    value = totalAmountText,
                    onValueChange = { totalAmountText = it.filter(Char::isDigit) },
                )
                MinimalInputRow("Budget Period Name", budgetName, modifier = Modifier.fillMaxWidth()) { budgetName = it }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    DateInputRow("Start Date", startDateText, Modifier.weight(1f)) { startDateText = it }
                    DateInputRow("End Date", endDateText, Modifier.weight(1f)) { endDateText = it }
                }
                MinimalInputRow("Currency", currencyCode, modifier = Modifier.fillMaxWidth()) {
                    currencyCode = it.uppercase().take(3)
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
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Save Setup", style = MaterialTheme.typography.labelLarge)
                }
                if (state.activePeriodId != null) {
                    OutlinedButton(
                        onClick = { showTopUpDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Top Up Budget")
                    }
                }
                if (state.activePeriodId != null) {
                    TextButton(
                        onClick = { showDeleteBudgetConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Delete Budget", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item {
            BudgetScaffoldCard {
                Text("Unallocated Balance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                MinimalInputRow("Category Name", categoryName, modifier = Modifier.fillMaxWidth()) { categoryName = it }
                IconDropdownField(
                    label = "Category Icon",
                    selectedIconKey = categoryIconKey,
                    modifier = Modifier.fillMaxWidth(),
                    onSelect = { categoryIconKey = it },
                )
                MinimalInputRow("Allocated Amount", categoryAmountText, modifier = Modifier.fillMaxWidth(), keyboardType = KeyboardType.Number) {
                    categoryAmountText = it.filter(Char::isDigit)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    Button(
                        onClick = {
                            onSaveCategory(
                                null,
                                categoryName,
                                categoryAmountText.toLongOrNull() ?: 0L,
                                categoryIconKey.ifBlank { resolveCategoryIconKey(categoryName) },
                            )
                            categoryName = ""
                            categoryAmountText = ""
                            categoryIconKey = "other"
                        },
                        enabled = state.hasActiveBudget,
                    ) {
                        Text("Add Category")
                    }
                }
                if (!state.hasActiveBudget) {
                    Text("Save the budget period first, then add categories.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { SectionHeader(title = "Category Allocation") }
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
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                            CategoryIconBubble(category.name, iconKey = category.iconKey)
                            Column {
                                Text(category.name, style = MaterialTheme.typography.titleMedium)
                                Text(if (category.isArchived) "Archived" else "Active", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            editCategoryName = category.name
                            editCategoryAmountText = category.allocatedAmountMinor.toString()
                            editCategoryIconKey = category.iconKey
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

@Composable
private fun CategoryEditDialog(
    name: String,
    amountText: String,
    iconKey: String,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Edit Category", style = MaterialTheme.typography.titleLarge)
                MinimalInputRow("Category Name", name, modifier = Modifier.fillMaxWidth(), onValueChange = onNameChange)
                IconDropdownField(
                    label = "Category Icon",
                    selectedIconKey = iconKey,
                    modifier = Modifier.fillMaxWidth(),
                    onSelect = onIconChange,
                )
                MinimalInputRow(
                    "Allocated Amount",
                    amountText,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardType = KeyboardType.Number,
                    onValueChange = onAmountChange,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = onSave, shape = RoundedCornerShape(14.dp)) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteBudgetDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text(confirmLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopUpDialog(
    currencyCode: String,
    amountText: String,
    onAmountChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Top Up Budget", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Add extra funds to your active budget period. This increases the total budget and unallocated balance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MinimalAmountInput(
                    currencyCode = currencyCode.ifBlank { "IDR" },
                    value = amountText,
                    onValueChange = onAmountChange,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(14.dp),
                        enabled = (amountText.toLongOrNull() ?: 0L) > 0,
                    ) {
                        Text("Top Up")
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalAmountInput(
    currencyCode: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            Text(currencyCode, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = DigitGroupingVisualTransformation,
                decorationBox = { inner ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isBlank()) {
                            Text("0", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.outline)
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun MinimalInputRow(
    label: String,
    value: String,
    hint: String = "",
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (keyboardType == KeyboardType.Number) DigitGroupingVisualTransformation else VisualTransformation.None,
                decorationBox = { inner ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isBlank() && hint.isNotBlank()) {
                            Text(hint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                        }
                        inner()
                    }
                },
            )
        }
    }
}
