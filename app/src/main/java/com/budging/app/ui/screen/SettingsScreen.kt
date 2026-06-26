package com.budging.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.theme.BudgingTheme

@Composable
fun SettingsScreen() {
    val spacing = BudgingTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        SectionHeader(eyebrow = "Later", title = "Settings")
        BudgetScaffoldCard {
            Text("Local-first by design", style = MaterialTheme.typography.titleLarge)
            Text(
                "Backup/export/import and small preferences can live here later. This phase only polishes the UI shell.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
