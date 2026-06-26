package com.budging.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.ExpenseEntryState
import com.budging.app.domain.SplitExpensePlanner
import com.budging.app.ui.component.BudgetChip
import com.budging.app.ui.component.BudgetMetricRow
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.DateInputRow
import com.budging.app.ui.component.Keypad
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.component.categoryIcon
import com.budging.app.ui.format.formatCurrency
import com.budging.app.domain.AppClock
import com.budging.app.ui.theme.BudgingTheme

@Composable
fun LogExpenseScreen(
    state: ExpenseEntryState,
    previewCurrentImpact: (amountMinor: Long, splitPeriodCount: Int) -> Long,
    onSaveExpense: (amountMinor: Long, categoryId: Long, dateText: String, note: String, splitPeriodCount: Int) -> Unit,
) {
    val spacing = BudgingTheme.spacing
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable(state.budgetName, state.categories.firstOrNull()?.id) {
        mutableStateOf<Long?>(state.categories.firstOrNull()?.id)
    }
    var dateText by rememberSaveable(state.budgetName) { mutableStateOf(AppClock.System.today().toString()) }
    var noteText by rememberSaveable { mutableStateOf("") }
    var splitPeriodCount by rememberSaveable { mutableStateOf(1) }

    val selectedCategory = state.categories.firstOrNull { it.id == selectedCategoryId }
    val amountMinor = amountText.toLongOrNull() ?: 0L
    val currentImpactAmount = previewCurrentImpact(amountMinor, splitPeriodCount)
    val predictedRemaining = selectedCategory?.remainingAmountMinor?.minus(currentImpactAmount)

    LazyColumn(
        modifier = Modifier.padding(horizontal = spacing.xl),
        contentPadding = PaddingValues(bottom = spacing.xxl + 88.dp),
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Text("Amount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formatCurrency(amountMinor, state.currencyCode),
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(state.dateRangeLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.pendingImpactCount > 0) {
            item {
                BudgetScaffoldCard {
                    Text("Pending future impacts", style = MaterialTheme.typography.titleMedium)
                    Text("${state.pendingImpactCount} split impact(s) are waiting for future budget periods or matching future categories.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (state.categories.isEmpty()) {
            item {
                CompactEmptyCard(
                    title = "No categories yet",
                    body = "Add categories in Set Budget before logging expenses.",
                )
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    items(state.categories) { category ->
                        BudgetChip(
                            selected = selectedCategoryId == category.id,
                            label = category.name,
                            icon = categoryIcon(category.iconKey),
                            onClick = { selectedCategoryId = category.id },
                        )
                    }
                }
            }
        }
        item {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Advanced", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (splitPeriodCount == 1) "Count all now" else "Split $splitPeriodCount periods", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                        SplitModeChip(selected = splitPeriodCount == 1, label = "Count all now", onClick = { splitPeriodCount = 1 })
                        SplitModeChip(selected = splitPeriodCount > 1, label = "Split across periods", onClick = { if (splitPeriodCount == 1) splitPeriodCount = 2 })
                    }
                    if (splitPeriodCount > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Periods", style = MaterialTheme.typography.bodyLarge)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { splitPeriodCount = (splitPeriodCount - 1).coerceAtLeast(2) }) { Text("-") }
                                Text("$splitPeriodCount", style = MaterialTheme.typography.titleLarge)
                                TextButton(onClick = { splitPeriodCount = (splitPeriodCount + 1).coerceAtMost(SplitExpensePlanner.MAX_SPLIT_PERIODS) }) { Text("+") }
                            }
                        }
                        BudgetMetricRow("Per-period impact", formatCurrency(currentImpactAmount, state.currencyCode))
                        BudgetMetricRow("Current period impact", formatCurrency(currentImpactAmount, state.currencyCode), strong = true)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                DateInputRow("Date", dateText, Modifier.weight(1f)) { dateText = it }
                CompactInputCard(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Outlined.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    label = "Note",
                    value = noteText,
                    hint = "Optional",
                    onValueChange = { noteText = it },
                )
            }
        }
        item {
            Keypad(
                onDigit = { digit -> amountText += digit },
                onDelete = { amountText = if (amountText.isNotEmpty()) amountText.dropLast(1) else "" },
                onClear = { amountText = "" },
            )
        }
        item {
            Button(
                onClick = { onSaveExpense(amountMinor, selectedCategoryId ?: 0L, dateText, noteText, splitPeriodCount) },
                modifier = Modifier.fillMaxWidth().height(68.dp),
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
                            "${selectedCategory?.name.orEmpty()} will have ${formatCurrency(predictedRemaining, state.currencyCode)} left this period"
                        } else {
                            "Choose a category to continue"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitModeChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun CompactInputCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { inner ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (value.isBlank()) {
                                Text(hint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                            }
                            inner()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CompactEmptyCard(
    title: String,
    body: String,
) {
    BudgetScaffoldCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
