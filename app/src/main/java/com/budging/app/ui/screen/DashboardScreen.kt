package com.budging.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.DashboardState
import com.budging.app.ui.component.BudgetProgressBar
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.CategoryIconBubble
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.component.categoryAccent
import com.budging.app.ui.format.formatCurrency
import com.budging.app.ui.theme.BudgingTheme

@Composable
fun DashboardScreen(
    state: DashboardState,
    onOpenCategory: (Long) -> Unit,
    onViewAllTransactions: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
) {
    val spacing = BudgingTheme.spacing
    LazyColumn(
        modifier = Modifier.padding(horizontal = spacing.xl),
        contentPadding = PaddingValues(bottom = spacing.xxl + 56.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        if (!state.hasActiveBudget) {
            item { EmptyDashboard() }
            return@LazyColumn
        }

        item { SectionHeader(eyebrow = "Current Budget", title = state.periodName) }
        item { HeroCard(state) }
        item { SectionHeader(title = "Category Budgets") }
        if (state.categories.isEmpty()) {
            item {
                EmptyDashboardCard(
                    title = "No categories yet",
                    body = "Open Set Budget to add category allocations for this budget.",
                )
            }
        } else {
            items(state.categories) { category ->
                val accent = categoryAccent(category.name)
                BudgetScaffoldCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCategory(category.id) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        CategoryIconBubble(categoryName = category.name)
                        Surface(
                            color = accent.background,
                            contentColor = accent.tint,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                "${formatCurrency(category.remainingAmountMinor, state.currencyCode)} left",
                                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(category.name, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "${formatCurrency(category.spentAmountMinor, state.currencyCode)} / ${formatCurrency(category.allocatedAmountMinor, state.currencyCode)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BudgetProgressBar(
                        progress = category.progressPercent / 100f,
                        color = accent.tint,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
        item {
            SectionHeader(
                title = "Recent Spending",
                action = "View All",
                onAction = onViewAllTransactions,
            )
        }
        if (state.recentTransactions.isEmpty()) {
            item { EmptyDashboardCard("No expenses yet", "Your latest spending will appear here once you start logging expenses.") }
        } else {
            items(state.recentTransactions) { transaction ->
                BudgetScaffoldCard(
                    modifier = Modifier.clickable { onTransactionClick(transaction.id) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CategoryIconBubble(categoryName = transaction.title, modifier = Modifier.size(40.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(transaction.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (transaction.splitCount > 1) {
                                        "Paid ${formatCurrency(transaction.paidAmountMinor, state.currencyCode)} / This period ${formatCurrency(transaction.impactAmountMinor, state.currencyCode)} / Split ${transaction.splitCount} periods"
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
private fun HeroCard(state: DashboardState) {
    val spentProgress = progress(state.totalSpentMinor, state.totalBudgetMinor)
    val leftPercent = ((1f - spentProgress) * 100).toInt().coerceIn(0, 100)
    val spentPercent = (spentProgress * 100).toInt().coerceIn(0, 100)

    BudgetScaffoldCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Total Remaining", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatCurrency(state.totalRemainingMinor, state.currencyCode),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
            Text("of ${formatCurrency(state.totalBudgetMinor, state.currencyCode)} budget", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
        BudgetProgressBar(
            progress = spentProgress,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$leftPercent% Left", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$spentPercent% Spent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "${formatCurrency(state.safeDailyMinor, state.currencyCode)}/day safe daily spend / ${state.daysRemainingInclusive} days left",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                state.periodDateRangeLabel,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

private fun progress(spentMinor: Long, totalMinor: Long): Float {
    if (totalMinor <= 0) return 0f
    return (spentMinor.toFloat() / totalMinor.toFloat()).coerceIn(0f, 1f)
}
