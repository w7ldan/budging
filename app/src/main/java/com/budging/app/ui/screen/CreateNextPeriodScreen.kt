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
import com.budging.app.data.model.RecurringPreviewItem
import com.budging.app.data.model.RecurringTemplateItem
import com.budging.app.domain.RecurringExpensePlanner
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.CategoryIconBubble
import com.budging.app.ui.component.DateInputRow
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.format.formatCurrency
import com.budging.app.ui.theme.BudgingTheme
import java.time.LocalDate

@Composable
fun CreateNextPeriodScreen(
    defaultName: String,
    defaultCurrencyCode: String,
    defaultStartDate: String,
    defaultEndDate: String,
    previousCategories: List<BudgetCategoryItem>,
    pendingImpacts: List<PendingImpactDetail>,
    recurringTemplates: List<RecurringTemplateItem>,
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
        applyRecurringPreviewKeys: List<String>,
        recurringCategoryMapping: Map<String, Long>,
    ) -> Unit,
) {
    val spacing = BudgingTheme.spacing
    var budgetName by rememberSaveable { mutableStateOf(defaultName) }
    var totalAmountText by rememberSaveable { mutableStateOf("") }
    var currencyCode by rememberSaveable { mutableStateOf(defaultCurrencyCode) }
    var startDateText by rememberSaveable { mutableStateOf(defaultStartDate) }
    var endDateText by rememberSaveable { mutableStateOf(defaultEndDate) }

    val copySelection = remember { mutableStateMapOf<Long, Boolean>() }
    val impactApplySelection = remember { mutableStateMapOf<Long, Boolean>() }
    val impactCategoryMapping = remember { mutableStateMapOf<Long, Long>() }
    val recurringApplySelection = remember { mutableStateMapOf<String, Boolean>() }
    val recurringCategoryMapping = remember { mutableStateMapOf<String, Long>() }
    var deleteConfirmImpactId by remember { mutableStateOf<Long?>(null) }

    val currency = currencyCode.ifBlank { activePeriodCurrency }.ifBlank { "IDR" }
    val totalForPreview = totalAmountText.toLongOrNull() ?: 0L
    val copiedCategories = previousCategories.filter { copySelection[it.id] == true }
    val copiedNames = copiedCategories.map { it.name.lowercase() }.toSet()
    val recurringPreviewItems = remember(recurringTemplates, copiedCategories, startDateText, endDateText, currency) {
        val start = runCatching { LocalDate.parse(startDateText) }.getOrNull()
        val end = runCatching { LocalDate.parse(endDateText) }.getOrNull()
        if (start == null || end == null || end.isBefore(start)) {
            emptyList()
        } else {
            recurringTemplates.flatMap { template ->
                val entityLike = com.budging.app.data.local.entity.RecurringExpenseTemplateEntity(
                    id = template.id,
                    title = template.title,
                    amountMinor = template.amountMinor,
                    currencyCode = template.currencyCode,
                    categoryNameSnapshot = template.categoryNameSnapshot,
                    iconKey = template.iconKey,
                    note = template.note,
                    frequency = template.frequency,
                    startDate = template.startDate,
                    endDate = template.endDate,
                    dayOfMonth = template.dayOfMonth,
                    applyMode = "CONFIRM",
                    isActive = template.isActive,
                    createdAtEpochMillis = 0,
                    updatedAtEpochMillis = 0,
                )
                RecurringExpensePlanner.occurrencesForPeriod(entityLike, start, end).map { occurrence ->
                    val matches = copiedCategories.filter { it.name.equals(template.categoryNameSnapshot, ignoreCase = true) }
                    RecurringPreviewItem(
                        previewKey = "${template.id}:${occurrence.occurrenceDate}",
                        templateId = template.id,
                        title = template.title,
                        amountMinor = template.amountMinor,
                        currencyCode = currency,
                        categoryNameSnapshot = template.categoryNameSnapshot,
                        iconKey = template.iconKey,
                        occurrenceDate = occurrence.occurrenceDate,
                        matchStatus = when {
                            matches.isEmpty() -> PendingMatchStatus.NO_MATCH
                            matches.size == 1 -> PendingMatchStatus.MATCHED
                            else -> PendingMatchStatus.AMBIGUOUS
                        },
                        matchingCategoryId = matches.singleOrNull()?.id,
                        matchingCategoryName = matches.singleOrNull()?.name,
                    )
                }
            }.sortedWith(compareBy({ it.occurrenceDate }, { it.title.lowercase() }))
        }
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = spacing.xl),
        contentPadding = PaddingValues(bottom = spacing.xxl + 88.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item { SectionHeader(title = "Create Next Period") }
        item {
            BudgetScaffoldCard {
                Text("New budget period details", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MinimalAmountInput(currency, totalAmountText) { totalAmountText = it.filter(Char::isDigit) }
                MinimalInputRow("Period Name", budgetName, modifier = Modifier.fillMaxWidth()) { budgetName = it }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    DateInputRow("Start Date", startDateText, Modifier.weight(1f)) { startDateText = it }
                    DateInputRow("End Date", endDateText, Modifier.weight(1f)) { endDateText = it }
                }
                MinimalInputRow("Currency", currencyCode, modifier = Modifier.fillMaxWidth()) { currencyCode = it.uppercase().take(3) }
                Text("Budget: ${formatCurrency(totalForPreview, currency)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }

        if (previousCategories.isNotEmpty()) {
            item { SectionHeader(title = "Copy Categories") }
            item {
                BudgetScaffoldCard {
                    previousCategories.forEach { category ->
                        val selected = copySelection[category.id] ?: false
                        SelectableCategoryRow(
                            category = category,
                            currency = currency,
                            selected = selected,
                            onToggle = { copySelection[category.id] = !selected },
                        )
                    }
                }
            }
        }

        if (pendingImpacts.isNotEmpty()) {
            item { SectionHeader(title = "Pending Impacts (${pendingImpacts.size})") }
            items(pendingImpacts) { impact ->
                val matchingCopiedCategories = copiedCategories.filter { it.name.equals(impact.categoryNameSnapshot, ignoreCase = true) }
                val effectiveMatch = when {
                    copiedNames.isEmpty() -> impact.matchStatus
                    matchingCopiedCategories.isEmpty() -> PendingMatchStatus.NO_MATCH
                    matchingCopiedCategories.size == 1 -> PendingMatchStatus.MATCHED
                    else -> PendingMatchStatus.AMBIGUOUS
                }
                val applySelected = impactApplySelection[impact.impactId] ?: (effectiveMatch == PendingMatchStatus.MATCHED)
                val mappedCategoryId = impactCategoryMapping[impact.impactId]
                BudgetScaffoldCard {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Text(impact.transactionTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${formatCurrency(impact.amountMinor, currency)} · ${impact.categoryNameSnapshot} · offset ${impact.plannedPeriodOffset}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        MatchStatusLabel(effectiveMatch, matchingCopiedCategories.singleOrNull()?.name ?: impact.matchingCategoryName)
                        if (effectiveMatch != PendingMatchStatus.MATCHED && applySelected && copiedCategories.isNotEmpty()) {
                            CategoryPicker(copiedCategories, mappedCategoryId, currency) { impactCategoryMapping[impact.impactId] = it }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            TextButton(onClick = { impactApplySelection[impact.impactId] = !applySelected }) {
                                Text(if (applySelected) "Skip" else "Apply")
                            }
                            TextButton(onClick = { deleteConfirmImpactId = impact.impactId }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        if (recurringPreviewItems.isNotEmpty()) {
            item { SectionHeader(title = "Recurring Expenses (${recurringPreviewItems.size})") }
            items(recurringPreviewItems) { recurring ->
                val applySelected = recurringApplySelection[recurring.previewKey] ?: (recurring.matchStatus == PendingMatchStatus.MATCHED)
                val mappedCategoryId = recurringCategoryMapping[recurring.previewKey]
                BudgetScaffoldCard {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                            CategoryIconBubble(recurring.categoryNameSnapshot, iconKey = recurring.iconKey)
                            Column {
                                Text(recurring.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${formatCurrency(recurring.amountMinor, currency)} · ${recurring.occurrenceDate}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        MatchStatusLabel(recurring.matchStatus, recurring.matchingCategoryName)
                        if (recurring.matchStatus != PendingMatchStatus.MATCHED && applySelected && copiedCategories.isNotEmpty()) {
                            CategoryPicker(copiedCategories, mappedCategoryId, currency) { recurringCategoryMapping[recurring.previewKey] = it }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            TextButton(onClick = { recurringApplySelection[recurring.previewKey] = !applySelected }) {
                                Text(if (applySelected) "Skip" else "Apply")
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    onSave(
                        budgetName,
                        totalAmountText.toLongOrNull() ?: 0L,
                        currencyCode.ifBlank { "IDR" },
                        startDateText,
                        endDateText,
                        copySelection.filterValues { it }.keys.toList(),
                        impactApplySelection.filterValues { it }.keys.toList(),
                        impactCategoryMapping.toMap(),
                        recurringApplySelection.filterValues { it }.keys.toList(),
                        recurringCategoryMapping.toMap(),
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

    deleteConfirmImpactId?.let { impactId ->
        val impact = pendingImpacts.find { it.impactId == impactId }
        AlertDialog(
            onDismissRequest = { deleteConfirmImpactId = null },
            title = { Text("Delete pending impact?") },
            text = { Text(impact?.let { "${it.transactionTitle} — ${formatCurrency(it.amountMinor, currency)}" } ?: "This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePendingImpact(impactId)
                    deleteConfirmImpactId = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteConfirmImpactId = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MatchStatusLabel(
    status: PendingMatchStatus,
    matchedName: String?,
) {
    val text = when (status) {
        PendingMatchStatus.MATCHED -> "Auto-matched to \"${matchedName.orEmpty()}\""
        PendingMatchStatus.NO_MATCH -> "No matching category in new period"
        PendingMatchStatus.AMBIGUOUS -> "Multiple matching categories"
    }
    val color = if (status == PendingMatchStatus.MATCHED) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
    val background = if (status == PendingMatchStatus.MATCHED) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
    Surface(shape = RoundedCornerShape(999.dp), color = background) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun SelectableCategoryRow(
    category: BudgetCategoryItem,
    currency: String,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryIconBubble(category.name, iconKey = category.iconKey)
                Column {
                    Text(category.name, style = MaterialTheme.typography.titleMedium)
                    Text(formatCurrency(category.allocatedAmountMinor, currency), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(if (selected) "Copied" else "Copy", color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary)
        }
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
        Text("Map to category:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        categories.forEach { category ->
            val isSelected = selectedCategoryId == category.id
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(category.id) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CategoryIconBubble(category.name, iconKey = category.iconKey, modifier = Modifier.padding(0.dp))
                        Text(
                            category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(formatCurrency(category.allocatedAmountMinor, currency), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    if (value.isBlank()) Text("0", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.outline)
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
                    if (value.isBlank() && hint.isNotBlank()) Text(hint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                    inner()
                },
            )
        }
    }
}
