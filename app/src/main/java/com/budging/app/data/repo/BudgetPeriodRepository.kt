package com.budging.app.data.repo

import android.content.Context
import androidx.room.withTransaction
import com.budging.app.data.local.BudgingDatabase
import com.budging.app.data.local.dao.BudgetCategoryDao
import com.budging.app.data.local.dao.BudgetImpactDao
import com.budging.app.data.local.dao.BudgetPeriodDao
import com.budging.app.data.local.dao.TransactionDao
import com.budging.app.data.local.entity.BudgetCategoryEntity
import com.budging.app.data.local.entity.BudgetPeriodEntity
import com.budging.app.data.local.entity.TransactionEntity
import com.budging.app.data.model.PeriodSummary
import com.budging.app.domain.AppClock
import com.budging.app.domain.TransactionSourceType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetPeriodRepository(
    private val appContext: Context,
    private val database: BudgingDatabase,
    private val budgetPeriodDao: BudgetPeriodDao,
    private val budgetCategoryDao: BudgetCategoryDao,
    private val budgetImpactDao: BudgetImpactDao,
    private val transactionDao: TransactionDao,
    private val pendingImpactService: PendingImpactService,
    private val recurringRepository: RecurringRepository,
    private val clock: AppClock,
) {
    private val longDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    suspend fun saveActiveBudgetPeriod(
        name: String,
        totalAmountMinor: Long,
        currencyCode: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): PendingApplicationResult {
        require(name.isNotBlank()) { "Budget name is required." }
        require(totalAmountMinor > 0) { "Budget amount must be positive." }
        require(!endDate.isBefore(startDate)) { "End date must be on or after start date." }
        val today = clock.today()
        require(!today.isBefore(startDate) && !today.isAfter(endDate)) {
            "Active budget period must include today."
        }

        val active = budgetPeriodDao.getActive()
        val now = clock.nowMillis()
        val periodId = database.withTransaction {
            if (active != null) {
                budgetPeriodDao.setActive(active.id, false)
            }
            budgetPeriodDao.enforceSingleActive()
            val period = BudgetPeriodEntity(
                name = name.trim(),
                startDate = startDate,
                endDate = endDate,
                totalAmountMinor = totalAmountMinor,
                currencyCode = currencyCode.trim().uppercase().ifBlank { "IDR" },
                isActive = true,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            )
            budgetPeriodDao.upsert(period)
        }
        val result = pendingImpactService.applyPendingImpactsForPeriod(periodId)
        refreshQuickAccess(appContext)
        return result
    }

    suspend fun createNextPeriod(
        name: String,
        totalAmountMinor: Long,
        currencyCode: String,
        startDate: LocalDate,
        endDate: LocalDate,
        copyCategoryIds: List<Long> = emptyList(),
        applyImpactIds: List<Long> = emptyList(),
        impactCategoryMapping: Map<Long, Long> = emptyMap(),
        applyRecurringPreviewKeys: List<String> = emptyList(),
        recurringCategoryMapping: Map<String, Long> = emptyMap(),
    ) {
        require(name.isNotBlank()) { "Budget name is required." }
        require(totalAmountMinor > 0) { "Budget amount must be positive." }
        require(!endDate.isBefore(startDate)) { "End date must be on or after start date." }

        val activePeriod = budgetPeriodDao.getActive()
        val now = clock.nowMillis()
        val copyTranslation = mutableMapOf<Long, Long>()
        val newPeriodId = database.withTransaction {
            if (activePeriod != null) {
                budgetPeriodDao.setActive(activePeriod.id, false)
            }
            budgetPeriodDao.enforceSingleActive()

            val newId = budgetPeriodDao.upsert(
                BudgetPeriodEntity(
                    name = name.trim(),
                    startDate = startDate,
                    endDate = endDate,
                    totalAmountMinor = totalAmountMinor,
                    currencyCode = currencyCode.trim().uppercase().ifBlank { "IDR" },
                    isActive = true,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )

            copyCategoryIds.forEach { oldCategoryId ->
                val oldCategory = budgetCategoryDao.getById(oldCategoryId) ?: return@forEach
                val newCategoryId = budgetCategoryDao.upsert(
                    BudgetCategoryEntity(
                        budgetPeriodId = newId,
                        name = oldCategory.name,
                        iconKey = oldCategory.iconKey,
                        allocatedAmountMinor = oldCategory.allocatedAmountMinor,
                        displayOrder = oldCategory.displayOrder,
                        isArchived = false,
                    ),
                )
                copyTranslation[oldCategoryId] = newCategoryId
            }

            newId
        }

        val translatedImpactMapping = impactCategoryMapping.mapNotNull { (impactId, oldCategoryId) ->
            copyTranslation[oldCategoryId]?.let { impactId to it }
        }.toMap()
        pendingImpactService.applyPendingImpactsForPeriod(newPeriodId, translatedImpactMapping, applyImpactIds.toSet())
        recurringRepository.applyRecurringTemplatesForPeriod(
            newPeriodId = newPeriodId,
            startDate = startDate,
            endDate = endDate,
            currencyCode = currencyCode,
            targetCategoryIds = copyTranslation.values.toSet(),
            applyRecurringPreviewKeys = applyRecurringPreviewKeys.toSet(),
            translatedRecurringMapping = recurringCategoryMapping.mapNotNull { (previewKey, oldCategoryId) ->
                copyTranslation[oldCategoryId]?.let { previewKey to it }
            }.toMap(),
        )
        pendingImpactService.notifyPendingImpactsChanged()
        refreshQuickAccess(appContext)
    }

    suspend fun topUpActiveBudget(amountMinor: Long) {
        require(amountMinor > 0) { "Top-up amount must be positive." }
        val active = budgetPeriodDao.getActive()
            ?: throw IllegalArgumentException("No active budget period to top up.")
        val now = clock.nowMillis()
        val paidDate = clock.today()
        database.withTransaction {
            budgetPeriodDao.topUp(active.id, amountMinor)
            transactionDao.upsert(
                TransactionEntity(
                    title = "Budget Top Up",
                    note = active.name,
                    amountMinor = amountMinor,
                    paidDate = paidDate,
                    paidAtEpochMillis = now,
                    categoryId = null,
                    splitCount = 1,
                    sourceType = TransactionSourceType.MANUAL.dbValue,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
        }
        refreshQuickAccess(appContext)
    }

    suspend fun deleteBudgetPeriod(periodId: Long) {
        val period = budgetPeriodDao.getById(periodId)
            ?: throw IllegalArgumentException("Budget period not found.")
        database.withTransaction {
            val transactionIds = budgetImpactDao.getTransactionIdsForPeriod(periodId)
            if (transactionIds.isNotEmpty()) {
                budgetImpactDao.deleteByTransactionIds(transactionIds)
                transactionDao.deleteByIds(transactionIds)
            }
            budgetPeriodDao.deleteById(period.id)
        }
        pendingImpactService.notifyPendingImpactsChanged()
        refreshQuickAccess(appContext)
    }

    suspend fun enforceSingleActivePeriod() {
        budgetPeriodDao.enforceSingleActive()
    }

    fun observeAllPeriods(): Flow<List<PeriodSummary>> =
        budgetPeriodDao.observeAll().flatMapLatest { periods ->
            if (periods.isEmpty()) flowOf(emptyList())
            else combine(periods.map { period ->
                budgetImpactDao.observeSpentForPeriod(period.id).flatMapLatest { spent ->
                    flowOf(
                        PeriodSummary(
                            id = period.id,
                            name = period.name,
                            dateRangeLabel = formatDateRange(period.startDate, period.endDate),
                            totalAmountMinor = period.totalAmountMinor,
                            spentAmountMinor = spent,
                            remainingAmountMinor = period.totalAmountMinor - spent,
                            currencyCode = period.currencyCode,
                            isActive = period.isActive,
                            categoryCount = 0,
                        ),
                    )
                }
            }) { it.toList() }
        }

    private fun formatDateRange(startDate: LocalDate, endDate: LocalDate): String =
        "${startDate.format(longDateFormatter)} - ${endDate.format(longDateFormatter)}"
}
