package com.budging.app.data.repo

import com.budging.app.data.local.dao.BudgetCategoryDao
import com.budging.app.data.local.dao.BudgetImpactDao
import com.budging.app.data.local.dao.BudgetPeriodDao
import com.budging.app.data.local.dao.TransactionDao
import com.budging.app.data.local.entity.BudgetCategoryEntity
import com.budging.app.data.local.query.TransactionImpactRow
import com.budging.app.data.model.BudgetCategoryItem
import com.budging.app.data.model.BudgetSetupState
import com.budging.app.data.model.CategoryDetailState
import com.budging.app.data.model.DashboardCategory
import com.budging.app.data.model.DashboardState
import com.budging.app.data.model.ExpenseCategoryOption
import com.budging.app.data.model.ExpenseEntryState
import com.budging.app.data.model.RecentTransaction
import com.budging.app.domain.AppClock
import com.budging.app.domain.BudgetMath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardRepository(
    private val budgetPeriodDao: BudgetPeriodDao,
    private val budgetCategoryDao: BudgetCategoryDao,
    private val budgetImpactDao: BudgetImpactDao,
    private val transactionDao: TransactionDao,
    private val clock: AppClock,
) {
    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd MMM")
    private val longDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    suspend fun getDashboardSnapshot(): DashboardState = observeDashboard().first()

    fun observeDashboard(): Flow<DashboardState> =
        budgetPeriodDao.observeActive().flatMapLatest { period ->
            if (period == null) {
                flowOf(DashboardState.Empty)
            } else {
                combine(
                    budgetCategoryDao.observeForPeriod(period.id),
                    budgetImpactDao.observeSpentForPeriod(period.id),
                    budgetImpactDao.observeCategorySpending(period.id),
                    transactionDao.observeRecentForPeriod(period.id, limit = 10),
                ) { categories, spentForPeriod, categorySpending, recentTransactions ->
                    val categoriesById = categories.associateBy { it.id }
                    val spendingByCategory = categorySpending.associate { it.categoryId to it.spentMinor }
                    val categoryCards = categories.map { category ->
                        val spent = spendingByCategory[category.id] ?: 0L
                        DashboardCategory(
                            id = category.id,
                            name = category.name,
                            iconKey = category.iconKey,
                            allocatedAmountMinor = category.allocatedAmountMinor,
                            spentAmountMinor = spent,
                            remainingAmountMinor = category.allocatedAmountMinor - spent,
                            progressPercent = progressPercent(spent, category.allocatedAmountMinor),
                            isArchived = category.isArchived,
                        )
                    }
                    val totalRemaining = period.totalAmountMinor - spentForPeriod
                    val allocatedTotal = categories.sumOf { it.allocatedAmountMinor }
                    val daysLeft = BudgetMath.daysRemainingInclusive(clock.today(), period.endDate)
                    DashboardState(
                        periodId = period.id,
                        periodName = period.name,
                        periodDateRangeLabel = formatDateRange(period.startDate, period.endDate),
                        currencyCode = period.currencyCode,
                        totalBudgetMinor = period.totalAmountMinor,
                        totalSpentMinor = spentForPeriod,
                        totalRemainingMinor = totalRemaining,
                        daysRemainingInclusive = daysLeft,
                        safeDailyMinor = BudgetMath.safeDailySpend(totalRemaining, daysLeft),
                        unallocatedAmountMinor = period.totalAmountMinor - allocatedTotal,
                        categories = categoryCards,
                        recentTransactions = recentTransactions.map { toRecentTransaction(it, categoriesById) },
                        hasActiveBudget = true,
                    )
                }
            }
        }

    fun observeBudgetSetup(): Flow<BudgetSetupState> =
        budgetPeriodDao.observeActive().flatMapLatest { period ->
            if (period == null) {
                flowOf(
                    BudgetSetupState(
                        startDateText = clock.today().toString(),
                        endDateText = clock.today().toString(),
                    ),
                )
            } else {
                combine(
                    budgetCategoryDao.observeAllForPeriod(period.id),
                    budgetImpactDao.observeCategorySpending(period.id),
                ) { categories, categorySpending ->
                    val spentByCategory = categorySpending.associate { it.categoryId to it.spentMinor }
                    BudgetSetupState(
                        activePeriodId = period.id,
                        periodName = period.name,
                        totalAmountMinor = period.totalAmountMinor,
                        currencyCode = period.currencyCode,
                        startDateText = period.startDate.toString(),
                        endDateText = period.endDate.toString(),
                        categories = categories.map { category ->
                            BudgetCategoryItem(
                                id = category.id,
                                name = category.name,
                                iconKey = category.iconKey,
                                allocatedAmountMinor = category.allocatedAmountMinor,
                                spentAmountMinor = spentByCategory[category.id] ?: 0L,
                                isArchived = category.isArchived,
                                hasTransactions = (spentByCategory[category.id] ?: 0L) > 0L,
                            )
                        },
                        unallocatedAmountMinor = period.totalAmountMinor - categories
                            .filterNot { it.isArchived }
                            .sumOf { it.allocatedAmountMinor },
                        hasActiveBudget = true,
                    )
                }
            }
        }

    fun observeExpenseEntry(): Flow<ExpenseEntryState> =
        budgetPeriodDao.observeActive().flatMapLatest { period ->
            if (period == null) {
                flowOf(ExpenseEntryState.Empty)
            } else {
                combine(
                    budgetCategoryDao.observeForPeriod(period.id),
                    budgetImpactDao.observeCategorySpending(period.id),
                ) { categories, categorySpending ->
                    val spentByCategory = categorySpending.associate { it.categoryId to it.spentMinor }
                    ExpenseEntryState(
                        hasActiveBudget = true,
                        currencyCode = period.currencyCode,
                        budgetName = period.name,
                        dateRangeLabel = formatDateRange(period.startDate, period.endDate),
                        pendingImpactCount = budgetImpactDao.countPendingImpacts(),
                        categories = categories.map { category ->
                            ExpenseCategoryOption(
                                id = category.id,
                                name = category.name,
                                iconKey = category.iconKey,
                                remainingAmountMinor = category.allocatedAmountMinor - (spentByCategory[category.id] ?: 0L),
                            )
                        },
                    )
                }
            }
        }

    fun observeCategoryDetail(categoryId: Long): Flow<CategoryDetailState?> =
        budgetCategoryDao.observeById(categoryId).flatMapLatest { category ->
            if (category == null) {
                flowOf(null)
            } else {
                combine(
                    budgetPeriodDao.observeById(category.budgetPeriodId),
                    budgetImpactDao.observeSpentForCategory(categoryId, category.budgetPeriodId),
                    transactionDao.observeForCategory(categoryId, category.budgetPeriodId),
                    budgetCategoryDao.observeForPeriod(category.budgetPeriodId),
                ) { period, spent, transactions, categories ->
                    val categoriesById = categories.associateBy { it.id }
                    CategoryDetailState(
                        categoryId = category.id,
                        currencyCode = period?.currencyCode ?: "IDR",
                        categoryName = category.name,
                        iconKey = category.iconKey,
                        allocatedAmountMinor = category.allocatedAmountMinor,
                        spentAmountMinor = spent,
                        remainingAmountMinor = category.allocatedAmountMinor - spent,
                        pendingImpactCount = budgetImpactDao.countPendingImpacts(),
                        transactions = transactions.map { toRecentTransaction(it, categoriesById) },
                    )
                }
            }
        }

    private fun toRecentTransaction(
        transaction: TransactionImpactRow,
        categoriesById: Map<Long, BudgetCategoryEntity>,
    ): RecentTransaction = RecentTransaction(
        id = transaction.transactionId,
        title = transaction.title,
        paidAmountMinor = transaction.paidAmountMinor,
        impactAmountMinor = transaction.impactAmountMinor,
        splitCount = transaction.splitCount,
        paidDateLabel = transaction.paidDate.format(shortDateFormatter),
        note = transaction.note,
        categoryIconKey = categoriesById[transaction.categoryId]?.iconKey,
    )

    private fun formatDateRange(startDate: LocalDate, endDate: LocalDate): String =
        "${startDate.format(longDateFormatter)} - ${endDate.format(longDateFormatter)}"

    private fun progressPercent(spentMinor: Long, allocatedMinor: Long): Int {
        if (allocatedMinor <= 0) return 0
        return ((spentMinor * 100) / allocatedMinor).toInt().coerceAtLeast(0)
    }
}
