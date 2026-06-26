package com.budging.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budging.app.ui.theme.BudgingTheme

@Composable
fun Keypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
) {
    val spacing = BudgingTheme.spacing
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("000", "0", "Del"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                row.forEach { key ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clickable {
                                when (key) {
                                    "Del" -> onDelete()
                                    else -> onDigit(key)
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(key, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        Text(
            "Clear",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onClear)
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
