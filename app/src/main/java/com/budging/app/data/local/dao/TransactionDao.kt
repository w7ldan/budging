package com.budging.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budging.app.data.local.entity.TransactionEntity
import com.budging.app.data.local.query.TransactionImpactRow
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT
            t.id AS transactionId,
            t.title AS title,
            t.note AS note,
            t.amount_minor AS paidAmountMinor,
            t.paid_date_epoch AS paidDate,
            t.paid_at_epoch_millis AS paidAtEpochMillis,
            t.split_count AS splitCount,
            bi.amount_minor AS impactAmountMinor
        FROM transactions t
        INNER JOIN budget_impacts bi ON bi.transaction_id = t.id
        WHERE bi.budget_period_id = :budgetPeriodId
          AND bi.status = 'APPLIED'
        ORDER BY t.paid_at_epoch_millis DESC, t.id DESC
        LIMIT :limit
        """,
    )
    fun observeRecentForPeriod(budgetPeriodId: Long, limit: Int): Flow<List<TransactionImpactRow>>

    @Query(
        """
        SELECT
            t.id AS transactionId,
            t.title AS title,
            t.note AS note,
            t.amount_minor AS paidAmountMinor,
            t.paid_date_epoch AS paidDate,
            t.paid_at_epoch_millis AS paidAtEpochMillis,
            t.split_count AS splitCount,
            bi.amount_minor AS impactAmountMinor
        FROM transactions t
        INNER JOIN budget_impacts bi ON bi.transaction_id = t.id
        WHERE bi.category_id = :categoryId
          AND bi.budget_period_id = :budgetPeriodId
          AND bi.status = 'APPLIED'
        ORDER BY t.paid_at_epoch_millis DESC, t.id DESC
        """,
    )
    fun observeForCategory(categoryId: Long, budgetPeriodId: Long): Flow<List<TransactionImpactRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: Long)
}
