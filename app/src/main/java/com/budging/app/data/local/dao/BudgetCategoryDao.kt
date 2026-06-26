package com.budging.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budging.app.data.local.entity.BudgetCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetCategoryDao {
    @Query(
        """
        SELECT * FROM budget_categories
        WHERE budget_period_id = :budgetPeriodId
          AND is_archived = 0
        ORDER BY display_order, id
        """,
    )
    fun observeForPeriod(budgetPeriodId: Long): Flow<List<BudgetCategoryEntity>>

    @Query(
        """
        SELECT * FROM budget_categories
        WHERE budget_period_id = :budgetPeriodId
        ORDER BY is_archived, display_order, id
        """,
    )
    fun observeAllForPeriod(budgetPeriodId: Long): Flow<List<BudgetCategoryEntity>>

    @Query("SELECT * FROM budget_categories WHERE id = :categoryId LIMIT 1")
    fun observeById(categoryId: Long): Flow<BudgetCategoryEntity?>

    @Query("SELECT * FROM budget_categories WHERE id = :categoryId LIMIT 1")
    suspend fun getById(categoryId: Long): BudgetCategoryEntity?

    @Query("SELECT COALESCE(MAX(display_order), -1) FROM budget_categories WHERE budget_period_id = :budgetPeriodId")
    suspend fun getMaxDisplayOrder(budgetPeriodId: Long): Int

    @Query("SELECT COALESCE(SUM(allocated_amount_minor), 0) FROM budget_categories WHERE budget_period_id = :budgetPeriodId AND is_archived = 0")
    suspend fun getAllocatedSumForActive(budgetPeriodId: Long): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: BudgetCategoryEntity): Long

    @Query("UPDATE budget_categories SET is_archived = :isArchived WHERE id = :categoryId")
    suspend fun setArchived(categoryId: Long, isArchived: Boolean)

    @Query("DELETE FROM budget_categories WHERE id = :categoryId")
    suspend fun deleteById(categoryId: Long)
}
