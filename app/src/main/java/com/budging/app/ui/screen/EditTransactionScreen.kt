package com.budging.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.ExpenseCategoryOption
import com.budging.app.data.model.TransactionDetailState
import com.budging.app.ui.component.BudgetChip
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.DateInputRow
import com.budging.app.ui.component.Keypad
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.component.categoryIcon
import com.budging.app.ui.format.formatCurrency
import com.budging.app.ui.theme.BudgingTheme

@Composable
fun EditTransactionScreen(
    state: TransactionDetailState?,
    categories: List<ExpenseCategoryOption>,
    onSaveNormal: (amountMinor: Long, categoryId: Long, note: String, dateText: String) -> Unit,
    onSaveNote: (note: String, dateText: String) -> Unit,
    onBack: () -> Unit,
) {
    val spacing = BudgingTheme.spacing
    if (state == null) return

    var amountText by rememberSaveable { mutableStateOf(state.amountMinor.toString()) }
    var selectedCategoryId by rememberSaveable(state.transactionId.toString()) {
        mutableStateOf(state.categoryId ?: categories.firstOrNull()?.id)
    }
    var dateText by rememberSaveable { mutableStateOf(state.paidDateIso) }
    var noteText by rememberSaveable { mutableStateOf(state.note ?: "") }

    val amountMinor = amountText.toLongOrNull() ?: 0L
    LazyColumn(
        modifier = Modifier.padding(horizontal = spacing.xl),
        contentPadding = PaddingValues(bottom = spacing.xxl + 88.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item { SectionHeader(title = if (state.isSplit) "Edit Note & Date" else "Edit Expense") }

        if (state.isSplit) {
            item {
                BudgetScaffoldCard {
                    Text(
                        "Editing split amount/category is not supported yet. Delete and recreate this split expense.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        "You can edit the note and date below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (!state.isSplit) {
            // Amount display
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    Text("Amount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCurrency(amountMinor, state.currencyCode),
                        style = MaterialTheme.typography.displayLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }

            // Category chips
            if (categories.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        items(categories) { category ->
                            BudgetChip(
                                selected = selectedCategoryId == category.id,
                                label = category.name,
                                icon = categoryIcon(category.iconKey),
                                onClick = { selectedCategoryId = category.id },
                            )
                        }
                    }
                }
            }
        }

        // Date and Note inputs
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                DateInputRow("Date", dateText, Modifier.weight(1f)) { dateText = it }
                CompactInputCard(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Outlined.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    label = "Note",
                    value = noteText,
                    hint = "Optional",
                    onValueChange = { noteText = it },
                )
            }
        }

        // Keypad (only for normal expenses)
        if (!state.isSplit) {
            item {
                Keypad(
                    onDigit = { digit -> amountText += digit },
                    onDelete = { amountText = if (amountText.isNotEmpty()) amountText.dropLast(1) else "" },
                    onClear = { amountText = "" },
                )
            }
        }

        // Save button
        item {
            Button(
                onClick = {
                    if (state.isSplit) {
                        onSaveNote(noteText, dateText)
                    } else {
                        onSaveNormal(amountMinor, selectedCategoryId ?: 0L, noteText, dateText)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(68.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Save Changes", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun CompactInputCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { inner ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (value.isBlank()) {
                                Text(hint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                            }
                            inner()
                        }
                    },
                )
            }
        }
    }
}
