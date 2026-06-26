package com.budging.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.TransactionDetailState
import com.budging.app.ui.component.BudgetMetricRow
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.CategoryIconBubble
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.format.formatCurrency
import com.budging.app.ui.theme.BudgingTheme

@Composable
fun TransactionDetailScreen(
    state: TransactionDetailState?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val spacing = BudgingTheme.spacing
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (state == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text("Transaction Detail", style = MaterialTheme.typography.headlineMedium)
            Text("Select a transaction to view its details.")
        }
        return
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = {
                Text(
                    if (state.isSplit) {
                        "This will permanently delete this split expense and all ${state.splitCount} budget impacts (applied and pending). This cannot be undone."
                    } else {
                        "This will permanently delete this expense and its budget impact. This cannot be undone."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        // Summary card
        BudgetScaffoldCard(dark = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    CategoryIconBubble(state.categoryName ?: state.title)
                    Column {
                        Text(state.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(state.paidDateLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
                    }
                }
            }
            Text(
                formatCurrency(state.amountMinor, state.currencyCode),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            state.note?.let { note ->
                BudgetMetricRow("Note", note)
            }
            state.categoryName?.let { cat ->
                BudgetMetricRow("Category", cat)
            }
            if (state.isSplit) {
                BudgetMetricRow("Split", "${state.splitCount} periods")
            }
        }

        // Impacts
        if (state.impacts.isNotEmpty()) {
            SectionHeader(title = "Budget Impacts")
            state.impacts.forEach { impact ->
                BudgetScaffoldCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(impact.categoryName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                buildString {
                                    append(impact.impactDateLabel)
                                    impact.periodName?.let { append(" · $it") }
                                    append(" · ${impact.status}")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            formatCurrency(impact.amountMinor, state.currencyCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Button(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("Edit")
            }
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.weight(1f),
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
