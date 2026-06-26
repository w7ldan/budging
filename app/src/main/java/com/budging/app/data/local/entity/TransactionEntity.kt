package com.budging.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "transactions",
    indices = [Index("category_id"), Index("recurring_template_id"), Index(value = ["recurring_template_id", "source_occurrence_date_epoch"])],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String? = null,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "paid_date_epoch") val paidDate: LocalDate,
    @ColumnInfo(name = "paid_at_epoch_millis") val paidAtEpochMillis: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "split_count") val splitCount: Int = 1,
    @ColumnInfo(name = "source_type") val sourceType: String = "MANUAL",
    @ColumnInfo(name = "recurring_template_id") val recurringTemplateId: Long? = null,
    @ColumnInfo(name = "source_occurrence_date_epoch") val sourceOccurrenceDate: LocalDate? = null,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
)
