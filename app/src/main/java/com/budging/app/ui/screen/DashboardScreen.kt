package com.budging.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.budging.app.data.model.DashboardState
import com.budging.app.ui.component.BudgetMetricRow
import com.budging.app.ui.component.BudgetProgressBar
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.CategoryIconBubble
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.theme.BudgingTheme
import com.budging.app.ui.format.formatCurrency
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    state: DashboardState,
    onOpenCategory: (Long) -> Unit,
) {
    val spacing = BudgingTheme.spacing
    LazyColumn(
        modifier = Modifier.padding(horizontal = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        if (!state.hasActiveBudget) {
            item { EmptyDashboard() }
            return@LazyColumn
        }

        item {
            SectionHeader(eyebrow = "Current Budget", title = state.periodName)
        }
        item {
            BudgetScaffoldCard(dark = true) {
                Text("Total Remaining", style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)))
                Text(
                    formatCurrency(state.totalRemainingMinor, state.currencyCode),
                    style = MaterialTheme.typography.displayLarge,
                    maxLines = 1,
                )
                Text(
                    state.periodDateRangeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                )
                BudgetProgressBar(
                    progress = progress(state.totalSpentMinor, state.totalBudgetMinor),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Safe Daily Spend", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f))
                        Text(
                            "${formatCurrency(state.safeDailyMinor, state.currencyCode)}/day",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Days Left", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f))
                        Text("${state.daysRemainingInclusive}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        item {
            BudgetScaffoldCard {
                SectionHeader(title = "Budget Progress")
                BudgetMetricRow("Total Budget", formatCurrency(state.totalBudgetMinor, state.currencyCode), strong = true)
                BudgetMetricRow("Spent This Period", formatCurrency(state.totalSpentMinor, state.currencyCode))
                BudgetMetricRow("Unallocated", formatCurrency(state.unallocatedAmountMinor, state.currencyCode))
            }
        }
        item {
            SectionHeader(title = "Categories")
        }
        items(state.categories) { category ->
            val accent = com.budging.app.ui.component.categoryAccent(category.name)
            BudgetScaffoldCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { onOpenCategory(category.id) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                        CategoryIconBubble(categoryName = category.name)
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                            Text(category.name, style = MaterialTheme.typography.titleLarge)
                            Text("Allocated ${formatCurrency(category.allocatedAmountMinor, state.currencyCode)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        color = accent.background,
                        contentColor = accent.tint,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            formatCurrency(category.remainingAmountMinor, state.currencyCode),
                            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                BudgetProgressBar(
                    progress = category.progressPercent / 100f,
                    color = accent.tint,
                    trackColor = accent.background,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    item { CompactMetric("Spent", formatCurrency(category.spentAmountMinor, state.currencyCode)) }
                    item { CompactMetric("Remaining", formatCurrency(category.remainingAmountMinor, state.currencyCode)) }
                    item { CompactMetric("Progress", "${category.progressPercent}%") }
                }
            }
        }
        item {
            SectionHeader(title = "Recent Spending")
        }
        if (state.recentTransactions.isEmpty()) {
            item { EmptyDashboardCard("No expenses yet", "Your latest spending will appear here once you start logging expenses.") }
        } else {
            items(state.recentTransactions) { transaction ->
                BudgetScaffoldCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                            CategoryIconBubble(categoryName = transaction.title, modifier = Modifier.size(40.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(transaction.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (transaction.splitCount > 1) {
                                        "Paid ${formatCurrency(transaction.paidAmountMinor, state.currencyCode)} · This period ${formatCurrency(transaction.impactAmountMinor, state.currencyCode)} · Split ${transaction.splitCount} periods"
                                    } else {
                                        transaction.note ?: transaction.paidDateLabel
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (transaction.note != null || transaction.splitCount > 1) {
                                    Text(transaction.paidDateLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Text(
                            formatCurrency(transaction.impactAmountMinor, state.currencyCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDashboard() {
    EmptyDashboardCard(
        title = "No active budget",
        body = "Open Set Budget to create a budget period, allocate categories, and start logging expenses.",
    )
}

@Composable
private fun EmptyDashboardCard(title: String, body: String) {
    BudgetScaffoldCard {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CompactMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

private fun progress(spentMinor: Long, totalMinor: Long): Float {
    if (totalMinor <= 0) return 0f
    return (spentMinor.toFloat() / totalMinor.toFloat()).coerceIn(0f, 1f)
}
