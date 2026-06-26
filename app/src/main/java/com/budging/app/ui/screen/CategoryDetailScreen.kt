package com.budging.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.CategoryDetailState
import com.budging.app.ui.component.BudgetMetricRow
import com.budging.app.ui.component.BudgetProgressBar
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.CategoryIconBubble
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.format.formatCurrency
import com.budging.app.ui.theme.BudgingTheme

@Composable
fun CategoryDetailScreen(
    state: CategoryDetailState?,
    onDeleteTransaction: (Long) -> Unit,
) {
    val spacing = BudgingTheme.spacing
    if (state == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text("Category Detail", style = MaterialTheme.typography.headlineMedium)
            Text("Open a category from the dashboard to load its spending history.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item {
            BudgetScaffoldCard(dark = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                        CategoryIconBubble(state.categoryName)
                        Column {
                            Text(state.categoryName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                            Text("Remaining for this budget period", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f))
                        }
                    }
                }
                Text(
                    formatCurrency(state.remainingAmountMinor, state.currencyCode),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                BudgetProgressBar(
                    progress = categoryProgress(state.spentAmountMinor, state.allocatedAmountMinor),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
                )
                BudgetMetricRow("Allocated", formatCurrency(state.allocatedAmountMinor, state.currencyCode))
                BudgetMetricRow("Spent", formatCurrency(state.spentAmountMinor, state.currencyCode))
            }
        }
        item { SectionHeader(title = "Recent Transactions") }
        if (state.transactions.isEmpty()) {
            item {
                BudgetScaffoldCard {
                    Text("No expenses in this category yet.", style = MaterialTheme.typography.titleMedium)
                    Text("Once you log spending here, it will appear in this list.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(state.transactions) { transaction ->
                BudgetScaffoldCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                            CategoryIconBubble(transaction.title)
                            Column {
                                Text(transaction.title, style = MaterialTheme.typography.titleMedium)
                                Text(transaction.note ?: transaction.paidDateLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (transaction.note != null) {
                                    Text(transaction.paidDateLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Text(
                            formatCurrency(transaction.amountMinor, state.currencyCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    TextButton(
                        onClick = { onDeleteTransaction(transaction.id) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

private fun categoryProgress(spentMinor: Long, allocatedMinor: Long): Float {
    if (allocatedMinor <= 0) return 0f
    return (spentMinor.toFloat() / allocatedMinor.toFloat()).coerceIn(0f, 1f)
}
