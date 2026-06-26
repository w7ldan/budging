package com.budging.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.budging.app.domain.AppClock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

@Composable
fun DateInputRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    allowEmpty: Boolean = false,
    emptyLabel: String = "Select date",
    onValueChange: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val selectedDate = runCatching { LocalDate.parse(value) }.getOrNull()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (value.isBlank() && allowEmpty) emptyLabel else value,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (value.isBlank() && allowEmpty) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    if (showPicker) {
        BudgetDatePickerDialog(
            initialDate = selectedDate ?: AppClock.System.today(),
            allowClear = allowEmpty,
            onDismiss = { showPicker = false },
            onClear = {
                onValueChange("")
                showPicker = false
            },
            onConfirm = {
                onValueChange(it.toString())
                showPicker = false
            },
        )
    }
}

@Composable
private fun BudgetDatePickerDialog(
    initialDate: LocalDate,
    allowClear: Boolean,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    var visibleMonth by remember(initialDate) { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonthNavButton(
                        icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        onClick = { visibleMonth = visibleMonth.minusMonths(1) },
                    )
                    Text(
                        visibleMonth.format(monthFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    MonthNavButton(
                        icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        onClick = { visibleMonth = visibleMonth.plusMonths(1) },
                    )
                }

                CalendarWeekHeader()
                CalendarMonthGrid(
                    visibleMonth = visibleMonth,
                    selectedDate = selectedDate,
                    onSelect = { selectedDate = it },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (allowClear) {
                        TextButton(onClick = onClear) { Text("Clear") }
                    } else {
                        Box {}
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Button(onClick = { onConfirm(selectedDate) }, shape = RoundedCornerShape(14.dp)) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CalendarWeekHeader() {
    val labels = remember {
        orderedDaysOfWeek().map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2) }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEach { label ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    val monthDays = remember(visibleMonth) { buildMonthCells(visibleMonth) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        monthDays.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                week.forEach { date ->
                    val isSelected = date == selectedDate
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .size(38.dp)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape,
                            )
                            .clickable(enabled = date != null) {
                                if (date != null) onSelect(date)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = date?.dayOfMonth?.toString().orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

private fun buildMonthCells(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val leadingBlanks = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value).mod(7)
    val days = MutableList<LocalDate?>(leadingBlanks) { null } +
        (1..month.lengthOfMonth()).map(month::atDay)
    val trailingBlanks = (7 - (days.size % 7)).mod(7)
    return days + MutableList(trailingBlanks) { null }
}

private fun orderedDaysOfWeek(): List<DayOfWeek> = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY,
)
