package com.budging.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "budget_impacts",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BudgetPeriodEntity::class,
            parentColumns = ["id"],
            childColumns = ["budget_period_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = BudgetCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("transaction_id"), Index("budget_period_id"), Index("category_id")],
)
data class BudgetImpactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "budget_period_id") val budgetPeriodId: Long?,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "source_budget_period_id") val sourceBudgetPeriodId: Long? = null,
    @ColumnInfo(name = "category_name_snapshot") val categoryNameSnapshot: String = "",
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "impact_date_epoch") val impactDate: LocalDate,
    @ColumnInfo(name = "planned_period_offset") val plannedPeriodOffset: Int = 0,
    @ColumnInfo(name = "pending_period_start_epoch") val pendingPeriodStartDate: LocalDate? = null,
    val status: String,
)
