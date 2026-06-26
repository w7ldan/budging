package com.budging.app.data.repo

import com.budging.app.data.local.dao.BudgetCategoryDao
import com.budging.app.data.local.dao.BudgetImpactDao
import com.budging.app.data.local.dao.BudgetPeriodDao
import com.budging.app.data.local.dao.TransactionDao
import com.budging.app.data.model.CategoryDetailState
import com.budging.app.data.model.DashboardCategory
import com.budging.app.data.model.DashboardState
import com.budging.app.data.model.RecentTransaction
import com.budging.app.domain.BudgetMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BudgetRepository(
    private val budgetPeriodDao: BudgetPeriodDao,
    private val budgetCategoryDao: BudgetCategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetImpactDao: BudgetImpactDao,
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")

    fun observeDashboard(referenceDate: LocalDate = LocalDate.now()): Flow<DashboardState> =
        budgetPeriodDao.observeActive(referenceDate.toEpochDay()).flatMapLatest { period ->
            if (period == null) {
                flowOf(DashboardState.Empty)
            } else {
                combine(
                    budgetCategoryDao.observeForPeriod(period.id),
                    budgetImpactDao.observeSpentForPeriod(period.id),
                    budgetImpactDao.observeCategorySpending(period.id),
                    transactionDao.observeRecentForPeriod(period.id, limit = 5),
                ) { categories, spentForPeriod, categorySpending, recentTransactions ->
                    val spendingByCategory = categorySpending.associate { it.categoryId to it.spentMinor }
                    val categoryCards = categories.map { category ->
                        val spent = spendingByCategory[category.id] ?: 0L
                        DashboardCategory(
                            id = category.id,
                            name = category.name,
                            allocatedAmountMinor = category.allocatedAmountMinor,
                            spentAmountMinor = spent,
                            remainingAmountMinor = category.allocatedAmountMinor - spent,
                        )
                    }
                    val totalRemaining = period.totalAmountMinor - spentForPeriod
                    val daysLeft = BudgetMath.daysRemainingInclusive(referenceDate, period.endDate)
                    DashboardState(
                        periodName = period.name,
                        totalBudgetMinor = period.totalAmountMinor,
                        totalSpentMinor = spentForPeriod,
                        totalRemainingMinor = totalRemaining,
                        daysRemainingInclusive = daysLeft,
                        safeDailyMinor = BudgetMath.safeDailySpend(totalRemaining, daysLeft),
                        categories = categoryCards,
                        recentTransactions = recentTransactions.map { transaction ->
                            RecentTransaction(
                                id = transaction.id,
                                title = transaction.title,
                                amountMinor = transaction.amountMinor,
                                paidDateLabel = transaction.paidDate.format(dateFormatter),
                            )
                        },
                    )
                }
            }
        }

    fun observeCategoryDetail(categoryId: Long): Flow<CategoryDetailState?> =
        combine(
            budgetCategoryDao.observeById(categoryId),
            budgetImpactDao.observeSpentForCategory(categoryId),
            transactionDao.observeForCategory(categoryId),
        ) { category, spent, transactions ->
            category?.let {
                CategoryDetailState(
                    categoryName = it.name,
                    allocatedAmountMinor = it.allocatedAmountMinor,
                    spentAmountMinor = spent,
                    remainingAmountMinor = it.allocatedAmountMinor - spent,
                    transactions = transactions.map { transaction ->
                        RecentTransaction(
                            id = transaction.id,
                            title = transaction.title,
                            amountMinor = transaction.amountMinor,
                            paidDateLabel = transaction.paidDate.format(dateFormatter),
                        )
                    },
                )
            }
        }
}
