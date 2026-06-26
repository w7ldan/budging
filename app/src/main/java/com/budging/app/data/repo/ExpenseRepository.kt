package com.budging.app.data.repo

import android.content.Context
import androidx.room.withTransaction
import com.budging.app.data.local.BudgingDatabase
import com.budging.app.data.local.dao.BudgetCategoryDao
import com.budging.app.data.local.dao.BudgetImpactDao
import com.budging.app.data.local.dao.BudgetPeriodDao
import com.budging.app.data.local.dao.TransactionDao
import com.budging.app.data.local.entity.BudgetCategoryEntity
import com.budging.app.data.local.entity.BudgetImpactEntity
import com.budging.app.data.local.entity.TransactionEntity
import com.budging.app.domain.AppClock
import com.budging.app.domain.BudgetImpactStatus
import com.budging.app.domain.SplitExpensePlanner
import com.budging.app.domain.TransactionSourceType
import java.time.Instant
import java.time.LocalDate

class ExpenseRepository(
    private val appContext: Context,
    private val database: BudgingDatabase,
    private val budgetPeriodDao: BudgetPeriodDao,
    private val budgetCategoryDao: BudgetCategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetImpactDao: BudgetImpactDao,
    private val clock: AppClock,
) {
    suspend fun logNormalExpense(
        amountMinor: Long,
        categoryId: Long,
        note: String,
        paidAtEpochMillis: Long,
    ) {
        require(amountMinor > 0) { "Expense amount must be positive." }

        val paidDate = Instant.ofEpochMilli(paidAtEpochMillis).atZone(clock.zoneId()).toLocalDate()
        val activePeriod = budgetPeriodDao.getActive()
            ?: throw IllegalArgumentException("No active budget period.")
        require(!paidDate.isBefore(activePeriod.startDate) && !paidDate.isAfter(activePeriod.endDate)) {
            "Expense date must be inside the active budget period (${activePeriod.startDate} to ${activePeriod.endDate})."
        }
        val category = budgetCategoryDao.getById(categoryId)
            ?: throw IllegalArgumentException("Choose a valid category.")
        require(!category.isArchived) { "Archived categories cannot receive new expenses." }

        val title = note.trim().ifBlank { category.name }
        insertAppliedTransaction(
            title = title,
            note = note,
            amountMinor = amountMinor,
            paidDate = paidDate,
            paidAtEpochMillis = paidAtEpochMillis,
            category = category,
            budgetPeriodId = activePeriod.id,
            sourceType = TransactionSourceType.MANUAL.dbValue,
            recurringTemplateId = null,
            sourceOccurrenceDate = null,
        )
    }

    suspend fun logSplitExpense(
        amountMinor: Long,
        categoryId: Long,
        note: String,
        paidAtEpochMillis: Long,
        periodCount: Int,
    ) {
        require(periodCount in SplitExpensePlanner.MIN_SPLIT_PERIODS..SplitExpensePlanner.MAX_SPLIT_PERIODS) {
            "Split period count must be between ${SplitExpensePlanner.MIN_SPLIT_PERIODS} and ${SplitExpensePlanner.MAX_SPLIT_PERIODS}."
        }
        if (periodCount == 1) {
            logNormalExpense(amountMinor, categoryId, note, paidAtEpochMillis)
            return
        }

        val paidDate = Instant.ofEpochMilli(paidAtEpochMillis).atZone(clock.zoneId()).toLocalDate()
        val activePeriod = budgetPeriodDao.getActive()
            ?: throw IllegalArgumentException("No active budget period.")
        require(!paidDate.isBefore(activePeriod.startDate) && !paidDate.isAfter(activePeriod.endDate)) {
            "Expense date must be inside the active budget period (${activePeriod.startDate} to ${activePeriod.endDate})."
        }
        val category = budgetCategoryDao.getById(categoryId)
            ?: throw IllegalArgumentException("Choose a valid category.")
        require(!category.isArchived) { "Archived categories cannot receive new expenses." }

        val splitAmounts = SplitExpensePlanner.splitAmounts(amountMinor, periodCount)
        require(splitAmounts.sum() == amountMinor) { "Split impacts must equal the original payment." }

        val title = note.trim().ifBlank { category.name }
        val now = clock.nowMillis()
        database.withTransaction {
            val transactionId = transactionDao.upsert(
                TransactionEntity(
                    title = title,
                    note = note.trim().ifBlank { null },
                    amountMinor = amountMinor,
                    paidDate = paidDate,
                    paidAtEpochMillis = paidAtEpochMillis,
                    categoryId = categoryId,
                    splitCount = periodCount,
                    sourceType = TransactionSourceType.MANUAL.dbValue,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
            val impacts = splitAmounts.mapIndexed { index, impactAmount ->
                if (index == 0) {
                    BudgetImpactEntity(
                        transactionId = transactionId,
                        budgetPeriodId = activePeriod.id,
                        categoryId = categoryId,
                        sourceBudgetPeriodId = activePeriod.id,
                        categoryNameSnapshot = category.name,
                        amountMinor = impactAmount,
                        impactDate = paidDate,
                        plannedPeriodOffset = 0,
                        status = BudgetImpactStatus.APPLIED.dbValue,
                    )
                } else {
                    BudgetImpactEntity(
                        transactionId = transactionId,
                        budgetPeriodId = null,
                        categoryId = null,
                        sourceBudgetPeriodId = activePeriod.id,
                        categoryNameSnapshot = category.name,
                        amountMinor = impactAmount,
                        impactDate = paidDate,
                        plannedPeriodOffset = index,
                        pendingPeriodStartDate = activePeriod.endDate.plusDays(index.toLong()),
                        status = BudgetImpactStatus.PENDING.dbValue,
                    )
                }
            }
            budgetImpactDao.insertAll(impacts)
        }
        refreshQuickAccess(appContext)
    }

    internal suspend fun insertAppliedTransaction(
        title: String,
        note: String,
        amountMinor: Long,
        paidDate: LocalDate,
        paidAtEpochMillis: Long,
        category: BudgetCategoryEntity,
        budgetPeriodId: Long,
        sourceType: String,
        recurringTemplateId: Long?,
        sourceOccurrenceDate: LocalDate?,
    ) {
        val now = clock.nowMillis()
        database.withTransaction {
            val transactionId = transactionDao.upsert(
                TransactionEntity(
                    title = title,
                    note = note.trim().ifBlank { null },
                    amountMinor = amountMinor,
                    paidDate = paidDate,
                    paidAtEpochMillis = paidAtEpochMillis,
                    categoryId = category.id,
                    splitCount = 1,
                    sourceType = sourceType,
                    recurringTemplateId = recurringTemplateId,
                    sourceOccurrenceDate = sourceOccurrenceDate,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
            budgetImpactDao.insert(
                BudgetImpactEntity(
                    transactionId = transactionId,
                    budgetPeriodId = budgetPeriodId,
                    categoryId = category.id,
                    sourceBudgetPeriodId = budgetPeriodId,
                    categoryNameSnapshot = category.name,
                    amountMinor = amountMinor,
                    impactDate = paidDate,
                    plannedPeriodOffset = 0,
                    status = BudgetImpactStatus.APPLIED.dbValue,
                ),
            )
        }
        refreshQuickAccess(appContext)
    }
}
