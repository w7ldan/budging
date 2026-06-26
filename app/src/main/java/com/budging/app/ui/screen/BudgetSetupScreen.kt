package com.budging.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.DashboardState

@Composable
fun BudgetSetupScreen(state: DashboardState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Budget Setup", style = MaterialTheme.typography.headlineMedium)
        Text("Current budget: ${state.periodName}")
        Text("This screen is reserved for create/edit budget period and category allocations.")
    }
}
