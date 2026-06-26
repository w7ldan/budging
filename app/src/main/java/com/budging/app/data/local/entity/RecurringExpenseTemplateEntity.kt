package com.budging.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "recurring_expense_templates")
data class RecurringExpenseTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "category_name_snapshot") val categoryNameSnapshot: String,
    @ColumnInfo(name = "icon_key") val iconKey: String? = null,
    val note: String? = null,
    val frequency: String,
    @ColumnInfo(name = "start_date_epoch") val startDate: LocalDate,
    @ColumnInfo(name = "end_date_epoch") val endDate: LocalDate? = null,
    @ColumnInfo(name = "day_of_month") val dayOfMonth: Int? = null,
    @ColumnInfo(name = "apply_mode") val applyMode: String = "CONFIRM",
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
)
