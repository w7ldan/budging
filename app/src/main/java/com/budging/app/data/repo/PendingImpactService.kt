package com.budging.app.data.repo

import android.content.Context
import com.budging.app.data.local.dao.BudgetCategoryDao
import com.budging.app.data.local.dao.BudgetImpactDao
import com.budging.app.data.local.dao.BudgetPeriodDao
import com.budging.app.data.local.dao.TransactionDao
import com.budging.app.data.model.PendingImpactDetail
import com.budging.app.data.model.PendingMatchStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

@OptIn(ExperimentalCoroutinesApi::class)
class PendingImpactService(
    private val appContext: Context,
    private val budgetImpactDao: BudgetImpactDao,
    private val budgetPeriodDao: BudgetPeriodDao,
    private val budgetCategoryDao: BudgetCategoryDao,
    private val transactionDao: TransactionDao,
) {
    private val _pendingImpactsVersion = MutableStateFlow(0L)
    val pendingImpactsVersion: StateFlow<Long> = _pendingImpactsVersion

    fun observePendingImpacts(): Flow<List<PendingImpactDetail>> =
        _pendingImpactsVersion.flatMapLatest { flow { emit(buildPendingImpactDetails()) } }

    fun notifyPendingImpactsChanged() {
        _pendingImpactsVersion.value += 1
    }

    suspend fun manuallyApplyPendingImpact(impactId: Long, budgetPeriodId: Long, categoryId: Long) {
        budgetImpactDao.applyPendingImpact(impactId, budgetPeriodId, categoryId)
        notifyPendingImpactsChanged()
        refreshQuickAccess(appContext)
    }

    suspend fun deletePendingImpact(impactId: Long) {
        budgetImpactDao.deleteById(impactId)
        notifyPendingImpactsChanged()
        refreshQuickAccess(appContext)
    }

    suspend fun applyPendingImpactsForPeriod(
        periodId: Long,
        manualMapping: Map<Long, Long> = emptyMap(),
        selectedImpactIds: Set<Long>? = null,
    ): PendingApplicationResult {
        val period = budgetPeriodDao.getById(periodId) ?: return PendingApplicationResult()
        val pendingImpacts = budgetImpactDao.getPendingImpacts()
        var appliedCount = 0
        var unresolvedCount = 0

        pendingImpacts.forEach { impact ->
            if (selectedImpactIds != null && impact.id !in selectedImpactIds) return@forEach
            val manualCategoryId = manualMapping[impact.id]
            if (manualCategoryId != null) {
                budgetImpactDao.applyPendingImpact(impact.id, period.id, manualCategoryId)
                appliedCount += 1
                return@forEach
            }

            val sourcePeriodId = impact.sourceBudgetPeriodId ?: run {
                unresolvedCount += 1
                return@forEach
            }
            val sourcePeriod = budgetPeriodDao.getById(sourcePeriodId) ?: run {
                unresolvedCount += 1
                return@forEach
            }
            if (!period.startDate.isAfter(sourcePeriod.startDate)) return@forEach
            val futurePeriods = budgetPeriodDao.getPeriodsAfter(sourcePeriod.startDate.toEpochDay())
            val periodIndex = futurePeriods.indexOfFirst { it.id == period.id }
            if (periodIndex == -1 || periodIndex + 1 != impact.plannedPeriodOffset) return@forEach

            val matchingCategories = budgetCategoryDao.getActiveByName(period.id, impact.categoryNameSnapshot)
            if (matchingCategories.size == 1) {
                budgetImpactDao.applyPendingImpact(impact.id, period.id, matchingCategories.single().id)
                appliedCount += 1
            } else {
                unresolvedCount += 1
            }
        }

        return PendingApplicationResult(
            appliedCount = appliedCount,
            unresolvedCount = unresolvedCount,
            pendingRemaining = budgetImpactDao.countPendingImpacts(),
        )
    }

    private suspend fun buildPendingImpactDetails(): List<PendingImpactDetail> {
        val pendingImpacts = budgetImpactDao.getPendingImpacts()
        val allCategories = budgetCategoryDao.getAll()
        return pendingImpacts.mapNotNull { impact ->
            val transaction = transactionDao.getById(impact.transactionId) ?: return@mapNotNull null
            val sourcePeriod = impact.sourceBudgetPeriodId?.let { budgetPeriodDao.getById(it) }
            val matching = allCategories.filter { it.name.equals(impact.categoryNameSnapshot, ignoreCase = true) }
            val matchStatus = when {
                matching.isEmpty() -> PendingMatchStatus.NO_MATCH
                matching.size == 1 -> PendingMatchStatus.MATCHED
                else -> PendingMatchStatus.AMBIGUOUS
            }
            PendingImpactDetail(
                impactId = impact.id,
                transactionId = impact.transactionId,
                transactionTitle = transaction.title,
                amountMinor = impact.amountMinor,
                categoryNameSnapshot = impact.categoryNameSnapshot,
                plannedPeriodOffset = impact.plannedPeriodOffset,
                sourcePeriodName = sourcePeriod?.name,
                matchingCategoryId = if (matching.size == 1) matching.single().id else null,
                matchingCategoryName = if (matching.size == 1) matching.single().name else null,
                matchStatus = matchStatus,
            )
        }
    }
}

data class PendingApplicationResult(
    val appliedCount: Int = 0,
    val unresolvedCount: Int = 0,
    val pendingRemaining: Int = 0,
)
