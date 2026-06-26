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

    @Query("SELECT * FROM budget_periods WHERE id = :periodId LIMIT 1")
    fun observeById(periodId: Long): Flow<BudgetPeriodEntity?>

    @Query(
        """
        SELECT * FROM budget_periods
        WHERE start_date_epoch <= :referenceEpochDay
          AND end_date_epoch >= :referenceEpochDay
        ORDER BY start_date_epoch DESC
        LIMIT 1
        """,
    )
    suspend fun getActive(referenceEpochDay: Long): BudgetPeriodEntity?

    @Query("SELECT * FROM budget_periods WHERE id = :periodId LIMIT 1")
    suspend fun getById(periodId: Long): BudgetPeriodEntity?

    @Query(
        """
        SELECT * FROM budget_periods
        WHERE start_date_epoch > :sourceStartEpochDay
        ORDER BY start_date_epoch ASC, id ASC
        """,
    )
    suspend fun getPeriodsAfter(sourceStartEpochDay: Long): List<BudgetPeriodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(period: BudgetPeriodEntity): Long
}
