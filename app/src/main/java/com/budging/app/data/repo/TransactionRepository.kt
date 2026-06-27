package com.budging.app.data.repo

import android.content.Context
import androidx.room.withTransaction
import com.budging.app.data.local.BudgingDatabase
import com.budging.app.data.local.dao.BudgetCategoryDao
import com.budging.app.data.local.dao.BudgetImpactDao
import com.budging.app.data.local.dao.BudgetPeriodDao
import com.budging.app.data.local.dao.TransactionDao
import com.budging.app.data.local.query.TransactionHistoryRow
import com.budging.app.data.model.ImpactDetail
import com.budging.app.data.model.TransactionDetailState
import com.budging.app.domain.AppClock
import com.budging.app.domain.BudgetImpactStatus
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val appContext: Context,
    private val database: BudgingDatabase,
    private val transactionDao: TransactionDao,
    private val budgetImpactDao: BudgetImpactDao,
    private val budgetPeriodDao: BudgetPeriodDao,
    private val budgetCategoryDao: BudgetCategoryDao,
    private val clock: AppClock,
) {
    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd MMM")
    private val longDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    suspend fun deleteTransaction(transactionId: Long) {
        transactionDao.deleteById(transactionId)
        refreshQuickAccess(appContext)
    }

    fun observeAllTransactions(): Flow<List<TransactionHistoryRow>> = transactionDao.observeAll()

    suspend fun getTransactionDetail(transactionId: Long): TransactionDetailState? {
        val transaction = transactionDao.getById(transactionId) ?: return null
        val impacts = budgetImpactDao.getByTransactionId(transactionId)
        val period = impacts.firstOrNull { it.budgetPeriodId != null }?.budgetPeriodId?.let { budgetPeriodDao.getById(it) }
        val currencyCode = period?.currencyCode ?: "IDR"

        return TransactionDetailState(
            transactionId = transaction.id,
            title = transaction.title,
            note = transaction.note,
            amountMinor = transaction.amountMinor,
            paidDateLabel = transaction.paidDate.format(longDateFormatter),
            paidDateIso = transaction.paidDate.toString(),
            categoryId = transaction.categoryId,
            categoryName = impacts.firstOrNull()?.categoryNameSnapshot,
            splitCount = transaction.splitCount,
            currencyCode = currencyCode,
            impacts = impacts.map { impact ->
                val impactPeriod = impact.budgetPeriodId?.let { budgetPeriodDao.getById(it) }
                ImpactDetail(
                    impactId = impact.id,
                    amountMinor = impact.amountMinor,
                    categoryName = impact.categoryNameSnapshot,
                    periodName = impactPeriod?.name,
                    status = impact.status,
                    impactDateLabel = impact.impactDate.format(shortDateFormatter),
                )
            },
            isSplit = transaction.splitCount > 1,
        )
    }

    suspend fun editNormalExpense(
        transactionId: Long,
        amountMinor: Long,
        categoryId: Long,
        note: String,
        paidAtEpochMillis: Long,
    ) {
        require(amountMinor > 0) { "Expense amount must be positive." }
        val paidDate = Instant.ofEpochMilli(paidAtEpochMillis).atZone(clock.zoneId()).toLocalDate()
        val activePeriod = budgetPeriodDao.getActive()
            ?: throw IllegalArgumentException("No active budget period.")
        val category = budgetCategoryDao.getById(categoryId)
            ?: throw IllegalArgumentException("Choose a valid category.")
        require(!category.isArchived) { "Archived categories cannot receive new expenses." }

        val existing = transactionDao.getById(transactionId)
            ?: throw IllegalArgumentException("Transaction not found.")
        require(existing.splitCount <= 1) { "Editing split transaction amounts is not supported. Delete and recreate this split expense." }

        val now = clock.nowMillis()
        database.withTransaction {
            transactionDao.update(
                existing.copy(
                    title = category.name,
                    note = note.trim().ifBlank { null },
                    amountMinor = amountMinor,
                    paidDate = paidDate,
                    paidAtEpochMillis = paidAtEpochMillis,
                    categoryId = categoryId,
                    updatedAtEpochMillis = now,
                ),
            )
            budgetImpactDao.getByTransactionId(transactionId).firstOrNull { it.status == BudgetImpactStatus.APPLIED.dbValue }?.let { appliedImpact ->
                budgetImpactDao.update(
                    appliedImpact.copy(
                        amountMinor = amountMinor,
                        categoryId = categoryId,
                        budgetPeriodId = activePeriod.id,
                        categoryNameSnapshot = category.name,
                        impactDate = paidDate,
                    ),
                )
            }
        }
        refreshQuickAccess(appContext)
    }

    suspend fun editTransactionNote(
        transactionId: Long,
        note: String,
        paidAtEpochMillis: Long,
    ) {
        val existing = transactionDao.getById(transactionId)
            ?: throw IllegalArgumentException("Transaction not found.")
        val paidDate = Instant.ofEpochMilli(paidAtEpochMillis).atZone(clock.zoneId()).toLocalDate()
        val now = clock.nowMillis()
        val title = budgetImpactDao.getByTransactionId(transactionId).firstOrNull()?.categoryNameSnapshot ?: existing.title
        database.withTransaction {
            transactionDao.update(
                existing.copy(
                    title = title,
                    note = note.trim().ifBlank { null },
                    paidDate = paidDate,
                    paidAtEpochMillis = paidAtEpochMillis,
                    updatedAtEpochMillis = now,
                ),
            )
            budgetImpactDao.getByTransactionId(transactionId).forEach { impact ->
                budgetImpactDao.update(impact.copy(impactDate = paidDate))
            }
        }
        refreshQuickAccess(appContext)
    }

}
