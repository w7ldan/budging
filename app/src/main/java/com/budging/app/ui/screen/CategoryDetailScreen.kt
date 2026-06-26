package com.budging.app.ui.screen

import androidx.compose.foundation.clickable
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
    onTransactionClick: (Long) -> Unit = {},
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
            BudgetScaffoldCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                        CategoryIconBubble(state.categoryName, iconKey = state.iconKey)
                        Column {
                            Text(state.categoryName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text("Remaining for this budget period", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text(
                    formatCurrency(state.remainingAmountMinor, state.currencyCode),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                BudgetProgressBar(
                    progress = categoryProgress(state.spentAmountMinor, state.allocatedAmountMinor),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                BudgetMetricRow("Allocated", formatCurrency(state.allocatedAmountMinor, state.currencyCode))
                BudgetMetricRow("Spent", formatCurrency(state.spentAmountMinor, state.currencyCode))
                if (state.pendingImpactCount > 0) {
                    BudgetMetricRow("Pending future impacts", state.pendingImpactCount.toString())
                }
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
                BudgetScaffoldCard(
                    modifier = Modifier.clickable { onTransactionClick(transaction.id) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                            CategoryIconBubble(transaction.title, iconKey = transaction.categoryIconKey)
                            Column {
                                Text(transaction.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (transaction.splitCount > 1) {
                                        "Paid ${formatCurrency(transaction.paidAmountMinor, state.currencyCode)} · This period ${formatCurrency(transaction.impactAmountMinor, state.currencyCode)} · Split ${transaction.splitCount} periods"
                                    } else {
                                        transaction.note ?: transaction.paidDateLabel
                                    },
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

private fun categoryProgress(spentMinor: Long, allocatedMinor: Long): Float {
    if (allocatedMinor <= 0) return 0f
    return (spentMinor.toFloat() / allocatedMinor.toFloat()).coerceIn(0f, 1f)
}
