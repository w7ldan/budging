package com.budging.app.ui.component

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun DateInputRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    allowEmpty: Boolean = false,
    emptyLabel: String = "Select date",
    onValueChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val selectedDate = runCatching { LocalDate.parse(value) }.getOrNull() ?: LocalDate.now()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        onValueChange(LocalDate.of(year, month + 1, dayOfMonth).toString())
                    },
                    selectedDate.year,
                    selectedDate.monthValue - 1,
                    selectedDate.dayOfMonth,
                ).show()
            },
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
}
