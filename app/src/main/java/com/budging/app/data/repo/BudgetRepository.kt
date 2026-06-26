package com.budging.app.data.repo

import android.content.Context
import androidx.room.withTransaction
import com.budging.app.data.local.BudgingDatabase
import com.budging.app.data.local.dao.BudgetCategoryDao
import com.budging.app.data.local.dao.BudgetImpactDao
import com.budging.app.data.local.dao.BudgetPeriodDao
import com.budging.app.data.local.dao.RecurringExpenseTemplateDao
import com.budging.app.data.local.dao.TransactionDao
import com.budging.app.data.local.entity.BudgetCategoryEntity
import com.budging.app.data.local.entity.BudgetImpactEntity
import com.budging.app.data.local.entity.BudgetPeriodEntity
import com.budging.app.data.local.entity.RecurringExpenseTemplateEntity
import com.budging.app.data.local.entity.TransactionEntity
import com.budging.app.data.local.query.TransactionImpactRow
import com.budging.app.data.local.query.TransactionHistoryRow
import com.budging.app.data.model.BudgetCategoryItem
import com.budging.app.data.model.BudgetSetupState
import com.budging.app.data.model.CategoryDetailState
import com.budging.app.data.model.DashboardCategory
import com.budging.app.data.model.DashboardState
import com.budging.app.data.model.ExpenseCategoryOption
import com.budging.app.data.model.ExpenseEntryState
import com.budging.app.data.model.ImpactDetail
import com.budging.app.data.model.PendingImpactDetail
import com.budging.app.data.model.PendingMatchStatus
import com.budging.app.data.model.PeriodSummary
import com.budging.app.data.model.RecentTransaction
import com.budging.app.data.model.RecurringPreviewItem
import com.budging.app.data.model.RecurringTemplateDraft
import com.budging.app.data.model.RecurringTemplateItem
import com.budging.app.data.model.TransactionDetailState
import com.budging.app.domain.BudgetMath
import com.budging.app.domain.RECURRING_APPLY_MODE_CONFIRM
import com.budging.app.domain.RECURRING_FREQUENCY_EVERY_BUDGET_PERIOD
import com.budging.app.domain.RECURRING_FREQUENCY_MONTHLY
import com.budging.app.domain.RecurringExpensePlanner
import com.budging.app.domain.SplitExpensePlanner
import com.budging.app.domain.TRANSACTION_SOURCE_MANUAL
import com.budging.app.domain.TRANSACTION_SOURCE_RECURRING
import com.budging.app.quickaccess.QuickAccessUpdater
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val STATUS_APPLIED = "APPLIED"
private const val STATUS_PENDING = "PENDING"

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetRepository(
    private val appContext: Context,
    private val database: BudgingDatabase,
    private val budgetPeriodDao: BudgetPeriodDao,
    private val budgetCategoryDao: BudgetCategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetImpactDao: BudgetImpactDao,
    private val recurringTemplateDao: RecurringExpenseTemplateDao,
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
                    val daysLeft = BudgetMath.daysRemainingInclusive(LocalDate.now(), period.endDate)
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
                        startDateText = LocalDate.now().toString(),
                        endDateText = LocalDate.now().toString(),
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

    fun observeRecurringTemplates(): Flow<List<RecurringTemplateItem>> =
        recurringTemplateDao.observeAll().flatMapLatest { templates ->
            flowOf(
                templates.map {
                    RecurringTemplateItem(
                        id = it.id,
                        title = it.title,
                        amountMinor = it.amountMinor,
                        currencyCode = it.currencyCode,
                        categoryNameSnapshot = it.categoryNameSnapshot,
                        iconKey = it.iconKey,
                        note = it.note,
                        frequency = it.frequency,
                        startDate = it.startDate,
                        endDate = it.endDate,
                        dayOfMonth = it.dayOfMonth,
                        isActive = it.isActive,
                    )
                },
            )
        }

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
        val today = LocalDate.now()
        require(!today.isBefore(startDate) && !today.isAfter(endDate)) {
            "Active budget period must include today."
        }

        val active = budgetPeriodDao.getActive()
        val now = System.currentTimeMillis()
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
        val result = applyPendingImpactsForPeriod(periodId)
        refreshQuickAccess()
        return result
    }

    suspend fun saveCategory(
        categoryId: Long?,
        name: String,
        allocatedAmountMinor: Long,
        iconKey: String,
    ) {
        require(name.isNotBlank()) { "Category name is required." }
        require(allocatedAmountMinor > 0) { "Category allocation must be positive." }

        val activePeriod = budgetPeriodDao.getActive()
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
            iconKey = iconKey,
            allocatedAmountMinor = allocatedAmountMinor,
            displayOrder = existing?.displayOrder ?: (budgetCategoryDao.getMaxDisplayOrder(activePeriod.id) + 1),
            isArchived = existing?.isArchived ?: false,
        )
        budgetCategoryDao.upsert(category)
        refreshQuickAccess()
    }

    suspend fun saveRecurringTemplate(draft: RecurringTemplateDraft) {
        require(draft.title.isNotBlank()) { "Subscription name is required." }
        require(draft.amountMinor > 0) { "Subscription amount must be positive." }
        require(draft.categoryNameSnapshot.isNotBlank()) { "Default category is required." }
        require(draft.frequency in setOf(RECURRING_FREQUENCY_EVERY_BUDGET_PERIOD, RECURRING_FREQUENCY_MONTHLY)) {
            "Choose a valid recurring frequency."
        }
        if (draft.frequency == RECURRING_FREQUENCY_MONTHLY) {
            require((draft.dayOfMonth ?: 0) in 1..31) { "Monthly day must be between 1 and 31." }
        }
        draft.endDate?.let { require(!it.isBefore(draft.startDate)) { "End date must be on or after start date." } }

        val existing = draft.templateId?.let { recurringTemplateDao.getById(it) }
        val now = System.currentTimeMillis()
        recurringTemplateDao.upsert(
            RecurringExpenseTemplateEntity(
                id = existing?.id ?: 0,
                title = draft.title.trim(),
                amountMinor = draft.amountMinor,
                currencyCode = draft.currencyCode.trim().uppercase().ifBlank { "IDR" },
                categoryNameSnapshot = draft.categoryNameSnapshot.trim(),
                iconKey = draft.iconKey,
                note = draft.note?.trim()?.ifBlank { null },
                frequency = draft.frequency,
                startDate = draft.startDate,
                endDate = draft.endDate,
                dayOfMonth = draft.dayOfMonth,
                applyMode = RECURRING_APPLY_MODE_CONFIRM,
                isActive = draft.isActive,
                createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                updatedAtEpochMillis = now,
            ),
        )
        refreshQuickAccess()
    }

    suspend fun deleteRecurringTemplate(templateId: Long) {
        recurringTemplateDao.deleteById(templateId)
        refreshQuickAccess()
    }

    suspend fun topUpActiveBudget(amountMinor: Long) {
        require(amountMinor > 0) { "Top-up amount must be positive." }
        val active = budgetPeriodDao.getActive()
            ?: throw IllegalArgumentException("No active budget period to top up.")
        budgetPeriodDao.topUp(active.id, amountMinor)
        refreshQuickAccess()
    }

    suspend fun setCategoryArchived(categoryId: Long, isArchived: Boolean) {
        budgetCategoryDao.setArchived(categoryId, isArchived)
        refreshQuickAccess()
    }

    suspend fun deleteCategory(categoryId: Long) {
        require(budgetImpactDao.countForCategory(categoryId) == 0) {
            "Category already has transactions. Archive it instead of deleting."
        }
        budgetCategoryDao.deleteById(categoryId)
        refreshQuickAccess()
    }

    suspend fun logNormalExpense(
        amountMinor: Long,
        categoryId: Long,
        note: String,
        paidAtEpochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        require(amountMinor > 0) { "Expense amount must be positive." }

        val paidDate = Instant.ofEpochMilli(paidAtEpochMillis).atZone(zoneId).toLocalDate()
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
            sourceType = TRANSACTION_SOURCE_MANUAL,
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
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        require(periodCount in SplitExpensePlanner.MIN_SPLIT_PERIODS..SplitExpensePlanner.MAX_SPLIT_PERIODS) {
            "Split period count must be between ${SplitExpensePlanner.MIN_SPLIT_PERIODS} and ${SplitExpensePlanner.MAX_SPLIT_PERIODS}."
        }
        if (periodCount == 1) {
            logNormalExpense(amountMinor, categoryId, note, paidAtEpochMillis, zoneId)
            return
        }

        val paidDate = Instant.ofEpochMilli(paidAtEpochMillis).atZone(zoneId).toLocalDate()
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
                    splitCount = periodCount,
                    sourceType = TRANSACTION_SOURCE_MANUAL,
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
                        status = STATUS_APPLIED,
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
                        status = STATUS_PENDING,
                    )
                }
            }
            budgetImpactDao.insertAll(impacts)
        }
        refreshQuickAccess()
    }

    suspend fun deleteTransaction(transactionId: Long) {
        transactionDao.deleteById(transactionId)
        refreshQuickAccess()
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
        notifyPendingImpactsChanged()
        refreshQuickAccess()
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
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        require(amountMinor > 0) { "Expense amount must be positive." }
        val paidDate = Instant.ofEpochMilli(paidAtEpochMillis).atZone(zoneId).toLocalDate()
        val activePeriod = budgetPeriodDao.getActive()
            ?: throw IllegalArgumentException("No active budget period.")
        val category = budgetCategoryDao.getById(categoryId)
            ?: throw IllegalArgumentException("Choose a valid category.")
        require(!category.isArchived) { "Archived categories cannot receive new expenses." }

        val existing = transactionDao.getById(transactionId)
            ?: throw IllegalArgumentException("Transaction not found.")
        require(existing.splitCount <= 1) { "Editing split transaction amounts is not supported. Delete and recreate this split expense." }

        val title = note.trim().ifBlank { category.name }
        val now = System.currentTimeMillis()
        database.withTransaction {
            transactionDao.update(
                existing.copy(
                    title = title,
                    note = note.trim().ifBlank { null },
                    amountMinor = amountMinor,
                    paidDate = paidDate,
                    paidAtEpochMillis = paidAtEpochMillis,
                    categoryId = categoryId,
                    updatedAtEpochMillis = now,
                ),
            )
            budgetImpactDao.getByTransactionId(transactionId).firstOrNull { it.status == STATUS_APPLIED }?.let { appliedImpact ->
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
        refreshQuickAccess()
    }

    suspend fun editTransactionNote(
        transactionId: Long,
        note: String,
        paidAtEpochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        val existing = transactionDao.getById(transactionId)
            ?: throw IllegalArgumentException("Transaction not found.")
        val paidDate = Instant.ofEpochMilli(paidAtEpochMillis).atZone(zoneId).toLocalDate()
        val now = System.currentTimeMillis()
        val title = note.trim().ifBlank { existing.title }
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
        refreshQuickAccess()
    }

    private suspend fun refreshQuickAccess() {
        QuickAccessUpdater.refresh(appContext)
    }

    private suspend fun applyPendingImpactsForPeriod(
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

    private val _pendingImpactsVersion = MutableStateFlow(0L)
    val pendingImpactsVersion: StateFlow<Long> = _pendingImpactsVersion

    fun observePendingImpacts(): Flow<List<PendingImpactDetail>> =
        _pendingImpactsVersion.flatMapLatest { flow { emit(buildPendingImpactDetails()) } }

    private fun notifyPendingImpactsChanged() {
        _pendingImpactsVersion.value += 1
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

    suspend fun previewRecurringForPeriod(
        startDate: LocalDate,
        endDate: LocalDate,
        targetCategories: List<BudgetCategoryItem>,
        currencyCode: String,
    ): List<RecurringPreviewItem> {
        val categoriesByName = targetCategories.groupBy { it.name.lowercase() }
        return recurringTemplateDao.getAll()
            .filter { it.isActive }
            .flatMap { template ->
                RecurringExpensePlanner.occurrencesForPeriod(template, startDate, endDate).map { occurrence ->
                    val matches = categoriesByName[template.categoryNameSnapshot.lowercase()].orEmpty()
                    val matchStatus = when {
                        matches.isEmpty() -> PendingMatchStatus.NO_MATCH
                        matches.size == 1 -> PendingMatchStatus.MATCHED
                        else -> PendingMatchStatus.AMBIGUOUS
                    }
                    RecurringPreviewItem(
                        previewKey = "${template.id}:${occurrence.occurrenceDate}",
                        templateId = template.id,
                        title = template.title,
                        amountMinor = template.amountMinor,
                        currencyCode = currencyCode,
                        categoryNameSnapshot = template.categoryNameSnapshot,
                        iconKey = template.iconKey,
                        occurrenceDate = occurrence.occurrenceDate,
                        matchStatus = matchStatus,
                        matchingCategoryId = matches.singleOrNull()?.id,
                        matchingCategoryName = matches.singleOrNull()?.name,
                    )
                }
            }
            .sortedWith(compareBy({ it.occurrenceDate }, { it.title.lowercase() }))
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
        val now = System.currentTimeMillis()

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
        applyPendingImpactsForPeriod(newPeriodId, translatedImpactMapping, applyImpactIds.toSet())
        applyRecurringTemplatesForPeriod(
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
        notifyPendingImpactsChanged()
        refreshQuickAccess()
    }

    private suspend fun applyRecurringTemplatesForPeriod(
        newPeriodId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
        currencyCode: String,
        targetCategoryIds: Set<Long>,
        applyRecurringPreviewKeys: Set<String>,
        translatedRecurringMapping: Map<String, Long>,
    ) {
        if (applyRecurringPreviewKeys.isEmpty()) return
        val targetCategories = budgetCategoryDao.getAll()
            .filter { it.budgetPeriodId == newPeriodId && it.id in targetCategoryIds }
        val previews = previewRecurringForPeriod(
            startDate = startDate,
            endDate = endDate,
            targetCategories = targetCategories.map {
                BudgetCategoryItem(it.id, it.name, it.iconKey, it.allocatedAmountMinor, 0L, it.isArchived, false)
            },
            currencyCode = currencyCode,
        ).associateBy { it.previewKey }

        applyRecurringPreviewKeys.forEach { previewKey ->
            val preview = previews[previewKey] ?: return@forEach
            val template = recurringTemplateDao.getById(preview.templateId) ?: return@forEach
            val categoryId = translatedRecurringMapping[preview.previewKey]
                ?: preview.matchingCategoryId
                ?: return@forEach
            if (transactionDao.findRecurringTransactionId(template.id, preview.occurrenceDate) != null) return@forEach
            val category = budgetCategoryDao.getById(categoryId) ?: return@forEach
            insertAppliedTransaction(
                title = template.title,
                note = template.note.orEmpty(),
                amountMinor = template.amountMinor,
                paidDate = preview.occurrenceDate,
                paidAtEpochMillis = preview.occurrenceDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                category = category,
                budgetPeriodId = newPeriodId,
                sourceType = TRANSACTION_SOURCE_RECURRING,
                recurringTemplateId = template.id,
                sourceOccurrenceDate = preview.occurrenceDate,
            )
        }
    }

    suspend fun enforceSingleActivePeriod() {
        budgetPeriodDao.enforceSingleActive()
    }

    suspend fun manuallyApplyPendingImpact(impactId: Long, budgetPeriodId: Long, categoryId: Long) {
        budgetImpactDao.applyPendingImpact(impactId, budgetPeriodId, categoryId)
        notifyPendingImpactsChanged()
        refreshQuickAccess()
    }

    suspend fun deletePendingImpact(impactId: Long) {
        budgetImpactDao.deleteById(impactId)
        notifyPendingImpactsChanged()
        refreshQuickAccess()
    }

    private suspend fun insertAppliedTransaction(
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
        val now = System.currentTimeMillis()
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
                    status = STATUS_APPLIED,
                ),
            )
        }
        refreshQuickAccess()
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

data class PendingApplicationResult(
    val appliedCount: Int = 0,
    val unresolvedCount: Int = 0,
    val pendingRemaining: Int = 0,
)
