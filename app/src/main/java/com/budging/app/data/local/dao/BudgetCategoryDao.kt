package com.budging.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budging.app.data.local.entity.BudgetCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetCategoryDao {
    @Query("SELECT * FROM budget_categories WHERE budget_period_id = :budgetPeriodId ORDER BY display_order, id")
    fun observeForPeriod(budgetPeriodId: Long): Flow<List<BudgetCategoryEntity>>

    @Query("SELECT * FROM budget_categories WHERE id = :categoryId LIMIT 1")
    fun observeById(categoryId: Long): Flow<BudgetCategoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<BudgetCategoryEntity>)
}
