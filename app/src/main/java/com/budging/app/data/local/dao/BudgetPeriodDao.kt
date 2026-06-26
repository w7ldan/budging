package com.budging.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budging.app.data.local.entity.BudgetPeriodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetPeriodDao {
    @Query(
        """
        SELECT * FROM budget_periods
        WHERE start_date_epoch <= :referenceEpochDay
          AND end_date_epoch >= :referenceEpochDay
        ORDER BY start_date_epoch DESC
        LIMIT 1
        """,
    )
    fun observeActive(referenceEpochDay: Long): Flow<BudgetPeriodEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(period: BudgetPeriodEntity): Long
}
