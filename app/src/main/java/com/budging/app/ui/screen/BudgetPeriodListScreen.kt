package com.budging.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.PeriodSummary
import com.budging.app.ui.component.BudgetMetricRow
import com.budging.app.ui.component.BudgetProgressBar
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.format.formatCurrency
import com.budging.app.ui.theme.BudgingTheme

@Composable
fun BudgetPeriodListScreen(
    periods: List<PeriodSummary>,
    onCreateNext: () -> Unit,
    onDeletePeriod: (periodId: Long, wasActive: Boolean) -> Unit,
) {
    val spacing = BudgingTheme.spacing
    val activePeriod = periods.firstOrNull { it.isActive }
    var pendingDeletePeriod by remember { mutableStateOf<PeriodSummary?>(null) }

    pendingDeletePeriod?.let { period ->
        DeleteBudgetDialog(
            title = if (period.isActive) "Cancel current budget?" else "Delete budget?",
            body = "This deletes ${period.name} and its transactions. This cannot be undone.",
            confirmLabel = "Delete Budget",
            onDismiss = { pendingDeletePeriod = null },
            onConfirm = {
                onDeletePeriod(period.id, period.isActive)
                pendingDeletePeriod = null
            },
        )
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = spacing.xl),
        contentPadding = PaddingValues(bottom = spacing.xxl + 88.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item { SectionHeader(title = "Budget Periods") }

        // Active period
        if (activePeriod != null) {
            item {
                SectionHeader(eyebrow = "Active", title = activePeriod.name)
                PeriodCard(activePeriod, onDelete = { pendingDeletePeriod = activePeriod })
            }
        }

        // Create Next button
        item {
            Button(
                onClick = onCreateNext,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    if (activePeriod == null) "Create Budget" else "Create Next Period",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        // Past periods
        val pastPeriods = periods.filter { !it.isActive }
        if (pastPeriods.isNotEmpty()) {
            item { SectionHeader(title = "Past Periods") }
            items(pastPeriods) { period ->
                PeriodCard(period, onDelete = { pendingDeletePeriod = period })
            }
        }
    }
}

@Composable
private fun PeriodCard(
    period: PeriodSummary,
    onDelete: () -> Unit,
) {
    val progressPercent = if (period.totalAmountMinor > 0) {
        ((period.spentAmountMinor * 100) / period.totalAmountMinor).toInt().coerceIn(0, 100)
    } else 0

    BudgetScaffoldCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(period.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    period.dateRangeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                color = if (period.isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    if (period.isActive) "Active" else "Archived",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (period.isActive) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        BudgetProgressBar(
            progress = progressPercent / 100f,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        BudgetMetricRow("Budget", formatCurrency(period.totalAmountMinor, period.currencyCode))
        BudgetMetricRow("Spent", formatCurrency(period.spentAmountMinor, period.currencyCode))
        BudgetMetricRow("Remaining", formatCurrency(period.remainingAmountMinor, period.currencyCode), strong = true)
        TextButton(
            onClick = onDelete,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Delete", color = MaterialTheme.colorScheme.error)
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
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
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
