package com.budging.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.RecurringTemplateItem
import com.budging.app.domain.RECURRING_FREQUENCY_EVERY_BUDGET_PERIOD
import com.budging.app.domain.RECURRING_FREQUENCY_MONTHLY
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.BudgetChip
import com.budging.app.ui.component.CategoryIconBubble
import com.budging.app.ui.component.DateInputRow
import com.budging.app.ui.component.IconDropdownField
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.component.categoryIcon
import com.budging.app.ui.format.formatCurrency
import com.budging.app.ui.theme.BudgingTheme

@Composable
fun SubscriptionsScreen(
    templates: List<RecurringTemplateItem>,
    defaultCurrencyCode: String,
    onSaveTemplate: (
        templateId: Long?,
        title: String,
        amountMinor: Long,
        currencyCode: String,
        categoryNameSnapshot: String,
        iconKey: String?,
        note: String,
        frequency: String,
        startDateText: String,
        endDateText: String,
        dayOfMonth: Int?,
        isActive: Boolean,
    ) -> Unit,
    onDeleteTemplate: (Long) -> Unit,
) {
    val spacing = BudgingTheme.spacing
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var title by rememberSaveable(editingId) { mutableStateOf("") }
    var amountText by rememberSaveable(editingId) { mutableStateOf("") }
    var currencyCode by rememberSaveable(editingId) { mutableStateOf(defaultCurrencyCode) }
    var categoryName by rememberSaveable(editingId) { mutableStateOf("") }
    var iconKey by rememberSaveable(editingId) { mutableStateOf("other") }
    var note by rememberSaveable(editingId) { mutableStateOf("") }
    var frequency by rememberSaveable(editingId) { mutableStateOf(RECURRING_FREQUENCY_MONTHLY) }
    var startDateText by rememberSaveable(editingId) { mutableStateOf(java.time.LocalDate.now().toString()) }
    var endDateText by rememberSaveable(editingId) { mutableStateOf("") }
    var dayOfMonthText by rememberSaveable(editingId) { mutableStateOf("15") }
    var isActive by rememberSaveable(editingId) { mutableStateOf(true) }

    fun load(template: RecurringTemplateItem?) {
        editingId = template?.id
        title = template?.title.orEmpty()
        amountText = template?.amountMinor?.toString().orEmpty()
        currencyCode = template?.currencyCode ?: defaultCurrencyCode
        categoryName = template?.categoryNameSnapshot.orEmpty()
        iconKey = template?.iconKey ?: "other"
        note = template?.note.orEmpty()
        frequency = template?.frequency ?: RECURRING_FREQUENCY_MONTHLY
        startDateText = template?.startDate?.toString() ?: java.time.LocalDate.now().toString()
        endDateText = template?.endDate?.toString().orEmpty()
        dayOfMonthText = template?.dayOfMonth?.toString() ?: "15"
        isActive = template?.isActive ?: true
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = spacing.xl),
        contentPadding = PaddingValues(bottom = spacing.xxl + 88.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item { SectionHeader(title = "Subscriptions") }
        item {
            BudgetScaffoldCard {
                MinimalInputRow("Name", title, modifier = Modifier.fillMaxWidth()) { title = it }
                MinimalInputRow("Amount", amountText, modifier = Modifier.fillMaxWidth(), keyboardType = KeyboardType.Number) {
                    amountText = it.filter(Char::isDigit)
                }
                MinimalInputRow("Category", categoryName, modifier = Modifier.fillMaxWidth()) { categoryName = it }
                IconDropdownField(
                    label = "Icon",
                    selectedIconKey = iconKey,
                    modifier = Modifier.fillMaxWidth(),
                    onSelect = { iconKey = it },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    BudgetChip(
                        selected = frequency == RECURRING_FREQUENCY_EVERY_BUDGET_PERIOD,
                        label = "Every budget period",
                        icon = categoryIcon("other"),
                        onClick = { frequency = RECURRING_FREQUENCY_EVERY_BUDGET_PERIOD },
                    )
                    BudgetChip(
                        selected = frequency == RECURRING_FREQUENCY_MONTHLY,
                        label = "Monthly",
                        icon = categoryIcon("calendar"),
                        onClick = { frequency = RECURRING_FREQUENCY_MONTHLY },
                    )
                }
                if (frequency == RECURRING_FREQUENCY_MONTHLY) {
                    MinimalInputRow("Monthly Day", dayOfMonthText, modifier = Modifier.fillMaxWidth(), keyboardType = KeyboardType.Number) {
                        dayOfMonthText = it.filter(Char::isDigit).take(2)
                    }
                }
                DateInputRow("Start Date", startDateText, Modifier.fillMaxWidth()) { startDateText = it }
                DateInputRow("End Date", endDateText, Modifier.fillMaxWidth(), allowEmpty = true, emptyLabel = "No end date") { endDateText = it }
                if (endDateText.isNotBlank()) {
                    TextButton(onClick = { endDateText = "" }) { Text("Clear end date") }
                }
                MinimalInputRow("Note", note, modifier = Modifier.fillMaxWidth()) { note = it }
                MinimalInputRow("Currency", currencyCode, modifier = Modifier.fillMaxWidth()) { currencyCode = it.uppercase().take(3) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Active", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
                Button(
                    onClick = {
                        onSaveTemplate(
                            editingId,
                            title,
                            amountText.toLongOrNull() ?: 0L,
                            currencyCode.ifBlank { defaultCurrencyCode },
                            categoryName,
                            iconKey,
                            note,
                            frequency,
                            startDateText,
                            endDateText,
                            dayOfMonthText.toIntOrNull(),
                            isActive,
                        )
                        load(null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (editingId == null) "Add Subscription" else "Update Subscription")
                }
            }
        }
        if (templates.isEmpty()) {
            item {
                BudgetScaffoldCard {
                    Text("No subscriptions yet.", style = MaterialTheme.typography.titleMedium)
                    Text("Add monthly services or every-budget-period recurring spend here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(templates) { template ->
                BudgetScaffoldCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                            CategoryIconBubble(template.categoryNameSnapshot, iconKey = template.iconKey)
                            Column {
                                Text(template.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${template.categoryNameSnapshot} · ${if (template.frequency == RECURRING_FREQUENCY_MONTHLY) "Monthly" else "Every budget period"}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(formatCurrency(template.amountMinor, template.currencyCode))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        TextButton(onClick = { load(template) }) { Text("Edit") }
                        TextButton(onClick = {
                            onSaveTemplate(
                                template.id,
                                template.title,
                                template.amountMinor,
                                template.currencyCode,
                                template.categoryNameSnapshot,
                                template.iconKey,
                                template.note.orEmpty(),
                                template.frequency,
                                template.startDate.toString(),
                                template.endDate?.toString().orEmpty(),
                                template.dayOfMonth,
                                !template.isActive,
                            )
                        }) {
                            Text(if (template.isActive) "Pause" else "Resume")
                        }
                        TextButton(onClick = { onDeleteTemplate(template.id) }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalInputRow(
    label: String,
    value: String,
    hint: String = "",
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                decorationBox = { inner ->
                    if (value.isBlank() && hint.isNotBlank()) {
                        Text(hint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                    }
                    inner()
                },
            )
        }
    }
}
