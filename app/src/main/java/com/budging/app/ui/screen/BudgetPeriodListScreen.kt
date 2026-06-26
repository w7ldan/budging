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
import androidx.compose.runtime.Composable
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
) {
    val spacing = BudgingTheme.spacing
    val activePeriod = periods.firstOrNull { it.isActive }

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
                PeriodCard(activePeriod)
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
                PeriodCard(period)
            }
        }
    }
}

@Composable
private fun PeriodCard(period: PeriodSummary) {
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
    }
}
