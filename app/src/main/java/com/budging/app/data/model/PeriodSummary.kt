package com.budging.app.data.model

data class PeriodSummary(
    val id: Long,
    val name: String,
    val dateRangeLabel: String,
    val totalAmountMinor: Long,
    val spentAmountMinor: Long,
    val remainingAmountMinor: Long,
    val currencyCode: String,
    val isActive: Boolean,
    val categoryCount: Int,
)

data class PendingImpactDetail(
    val impactId: Long,
    val transactionId: Long,
    val transactionTitle: String,
    val amountMinor: Long,
    val categoryNameSnapshot: String,
    val plannedPeriodOffset: Int,
    val sourcePeriodName: String?,
    val matchingCategoryId: Long?,
    val matchingCategoryName: String?,
    val matchStatus: PendingMatchStatus,
)

enum class PendingMatchStatus {
    MATCHED,
    NO_MATCH,
    AMBIGUOUS,
}
