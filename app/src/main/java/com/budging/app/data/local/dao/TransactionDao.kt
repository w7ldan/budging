package com.budging.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budging.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT DISTINCT t.*
        FROM transactions t
        INNER JOIN budget_impacts bi ON bi.transaction_id = t.id
        WHERE bi.budget_period_id = :budgetPeriodId
        ORDER BY t.paid_at_epoch_millis DESC, t.id DESC
        LIMIT :limit
        """,
    )
    fun observeRecentForPeriod(budgetPeriodId: Long, limit: Int): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT DISTINCT t.*
        FROM transactions t
        INNER JOIN budget_impacts bi ON bi.transaction_id = t.id
        WHERE bi.category_id = :categoryId
        ORDER BY t.paid_at_epoch_millis DESC, t.id DESC
        """,
    )
    fun observeForCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: Long)
}
