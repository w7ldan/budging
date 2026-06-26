package com.budging.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budging.app.data.local.entity.TransactionEntity
import com.budging.app.data.local.query.TransactionHistoryRow
import com.budging.app.data.local.query.TransactionImpactRow
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

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
            t.category_id AS categoryId,
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
            t.category_id AS categoryId,
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

    @Query(
        """
        SELECT
            t.id, t.title, t.note, t.amount_minor AS amountMinor,
            t.paid_date_epoch AS paidDate, t.paid_at_epoch_millis AS paidAtEpochMillis,
            t.category_id AS categoryId, t.split_count AS splitCount,
            c.name AS categoryName
        FROM transactions t
        LEFT JOIN budget_categories c ON c.id = t.category_id
        ORDER BY t.paid_at_epoch_millis DESC, t.id DESC
        """,
    )
    fun observeAll(): Flow<List<TransactionHistoryRow>>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getById(transactionId: Long): TransactionEntity?

    @Query(
        """
        SELECT id FROM transactions
        WHERE recurring_template_id = :templateId
          AND source_occurrence_date_epoch = :occurrenceDate
        LIMIT 1
        """,
    )
    suspend fun findRecurringTransactionId(templateId: Long, occurrenceDate: LocalDate): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: Long)

    @Query("SELECT * FROM transactions ORDER BY id")
    suspend fun getAll(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)
}
