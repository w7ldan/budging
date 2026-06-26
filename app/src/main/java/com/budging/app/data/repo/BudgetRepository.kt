package com.budging.app.data.repo

import androidx.room.withTransaction
import com.budging.app.data.local.BudgingDatabase
import com.budging.app.data.local.dao.BudgetCategoryDao
import com.budging.app.data.local.dao.BudgetImpactDao
import com.budging.app.data.local.dao.BudgetPeriodDao
import com.budging.app.data.local.dao.TransactionDao
import com.budging.app.data.local.entity.BudgetCategoryEntity
import com.budging.app.data.local.entity.BudgetImpactEntity
import com.budging.app.data.local.entity.BudgetPeriodEntity
import com.budging.app.data.local.entity.TransactionEntity
import com.budging.app.data.model.BudgetCategoryItem
import com.budging.app.data.model.BudgetSetupState
import com.budging.app.data.model.CategoryDetailState
import com.budging.app.data.model.DashboardCategory
import com.budging.app.data.model.DashboardState
import com.budging.app.data.model.ExpenseCategoryOption
import com.budging.app.data.model.ExpenseEntryState
import com.budging.app.data.model.RecentTransaction
import com.budging.app.domain.BudgetMath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetRepository(
    private val database: BudgingDatabase,
    private val budgetPeriodDao: BudgetPeriodDao,
    private val budgetCategoryDao: BudgetCategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetImpactDao: BudgetImpactDao,
) {
    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd MMM")
    private val longDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    fun observeDashboard(referenceDate: LocalDate = LocalDate.now()): Flow<DashboardState> =
        budgetPeriodDao.observeActive(referenceDate.toEpochDay()).flatMapLatest { period ->
            if (period == null) {
                flowOf(DashboardState.Empty)
            } else {
                combine(
                    budgetCategoryDao.observeForPeriod(period.id),
                    budgetImpactDao.observeSpentForPeriod(period.id),
                    budgetImpactDao.observeCategorySpending(period.id),
                    transactionDao.observeRecentForPeriod(period.id, limit = 10),
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
                            progressPercent = progressPercent(spent, category.allocatedAmountMinor),
                            isArchived = category.isArchived,
                        )
                    }
                    val totalRemaining = period.totalAmountMinor - spentForPeriod
                    val allocatedTotal = categories.sumOf { it.allocatedAmountMinor }
                    val daysLeft = BudgetMath.daysRemainingInclusive(referenceDate, period.endDate)
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
                        recentTransactions = recentTransactions.map(::toRecentTransaction),
                        hasActiveBudget = true,
                    )
                }
            }
        }

    fun observeBudgetSetup(referenceDate: LocalDate = LocalDate.now()): Flow<BudgetSetupState> =
        budgetPeriodDao.observeActive(referenceDate.toEpochDay()).flatMapLatest { period ->
            if (period == null) {
                flowOf(
                    BudgetSetupState(
                        startDateText = referenceDate.toString(),
                        endDateText = referenceDate.toString(),
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

    fun observeExpenseEntry(referenceDate: LocalDate = LocalDate.now()): Flow<ExpenseEntryState> =
        budgetPeriodDao.observeActive(referenceDate.toEpochDay()).flatMapLatest { period ->
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
                        categories = categories.map { category ->
                            ExpenseCategoryOption(
                                id = category.id,
                                name = category.name,
                                remainingAmountMinor = category.allocatedAmountMinor - (spentByCategory[category.id]
                                    ?: 0L),
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
                    budgetImpactDao.observeSpentForCategory(categoryId),
                    transactionDao.observeForCategory(categoryId),
                ) { period, spent, transactions ->
                    CategoryDetailState(
                        categoryId = category.id,
                        currencyCode = period?.currencyCode ?: "IDR",
                        categoryName = category.name,
                        allocatedAmountMinor = category.allocatedAmountMinor,
                        spentAmountMinor = spent,
                        remainingAmountMinor = category.allocatedAmountMinor - spent,
                        transactions = transactions.map(::toRecentTransaction),
                    )
                }
            }
        }

    suspend fun saveActiveBudgetPeriod(
        name: String,
        totalAmountMinor: Long,
        currencyCode: String,
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate = LocalDate.now(),
    ) {
        require(name.isNotBlank()) { "Budget name is required." }
        require(totalAmountMinor > 0) { "Budget amount must be positive." }
        require(!endDate.isBefore(startDate)) { "End date must be on or after start date." }
        require(!today.isBefore(startDate) && !today.isAfter(endDate)) {
            "Active budget period must include today."
        }

        val active = budgetPeriodDao.getActive(today.toEpochDay())
        val now = System.currentTimeMillis()
        val period = BudgetPeriodEntity(
            id = active?.id ?: 0,
            name = name.trim(),
            startDate = startDate,
            endDate = endDate,
            totalAmountMinor = totalAmountMinor,
            currencyCode = currencyCode.trim().uppercase().ifBlank { "IDR" },
            createdAtEpochMillis = active?.createdAtEpochMillis ?: now,
            updatedAtEpochMillis = now,
        )

        if (active != null) {
            val allocated = budgetCategoryDao.getAllocatedSumForActive(active.id)
            require(allocated <= totalAmountMinor) {
                "Total budget cannot be lower than the current category allocations."
            }
        }

        budgetPeriodDao.upsert(period)
    }

    suspend fun saveCategory(
        categoryId: Long?,
        name: String,
        allocatedAmountMinor: Long,
        today: LocalDate = LocalDate.now(),
    ) {
        require(name.isNotBlank()) { "Category name is required." }
        require(allocatedAmountMinor > 0) { "Category allocation must be positive." }

        val activePeriod = budgetPeriodDao.getActive(today.toEpochDay())
            ?: throw IllegalArgumentException("Create an active budget before adding categories.")
        val existing = categoryId?.let { budgetCategoryDao.getById(it) }
        val allocatedWithoutCurrent = budgetCategoryDao.getAllocatedSumForActive(activePeriod.id) -
            (existing?.takeIf { !it.isArchived }?.allocatedAmountMinor ?: 0L)
        require(allocatedWithoutCurrent + allocatedAmountMinor <= activePeriod.totalAmountMinor) {
            "Category allocations cannot exceed the total budget."
        }

        val category = BudgetCategoryEntity(
            id = existing?.id ?: 0,
            budgetPeriodId = activePeriod.id,
            name = name.trim(),
            allocatedAmountMinor = allocatedAmountMinor,
            displayOrder = existing?.displayOrder ?: (budgetCategoryDao.getMaxDisplayOrder(activePeriod.id) + 1),
            isArchived = existing?.isArchived ?: false,
        )
        budgetCategoryDao.upsert(category)
    }

    suspend fun setCategoryArchived(categoryId: Long, isArchived: Boolean) {
        budgetCategoryDao.setArchived(categoryId, isArchived)
    }

    suspend fun deleteCategory(categoryId: Long) {
        require(budgetImpactDao.countForCategory(categoryId) == 0) {
            "Category already has transactions. Archive it instead of deleting."
        }
        budgetCategoryDao.deleteById(categoryId)
    }

    suspend fun logExpense(
        amountMinor: Long,
        categoryId: Long,
        note: String,
        paidAtEpochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        require(amountMinor > 0) { "Expense amount must be positive." }

        val paidDate = Instant.ofEpochMilli(paidAtEpochMillis).atZone(zoneId).toLocalDate()
        val activePeriod = budgetPeriodDao.getActive(paidDate.toEpochDay())
            ?: throw IllegalArgumentException("Expense date must be inside the active budget period.")
        val category = budgetCategoryDao.getById(categoryId)
            ?: throw IllegalArgumentException("Choose a valid category.")
        require(!category.isArchived) { "Archived categories cannot receive new expenses." }

        val title = note.trim().ifBlank { category.name }
        val now = System.currentTimeMillis()
        database.withTransaction {
            val transactionId = transactionDao.upsert(
                TransactionEntity(
                    title = title,
                    note = note.trim().ifBlank { null },
                    amountMinor = amountMinor,
                    paidDate = paidDate,
                    paidAtEpochMillis = paidAtEpochMillis,
                    categoryId = categoryId,
                    splitCount = 1,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
            budgetImpactDao.insert(
                BudgetImpactEntity(
                    transactionId = transactionId,
                    budgetPeriodId = activePeriod.id,
                    categoryId = categoryId,
                    amountMinor = amountMinor,
                    impactDate = paidDate,
                    status = "APPLIED",
                ),
            )
        }
    }

    suspend fun deleteTransaction(transactionId: Long) {
        transactionDao.deleteById(transactionId)
    }

    private fun toRecentTransaction(transaction: TransactionEntity): RecentTransaction = RecentTransaction(
        id = transaction.id,
        title = transaction.title,
        amountMinor = transaction.amountMinor,
        paidDateLabel = transaction.paidDate.format(shortDateFormatter),
        note = transaction.note,
    )

    private fun formatDateRange(startDate: LocalDate, endDate: LocalDate): String =
        "${startDate.format(longDateFormatter)} - ${endDate.format(longDateFormatter)}"

    private fun progressPercent(spentMinor: Long, allocatedMinor: Long): Int {
        if (allocatedMinor <= 0) return 0
        return ((spentMinor * 100) / allocatedMinor).toInt().coerceAtLeast(0)
    }
}
