package com.budging.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.CategoryDetailState
import com.budging.app.ui.format.formatCurrency

@Composable
fun CategoryDetailScreen(
    state: CategoryDetailState?,
    onDeleteTransaction: (Long) -> Unit,
) {
    if (state == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Category Detail", style = MaterialTheme.typography.headlineMedium)
            Text("Open a category from the dashboard to load its spending history.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(state.categoryName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatCurrency(state.remainingAmountMinor, state.currencyCode),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Remaining for this budget period")
                    LinearProgressIndicator(
                        progress = {
                            categoryProgress(state.spentAmountMinor, state.allocatedAmountMinor)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MetricRow("Allocated", formatCurrency(state.allocatedAmountMinor, state.currencyCode))
                    MetricRow("Spent", formatCurrency(state.spentAmountMinor, state.currencyCode))
                }
            }
        }
        item {
            Text("Recent Transactions", style = MaterialTheme.typography.titleLarge)
        }
        if (state.transactions.isEmpty()) {
            item {
                Text("No expenses in this category yet.")
            }
        } else {
            items(state.transactions) { transaction ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(transaction.title, style = MaterialTheme.typography.titleMedium)
                            Text(formatCurrency(transaction.amountMinor, state.currencyCode))
                        }
                        transaction.note?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(transaction.paidDateLabel, style = MaterialTheme.typography.labelMedium)
                        TextButton(
                            onClick = { onDeleteTransaction(transaction.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Delete Transaction")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun categoryProgress(spentMinor: Long, allocatedMinor: Long): Float {
    if (allocatedMinor <= 0) return 0f
    return (spentMinor.toFloat() / allocatedMinor.toFloat()).coerceIn(0f, 1f)
}
