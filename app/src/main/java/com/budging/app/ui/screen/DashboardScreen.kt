package com.budging.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.DashboardState
import com.budging.app.ui.format.formatCurrency

@Composable
fun DashboardScreen(
    state: DashboardState,
    onOpenCategory: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!state.hasActiveBudget) {
            item {
                EmptyDashboard()
            }
            return@LazyColumn
        }

        item {
            Text("Current Budget", style = MaterialTheme.typography.titleMedium)
            Text(state.periodName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(state.periodDateRangeLabel, style = MaterialTheme.typography.bodyMedium)
        }
        item {
            HeroBudgetCard(
                title = "Total Remaining",
                value = formatCurrency(state.totalRemainingMinor, state.currencyCode),
                supporting = "Safe Daily Spend ${formatCurrency(state.safeDailyMinor, state.currencyCode)}/day",
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Budget Progress", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = {
                            progress(state.totalSpentMinor, state.totalBudgetMinor)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MetricRow("Total Budget", formatCurrency(state.totalBudgetMinor, state.currencyCode))
                    MetricRow("Spent This Period", formatCurrency(state.totalSpentMinor, state.currencyCode))
                    MetricRow("Days Left", state.daysRemainingInclusive.toString())
                    MetricRow("Unallocated", formatCurrency(state.unallocatedAmountMinor, state.currencyCode))
                }
            }
        }
        item {
            Text("Categories", style = MaterialTheme.typography.titleLarge)
        }
        items(state.categories) { category ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenCategory(category.id) },
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(category.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatCurrency(category.remainingAmountMinor, state.currencyCode),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (category.progressPercent.coerceAtMost(100) / 100f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MetricRow("Allocated", formatCurrency(category.allocatedAmountMinor, state.currencyCode))
                    MetricRow("Spent", formatCurrency(category.spentAmountMinor, state.currencyCode))
                    MetricRow("Remaining", formatCurrency(category.remainingAmountMinor, state.currencyCode))
                }
            }
        }
        item {
            Text("Recent Spending", style = MaterialTheme.typography.titleLarge)
        }
        if (state.recentTransactions.isEmpty()) {
            item {
                Text("No expenses yet.")
            }
        } else {
            items(state.recentTransactions) { transaction ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(transaction.title, style = MaterialTheme.typography.titleMedium)
                            transaction.note?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(transaction.paidDateLabel, style = MaterialTheme.typography.labelMedium)
                        }
                        Text(formatCurrency(transaction.amountMinor, state.currencyCode))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDashboard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("No active budget", style = MaterialTheme.typography.headlineSmall)
            Text("Open Set Budget to create a budget period, allocate categories, and start logging expenses.")
        }
    }
}

@Composable
private fun HeroBudgetCard(title: String, value: String, supporting: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(supporting, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun progress(spentMinor: Long, totalMinor: Long): Float {
    if (totalMinor <= 0) return 0f
    return (spentMinor.toFloat() / totalMinor.toFloat()).coerceIn(0f, 1f)
}
