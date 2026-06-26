package com.budging.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.budging.app.ui.Screen

@Composable
fun BottomNavItemPill(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val content = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = screenIcon(screen, selected),
            contentDescription = screen.label,
            modifier = Modifier.size(20.dp),
            tint = content,
        )
        Text(
            screen.label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}

private fun screenIcon(screen: Screen, selected: Boolean): ImageVector = when (screen) {
    Screen.Dashboard -> Icons.Filled.Dashboard
    Screen.BudgetSetup -> Icons.Filled.AccountBalanceWallet
    Screen.LogExpense -> Icons.Filled.AddCircle
    Screen.Settings -> Icons.Filled.Settings
    else -> Icons.Filled.Settings
}
