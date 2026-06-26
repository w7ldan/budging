package com.budging.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "budget_periods")
data class BudgetPeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "start_date_epoch") val startDate: LocalDate,
    @ColumnInfo(name = "end_date_epoch") val endDate: LocalDate,
    @ColumnInfo(name = "total_amount_minor") val totalAmountMinor: Long,
    @ColumnInfo(name = "currency_code") val currencyCode: String = "IDR",
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
)
