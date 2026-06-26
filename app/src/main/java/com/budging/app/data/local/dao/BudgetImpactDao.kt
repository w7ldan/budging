package com.budging.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budging.app.data.local.entity.BudgetImpactEntity
import com.budging.app.data.local.query.CategorySpendingRow
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetImpactDao {
    @Query(
        """
        SELECT COALESCE(SUM(amount_minor), 0)
        FROM budget_impacts
        WHERE budget_period_id = :budgetPeriodId
          AND status = 'APPLIED'
        """,
    )
    fun observeSpentForPeriod(budgetPeriodId: Long): Flow<Long>

    @Query(
        """
        SELECT c.id AS categoryId, COALESCE(SUM(bi.amount_minor), 0) AS spentMinor
        FROM budget_categories c
        LEFT JOIN budget_impacts bi
            ON bi.category_id = c.id
           AND bi.budget_period_id = :budgetPeriodId
           AND bi.status = 'APPLIED'
        WHERE c.budget_period_id = :budgetPeriodId
        GROUP BY c.id
        ORDER BY c.display_order, c.id
        """,
    )
    fun observeCategorySpending(budgetPeriodId: Long): Flow<List<CategorySpendingRow>>

    @Query(
        """
        SELECT COALESCE(SUM(amount_minor), 0)
        FROM budget_impacts
        WHERE category_id = :categoryId
          AND budget_period_id = :budgetPeriodId
          AND status = 'APPLIED'
        """,
    )
    fun observeSpentForCategory(categoryId: Long, budgetPeriodId: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM budget_impacts WHERE category_id = :categoryId")
    suspend fun countForCategory(categoryId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(impact: BudgetImpactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(impacts: List<BudgetImpactEntity>)

    @Query(
        """
        SELECT * FROM budget_impacts
        WHERE status = 'PENDING'
        ORDER BY source_budget_period_id, planned_period_offset, id
        """,
    )
    suspend fun getPendingImpacts(): List<BudgetImpactEntity>

    @Query(
        """
        UPDATE budget_impacts
        SET budget_period_id = :budgetPeriodId,
            category_id = :categoryId,
            status = 'APPLIED'
        WHERE id = :impactId
        """,
    )
    suspend fun applyPendingImpact(impactId: Long, budgetPeriodId: Long, categoryId: Long)

    @Query("SELECT COUNT(*) FROM budget_impacts WHERE status = 'PENDING'")
    suspend fun countPendingImpacts(): Int
}
