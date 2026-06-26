package com.budging.app.data.model

data class DashboardState(
    val periodName: String,
    val totalBudgetMinor: Long,
    val totalSpentMinor: Long,
    val totalRemainingMinor: Long,
    val daysRemainingInclusive: Long,
    val safeDailyMinor: Long,
    val categories: List<DashboardCategory>,
    val recentTransactions: List<RecentTransaction>,
) {
    companion object {
        val Empty = DashboardState(
            periodName = "No active budget",
            totalBudgetMinor = 0,
            totalSpentMinor = 0,
            totalRemainingMinor = 0,
            daysRemainingInclusive = 0,
            safeDailyMinor = 0,
            categories = emptyList(),
            recentTransactions = emptyList(),
        )
    }
}

data class DashboardCategory(
    val id: Long,
    val name: String,
    val allocatedAmountMinor: Long,
    val spentAmountMinor: Long,
    val remainingAmountMinor: Long,
)

data class RecentTransaction(
    val id: Long,
    val title: String,
    val amountMinor: Long,
    val paidDateLabel: String,
)
