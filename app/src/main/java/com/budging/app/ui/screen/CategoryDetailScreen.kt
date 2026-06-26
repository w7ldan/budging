package com.budging.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budging.app.data.model.CategoryDetailState
import com.budging.app.ui.format.formatIdr

@Composable
fun CategoryDetailScreen(state: CategoryDetailState?) {
    if (state == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Category Detail", style = MaterialTheme.typography.headlineMedium)
            Text("Open a category from the dashboard to load its spending history.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(state.categoryName, style = MaterialTheme.typography.headlineMedium)
            Text("Allocated: ${formatIdr(state.allocatedAmountMinor)}")
            Text("Spent: ${formatIdr(state.spentAmountMinor)}")
            Text("Remaining: ${formatIdr(state.remainingAmountMinor)}")
        }
        items(state.transactions) { transaction ->
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(transaction.title, style = MaterialTheme.typography.titleMedium)
                    Text(formatIdr(transaction.amountMinor))
                    Text(transaction.paidDateLabel)
                }
            }
        }
    }
}
