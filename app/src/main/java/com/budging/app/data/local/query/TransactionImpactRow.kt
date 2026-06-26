package com.budging.app.data.local.query

import java.time.LocalDate

data class TransactionImpactRow(
    val transactionId: Long,
    val title: String,
    val note: String?,
    val paidAmountMinor: Long,
    val paidDate: LocalDate,
    val paidAtEpochMillis: Long,
    val splitCount: Int,
    val impactAmountMinor: Long,
)
