package com.budging.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budget_categories",
    foreignKeys = [
        ForeignKey(
            entity = BudgetPeriodEntity::class,
            parentColumns = ["id"],
            childColumns = ["budget_period_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("budget_period_id")],
)
data class BudgetCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "budget_period_id") val budgetPeriodId: Long,
    val name: String,
    @ColumnInfo(name = "allocated_amount_minor") val allocatedAmountMinor: Long,
    @ColumnInfo(name = "display_order") val displayOrder: Int = 0,
)
