package com.budging.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budging.app.data.local.query.TransactionHistoryRow
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.CategoryIconBubble
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.format.formatCurrency
import com.budging.app.ui.theme.BudgingTheme
import java.time.format.DateTimeFormatter

@Composable
fun TransactionHistoryScreen(
    transactions: List<TransactionHistoryRow>,
    currencyCode: String,
    onTransactionClick: (Long) -> Unit,
) {
    val spacing = BudgingTheme.spacing
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    LazyColumn(
        modifier = Modifier.padding(horizontal = spacing.xl),
        contentPadding = PaddingValues(bottom = spacing.xxl + 56.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item { SectionHeader(title = "All Transactions") }
        if (transactions.isEmpty()) {
            item {
                BudgetScaffoldCard {
                    Text("No transactions yet.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Your spending history will appear here once you start logging expenses.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(transactions) { transaction ->
                BudgetScaffoldCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTransactionClick(transaction.id) },
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
                            CategoryIconBubble(
                                categoryName = transaction.categoryName ?: transaction.title,
                                modifier = Modifier.size(40.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        transaction.title,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    if (transaction.splitCount > 1) {
                                        Text(
                                            "Split ${transaction.splitCount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                Text(
                                    buildString {
                                        append(transaction.paidDate.format(dateFormatter))
                                        transaction.categoryName?.let { append(" · $it") }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                transaction.note?.let { note ->
                                    Text(
                                        note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Text(
                            formatCurrency(transaction.amountMinor, currencyCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
