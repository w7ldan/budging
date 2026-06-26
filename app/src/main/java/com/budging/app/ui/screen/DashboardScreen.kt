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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.DashboardState
import com.budging.app.ui.format.formatIdr

@Composable
fun DashboardScreen(
    state: DashboardState,
    onOpenCategory: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(state.periodName, style = MaterialTheme.typography.headlineMedium)
        }
        item {
            SummaryCard(label = "Total Remaining", value = formatIdr(state.totalRemainingMinor))
        }
        item {
            SummaryCard(
                label = "Safe Daily Spend",
                value = "${formatIdr(state.safeDailyMinor)}/day",
            )
        }
        item {
            SummaryCard(
                label = "Days Left",
                value = state.daysRemainingInclusive.toString(),
            )
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(category.name, style = MaterialTheme.typography.titleMedium)
                        Text(formatIdr(category.remainingAmountMinor))
                    }
                    Text("Allocated ${formatIdr(category.allocatedAmountMinor)}")
                    Text("Spent ${formatIdr(category.spentAmountMinor)}")
                }
            }
        }
        item {
            Text("Recent Spending", style = MaterialTheme.typography.titleLarge)
        }
        items(state.recentTransactions) { transaction ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(transaction.title, style = MaterialTheme.typography.titleMedium)
                        Text(transaction.paidDateLabel)
                    }
                    Text(formatIdr(transaction.amountMinor))
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
