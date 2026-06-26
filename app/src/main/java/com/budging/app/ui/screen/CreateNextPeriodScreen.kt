package com.budging.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.BudgetCategoryItem
import com.budging.app.data.model.PendingImpactDetail
import com.budging.app.data.model.PendingMatchStatus
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.format.formatCurrency
import com.budging.app.ui.theme.BudgingTheme

@Composable
fun CreateNextPeriodScreen(
    defaultName: String,
    defaultCurrencyCode: String,
    defaultStartDate: String,
    defaultEndDate: String,
    previousCategories: List<BudgetCategoryItem>,
    pendingImpacts: List<PendingImpactDetail>,
    activePeriodCurrency: String,
    onDeletePendingImpact: (Long) -> Unit = {},
    onSave: (
        name: String,
        totalAmountMinor: Long,
        currencyCode: String,
        startDateText: String,
        endDateText: String,
        copyCategoryIds: List<Long>,
        applyImpactIds: List<Long>,
        impactCategoryMapping: Map<Long, Long>,
    ) -> Unit,
) {
    val spacing = BudgingTheme.spacing
    var budgetName by rememberSaveable { mutableStateOf(defaultName) }
    var totalAmountText by rememberSaveable { mutableStateOf("") }
    var currencyCode by rememberSaveable { mutableStateOf(defaultCurrencyCode) }
    var startDateText by rememberSaveable { mutableStateOf(defaultStartDate) }
    var endDateText by rememberSaveable { mutableStateOf(defaultEndDate) }

    // Category copy selection
    val copySelection = remember { mutableStateMapOf<Long, Boolean>() }

    // Pending impact resolution
    val impactApplySelection = remember { mutableStateMapOf<Long, Boolean>() }
    val impactCategoryMapping = remember { mutableStateMapOf<Long, Long>() }

    // Delete confirmation
    var deleteConfirmImpactId by remember { mutableStateOf<Long?>(null) }

    val currency = currencyCode.ifBlank { activePeriodCurrency }.ifBlank { "IDR" }
    val totalForPreview = totalAmountText.toLongOrNull() ?: 0L

    // Compute which category names are being copied — used for match-status recalculation
    val copiedCategoryNames = remember(copySelection, previousCategories) {
        previousCategories
            .filter { copySelection[it.id] == true }
            .map { it.name.lowercase() }
            .toSet()
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = spacing.xl),
        contentPadding = PaddingValues(bottom = spacing.xxl + 88.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item { SectionHeader(title = "Create Next Period") }

        // Period details
        item {
            BudgetScaffoldCard {
                Text("New budget period details", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MinimalAmountInput(
                    currencyCode = currency,
                    value = totalAmountText,
                    onValueChange = { totalAmountText = it.filter(Char::isDigit) },
                )
                MinimalInputRow("Period Name", budgetName, modifier = Modifier.fillMaxWidth()) { budgetName = it }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    MinimalInputRow("Start Date", startDateText, "YYYY-MM-DD", Modifier.weight(1f)) { startDateText = it }
                    MinimalInputRow("End Date", endDateText, "YYYY-MM-DD", Modifier.weight(1f)) { endDateText = it }
                }
                MinimalInputRow("Currency", currencyCode, modifier = Modifier.fillMaxWidth()) {
                    currencyCode = it.uppercase().take(3)
                }
                Text(
                    "Budget: ${formatCurrency(totalForPreview, currency)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Copy categories from previous period
        if (previousCategories.isNotEmpty()) {
            item { SectionHeader(title = "Copy Categories") }
            item {
                BudgetScaffoldCard {
                    Text(
                        "Select categories to copy from the previous period:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    previousCategories.forEach { category ->
                        val selected = copySelection[category.id] ?: false
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(category.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    formatCurrency(category.allocatedAmountMinor, currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { copySelection[category.id] = !selected }) {
                                Text(if (selected) "Remove" else "Copy")
                            }
                        }
                    }
                }
            }
        }

        // Pending impacts
        if (pendingImpacts.isNotEmpty()) {
            item { SectionHeader(title = "Pending Impacts (${pendingImpacts.size})") }
            items(pendingImpacts) { impact ->
                // Recompute match against selected copy categories
                val matchingCopiedCategories = previousCategories
                    .filter { copySelection[it.id] == true }
                    .filter { it.name.equals(impact.categoryNameSnapshot, ignoreCase = true) }
                val effectiveMatch = when {
                    copiedCategoryNames.isEmpty() -> impact.matchStatus // no copies selected — show original
                    matchingCopiedCategories.isEmpty() -> PendingMatchStatus.NO_MATCH
                    matchingCopiedCategories.size == 1 -> PendingMatchStatus.MATCHED
                    else -> PendingMatchStatus.AMBIGUOUS
                }
                val effectiveMatchingName = if (matchingCopiedCategories.size == 1) matchingCopiedCategories.first().name
                    else impact.matchingCategoryName

                val applySelected = impactApplySelection[impact.impactId] ?: (effectiveMatch == PendingMatchStatus.MATCHED)
                val mappedCategoryId = impactCategoryMapping[impact.impactId]

                // Build list of copy categories for the mapping dropdown
                val copyTargetCategories = previousCategories.filter { copySelection[it.id] == true }

                BudgetScaffoldCard {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Text(impact.transactionTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            buildString {
                                append(formatCurrency(impact.amountMinor, currency))
                                append(" · ${impact.categoryNameSnapshot}")
                                impact.sourcePeriodName?.let { append(" · from $it") }
                                append(" · offset ${impact.plannedPeriodOffset}")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            when (effectiveMatch) {
                                PendingMatchStatus.MATCHED -> {
                                    val name = effectiveMatchingName ?: impact.categoryNameSnapshot
                                    "Auto-matched — will apply to \"$name\""
                                }
                                PendingMatchStatus.NO_MATCH -> "No matching category in new period"
                                PendingMatchStatus.AMBIGUOUS -> "Multiple matching categories"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = when (effectiveMatch) {
                                PendingMatchStatus.MATCHED -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            },
                        )

                        // Manual category picker for unmatched/ambiguous when apply is selected
                        if (effectiveMatch != PendingMatchStatus.MATCHED && applySelected && copyTargetCategories.isNotEmpty()) {
                            CategoryPicker(
                                categories = copyTargetCategories,
                                selectedCategoryId = mappedCategoryId,
                                currency = currency,
                                onSelect = { catId -> impactCategoryMapping[impact.impactId] = catId },
                            )
                        }

                        if (effectiveMatch != PendingMatchStatus.MATCHED && applySelected && copyTargetCategories.isEmpty()) {
                            Text(
                                "Select at least one category to copy above to enable mapping.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            TextButton(onClick = {
                                impactApplySelection[impact.impactId] = !applySelected
                                if (!applySelected && effectiveMatch != PendingMatchStatus.MATCHED) {
                                    impactCategoryMapping.remove(impact.impactId)
                                }
                            }) {
                                Text(if (applySelected) "Skip" else "Apply")
                            }
                            // Delete button with confirmation
                            TextButton(onClick = { deleteConfirmImpactId = impact.impactId }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Save button
        item {
            Button(
                onClick = {
                    onSave(
                        budgetName,
                        totalAmountText.toLongOrNull() ?: 0L,
                        currencyCode.ifBlank { "IDR" },
                        startDateText,
                        endDateText,
                        copySelection.filter { it.value }.keys.toList(),
                        impactApplySelection.filter { it.value }.keys.toList(),
                        impactCategoryMapping.toMap(),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Create Next Period", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    // Delete confirmation dialog
    deleteConfirmImpactId?.let { impactId ->
        val impact = pendingImpacts.find { it.impactId == impactId }
        AlertDialog(
            onDismissRequest = { deleteConfirmImpactId = null },
            title = { Text("Delete pending impact?") },
            text = {
                Text(
                    impact?.let { "${it.transactionTitle} — ${formatCurrency(it.amountMinor, currency)}" }
                        ?: "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePendingImpact(impactId)
                    deleteConfirmImpactId = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmImpactId = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun CategoryPicker(
    categories: List<BudgetCategoryItem>,
    selectedCategoryId: Long?,
    currency: String,
    onSelect: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BudgingTheme.spacing.xs)) {
        Text(
            "Map to category:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        categories.forEach { category ->
            val isSelected = selectedCategoryId == category.id
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(category.id) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        formatCurrency(category.allocatedAmountMinor, currency),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                decorationBox = { inner ->
                    if (value.isBlank()) {
                        Text("0", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.outline)
                    }
                    inner()
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
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                decorationBox = { inner ->
                    if (value.isBlank() && hint.isNotBlank()) {
                        Text(hint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                    }
                    inner()
                },
            )
        }
    }
}
