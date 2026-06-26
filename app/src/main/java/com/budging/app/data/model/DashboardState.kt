package com.budging.app.data.model

data class DashboardState(
    val periodId: Long? = null,
    val periodName: String,
    val periodDateRangeLabel: String,
    val currencyCode: String,
    val totalBudgetMinor: Long,
    val totalSpentMinor: Long,
    val totalRemainingMinor: Long,
    val daysRemainingInclusive: Long,
    val safeDailyMinor: Long,
    val unallocatedAmountMinor: Long,
    val categories: List<DashboardCategory>,
    val recentTransactions: List<RecentTransaction>,
    val hasActiveBudget: Boolean,
) {
    companion object {
        val Empty = DashboardState(
            periodName = "No active budget",
            periodDateRangeLabel = "",
            currencyCode = "IDR",
            totalBudgetMinor = 0,
            totalSpentMinor = 0,
            totalRemainingMinor = 0,
            daysRemainingInclusive = 0,
            safeDailyMinor = 0,
            unallocatedAmountMinor = 0,
            categories = emptyList(),
            recentTransactions = emptyList(),
            hasActiveBudget = false,
        )
    }
}

data class DashboardCategory(
    val id: Long,
    val name: String,
    val allocatedAmountMinor: Long,
    val spentAmountMinor: Long,
    val remainingAmountMinor: Long,
    val progressPercent: Int,
    val isArchived: Boolean,
)

data class RecentTransaction(
    val id: Long,
    val title: String,
    val paidAmountMinor: Long,
    val impactAmountMinor: Long,
    val splitCount: Int,
    val paidDateLabel: String,
    val note: String?,
)
