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
        WHERE is_active = 1
        ORDER BY start_date_epoch DESC
        LIMIT 1
        """,
    )
    fun observeActive(): Flow<BudgetPeriodEntity?>

    @Query("SELECT * FROM budget_periods WHERE id = :periodId LIMIT 1")
    fun observeById(periodId: Long): Flow<BudgetPeriodEntity?>

    @Query(
        """
        SELECT * FROM budget_periods
        WHERE is_active = 1
        ORDER BY start_date_epoch DESC
        LIMIT 1
        """,
    )
    suspend fun getActive(): BudgetPeriodEntity?

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

    @Query("SELECT * FROM budget_periods ORDER BY start_date_epoch DESC, id DESC")
    fun observeAll(): Flow<List<BudgetPeriodEntity>>

    @Query("SELECT * FROM budget_periods ORDER BY id")
    suspend fun getAll(): List<BudgetPeriodEntity>

    @Query("UPDATE budget_periods SET is_active = :isActive WHERE id = :periodId")
    suspend fun setActive(periodId: Long, isActive: Boolean)

    @Query("DELETE FROM budget_periods WHERE id = :periodId")
    suspend fun deleteById(periodId: Long)

    @Query(
        """
        UPDATE budget_periods SET is_active = 0
        WHERE is_active = 1
          AND id != (
              SELECT id FROM budget_periods
              WHERE is_active = 1
              ORDER BY start_date_epoch DESC, updated_at_epoch_millis DESC
              LIMIT 1
          )
        """,
    )
    suspend fun enforceSingleActive()

    @Query("SELECT COUNT(*) FROM budget_periods WHERE is_active = 1")
    suspend fun countActive(): Int

    @Query("DELETE FROM budget_periods")
    suspend fun deleteAll()
}
