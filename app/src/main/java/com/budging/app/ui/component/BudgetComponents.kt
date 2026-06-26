package com.budging.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.budging.app.ui.Screen
import com.budging.app.ui.theme.AppInkBlue
import com.budging.app.ui.theme.AppPrimary
import com.budging.app.ui.theme.AppPrimarySoft
import com.budging.app.ui.theme.AppSecondary
import com.budging.app.ui.theme.AppSecondarySoft
import com.budging.app.ui.theme.AppSuccess
import com.budging.app.ui.theme.AppSurfaceLow
import com.budging.app.ui.theme.AppWarm
import com.budging.app.ui.theme.AppWarmSoft
import com.budging.app.ui.theme.BudgingTheme

data class CategoryAccent(
    val icon: ImageVector,
    val tint: Color,
    val background: Color,
)

@Composable
fun BudgetScaffoldCard(
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(BudgingTheme.spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.material3.Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            contentColor = if (dark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (dark) 2.dp else 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(BudgingTheme.spacing.md),
            content = content,
        )
    }
}

@Composable
fun SectionHeader(
    eyebrow: String? = null,
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BudgingTheme.spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                eyebrow?.let {
                    Text(
                        it.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            if (action != null && onAction != null) {
                Text(
                    action,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable { onAction() }.padding(8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun BudgetMetricRow(
    label: String,
    value: String,
    strong: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun BudgetProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

@Composable
fun CategoryIconBubble(
    categoryName: String,
    modifier: Modifier = Modifier,
    accent: CategoryAccent = categoryAccent(categoryName),
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(accent.background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = accent.icon,
            contentDescription = null,
            tint = accent.tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun BottomNavItemPill(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = when (screen) {
        Screen.Dashboard -> AppSecondarySoft
        Screen.BudgetSetup -> MaterialTheme.colorScheme.surfaceVariant
        Screen.LogExpense -> AppWarmSoft
        Screen.Settings -> AppPrimarySoft
        Screen.CategoryDetail -> AppPrimarySoft
        Screen.TransactionHistory -> AppPrimarySoft
        Screen.TransactionDetail -> AppPrimarySoft
        Screen.EditTransaction -> AppPrimarySoft
        Screen.BudgetPeriodList -> AppPrimarySoft
        Screen.CreateNextPeriod -> AppPrimarySoft
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = screenIcon(screen),
            contentDescription = screen.label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            screen.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun BudgetTopBar(
    title: String,
    showBack: Boolean,
    onBack: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = BudgingTheme.spacing.xl, vertical = BudgingTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (showBack && onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.padding(6.dp).size(28.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column {
                Text("Budging", style = MaterialTheme.typography.headlineMedium)
                if (showBack) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        IconButton(
            onClick = {},
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Icon(Icons.Default.NotificationsNone, contentDescription = "Alerts")
        }
    }
}

@Composable
fun BudgetChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            selectedContainerColor = AppSecondarySoft,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            selectedBorderColor = Color.Transparent,
        ),
    )
}

fun categoryAccent(name: String): CategoryAccent {
    val key = name.lowercase()
    return when {
        "food" in key || "restaurant" in key -> CategoryAccent(Icons.Default.Restaurant, AppPrimary, AppSecondarySoft)
        "transport" in key || "travel" in key -> CategoryAccent(Icons.Default.DirectionsBus, AppWarm, AppWarmSoft.copy(alpha = 0.6f))
        "gym" in key || "fitness" in key -> CategoryAccent(Icons.Default.FitnessCenter, AppSuccess, AppSuccess.copy(alpha = 0.15f))
        "fun" in key || "entertain" in key -> CategoryAccent(Icons.Default.Celebration, AppWarm, AppWarmSoft)
        "shopping" in key -> CategoryAccent(Icons.Default.ShoppingBag, AppInkBlue, AppPrimarySoft)
        "coffee" in key -> CategoryAccent(Icons.Default.LocalCafe, AppWarm, AppWarmSoft)
        "utility" in key -> CategoryAccent(Icons.Default.Bolt, AppInkBlue, AppPrimarySoft.copy(alpha = 0.6f))
        else -> CategoryAccent(Icons.Default.Payments, AppSecondary, AppSurfaceLow)
    }
}

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

private fun screenIcon(screen: Screen): ImageVector = when (screen) {
    Screen.Dashboard -> Icons.Default.Dashboard
    Screen.BudgetSetup -> Icons.Default.SettingsSuggest
    Screen.LogExpense -> Icons.Default.Add
    Screen.Settings -> Icons.Default.EditNote
    Screen.CategoryDetail -> Icons.Default.EditNote
    Screen.TransactionHistory -> Icons.Default.EditNote
    Screen.TransactionDetail -> Icons.Default.EditNote
    Screen.EditTransaction -> Icons.Default.EditNote
    Screen.BudgetPeriodList -> Icons.Default.SettingsSuggest
    Screen.CreateNextPeriod -> Icons.Default.SettingsSuggest
}
