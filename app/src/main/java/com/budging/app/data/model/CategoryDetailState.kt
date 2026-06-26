package com.budging.app.data.model

data class CategoryDetailState(
    val categoryName: String,
    val allocatedAmountMinor: Long,
    val spentAmountMinor: Long,
    val remainingAmountMinor: Long,
    val transactions: List<RecentTransaction>,
)
