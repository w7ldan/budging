package com.budging.app.data.model

data class CategoryDetailState(
    val categoryId: Long,
    val currencyCode: String,
    val categoryName: String,
    val allocatedAmountMinor: Long,
    val spentAmountMinor: Long,
    val remainingAmountMinor: Long,
    val transactions: List<RecentTransaction>,
)
