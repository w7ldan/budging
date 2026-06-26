package com.budging.app.data.local.query

import java.time.LocalDate

data class TransactionHistoryRow(
    val id: Long,
    val title: String,
    val note: String?,
    val amountMinor: Long,
    val paidDate: LocalDate,
    val paidAtEpochMillis: Long,
    val categoryId: Long?,
    val splitCount: Int,
    val categoryName: String?,
)
