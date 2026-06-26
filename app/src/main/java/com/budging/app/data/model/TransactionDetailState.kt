package com.budging.app.data.model

data class TransactionDetailState(
    val transactionId: Long,
    val title: String,
    val note: String?,
    val amountMinor: Long,
    val paidDateLabel: String,
    val paidDateIso: String,
    val categoryId: Long?,
    val categoryName: String?,
    val splitCount: Int,
    val currencyCode: String,
    val impacts: List<ImpactDetail>,
    val isSplit: Boolean,
) {
    val isNormal: Boolean get() = !isSplit
}

data class ImpactDetail(
    val impactId: Long,
    val amountMinor: Long,
    val categoryName: String,
    val periodName: String?,
    val status: String,
    val impactDateLabel: String,
)
