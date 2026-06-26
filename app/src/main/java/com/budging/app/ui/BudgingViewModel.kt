package com.budging.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.budging.app.data.backup.BackupRepository
import com.budging.app.data.backup.URIResult
import com.budging.app.data.local.query.TransactionHistoryRow
import com.budging.app.data.model.BudgetCategoryItem
import com.budging.app.data.model.BudgetSetupState
import com.budging.app.data.model.CategoryDetailState
import com.budging.app.data.model.DashboardState
import com.budging.app.data.model.ExpenseEntryState
import com.budging.app.data.model.PendingImpactDetail
import com.budging.app.data.model.PeriodSummary
import com.budging.app.data.model.RecurringPreviewItem
import com.budging.app.data.model.RecurringTemplateDraft
import com.budging.app.data.model.RecurringTemplateItem
import com.budging.app.data.model.TransactionDetailState
import com.budging.app.data.repo.BudgetPeriodRepository
import com.budging.app.data.repo.CategoryRepository
import com.budging.app.data.repo.DashboardRepository
import com.budging.app.data.repo.ExpenseRepository
import com.budging.app.data.repo.PendingImpactService
import com.budging.app.data.repo.RecurringRepository
import com.budging.app.data.repo.TransactionRepository
import com.budging.app.domain.AppClock
import com.budging.app.domain.RecurringFrequency
import com.budging.app.domain.SplitExpensePlanner
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BudgingViewModel(
    private val dashboardRepo: DashboardRepository,
    private val budgetPeriodRepo: BudgetPeriodRepository,
    private val categoryRepo: CategoryRepository,
    private val expenseRepo: ExpenseRepository,
    private val transactionRepo: TransactionRepository,
    private val recurringRepo: RecurringRepository,
    private val pendingImpactService: PendingImpactService,
    private val backupRepository: BackupRepository,
    val clock: AppClock,
) : ViewModel() {
    val dashboardState: StateFlow<DashboardState> = dashboardRepo.observeDashboard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState.Empty)

    val budgetSetupState: StateFlow<BudgetSetupState> = dashboardRepo.observeBudgetSetup()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            BudgetSetupState(startDateText = clock.today().toString(), endDateText = clock.today().toString()),
        )

    val expenseEntryState: StateFlow<ExpenseEntryState> = dashboardRepo.observeExpenseEntry()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseEntryState.Empty)

    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    val categoryDetailState: StateFlow<CategoryDetailState?> = selectedCategoryId
        .flatMapLatest { categoryId -> if (categoryId == null) flowOf(null) else dashboardRepo.observeCategoryDetail(categoryId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val selectedTransactionId = MutableStateFlow<Long?>(null)
    val transactionDetailState: StateFlow<TransactionDetailState?> = selectedTransactionId
        .flatMapLatest { id -> if (id == null) flowOf(null) else flowOf(transactionRepo.getTransactionDetail(id)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val transactionHistoryState: StateFlow<List<TransactionHistoryRow>> =
        transactionRepo.observeAllTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val periodListState: StateFlow<List<PeriodSummary>> =
        budgetPeriodRepo.observeAllPeriods()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingImpactsState: StateFlow<List<PendingImpactDetail>> =
        pendingImpactService.observePendingImpacts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recurringTemplatesState: StateFlow<List<RecurringTemplateItem>> =
        recurringRepo.observeRecurringTemplates()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _backupMessage = MutableStateFlow<BackupMessage?>(null)
    val backupMessage: StateFlow<BackupMessage?> = _backupMessage

    fun loadCategory(categoryId: Long) {
        selectedCategoryId.value = categoryId
    }

    fun loadTransaction(transactionId: Long) {
        selectedTransactionId.value = transactionId
    }

    fun clearMessage() {
        _message.value = null
    }

    fun clearBackupMessage() {
        _backupMessage.value = null
    }

    fun saveBudgetPeriod(name: String, totalAmountMinor: Long, currencyCode: String, startDateText: String, endDateText: String) {
        viewModelScope.launch {
            runCatching {
                budgetPeriodRepo.saveActiveBudgetPeriod(
                    name = name,
                    totalAmountMinor = totalAmountMinor,
                    currencyCode = currencyCode,
                    startDate = LocalDate.parse(startDateText),
                    endDate = LocalDate.parse(endDateText),
                )
            }.onSuccess { pendingResult ->
                _message.value = buildString {
                    append("Budget period saved.")
                    if (pendingResult.appliedCount > 0) append(" Applied ${pendingResult.appliedCount} pending impact${if (pendingResult.appliedCount > 1) "s" else ""}.")
                    if (pendingResult.unresolvedCount > 0) append(" ${pendingResult.unresolvedCount} pending impact${if (pendingResult.unresolvedCount > 1) "s" else ""} still need a matching future category.")
                }
            }.onFailure {
                _message.value = it.message ?: "Could not save budget period."
            }
        }
    }

    fun saveCategory(categoryId: Long?, name: String, allocatedAmountMinor: Long, iconKey: String) {
        viewModelScope.launch {
            runCatching {
                categoryRepo.saveCategory(categoryId, name, allocatedAmountMinor, iconKey)
            }.onSuccess {
                _message.value = if (categoryId == null) "Category added." else "Category updated."
            }.onFailure {
                _message.value = it.message ?: "Could not save category."
            }
        }
    }

    fun saveRecurringTemplate(
        templateId: Long?,
        title: String,
        amountMinor: Long,
        currencyCode: String,
        categoryNameSnapshot: String,
        iconKey: String?,
        note: String,
        frequency: String,
        startDateText: String,
        endDateText: String,
        dayOfMonth: Int?,
        isActive: Boolean,
    ) {
        viewModelScope.launch {
            runCatching {
                recurringRepo.saveRecurringTemplate(
                    RecurringTemplateDraft(
                        templateId = templateId,
                        title = title,
                        amountMinor = amountMinor,
                        currencyCode = currencyCode,
                        categoryNameSnapshot = categoryNameSnapshot,
                        iconKey = iconKey,
                        note = note,
                        frequency = frequency,
                        startDate = LocalDate.parse(startDateText),
                        endDate = endDateText.ifBlank { null }?.let(LocalDate::parse),
                        dayOfMonth = if (frequency == RecurringFrequency.EVERY_BUDGET_PERIOD.dbValue) null else dayOfMonth,
                        isActive = isActive,
                    ),
                )
            }.onSuccess {
                _message.value = if (templateId == null) "Subscription added." else "Subscription updated."
            }.onFailure {
                _message.value = it.message ?: "Could not save subscription."
            }
        }
    }

    fun deleteRecurringTemplate(templateId: Long) {
        viewModelScope.launch {
            runCatching { recurringRepo.deleteRecurringTemplate(templateId) }
                .onSuccess { _message.value = "Subscription deleted." }
                .onFailure { _message.value = it.message ?: "Could not delete subscription." }
        }
    }

    fun setCategoryArchived(categoryId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            categoryRepo.setCategoryArchived(categoryId, isArchived)
            _message.value = if (isArchived) "Category archived." else "Category restored."
        }
    }

    fun topUpBudget(amountMinor: Long) {
        viewModelScope.launch {
            runCatching {
                budgetPeriodRepo.topUpActiveBudget(amountMinor)
            }.onSuccess {
                _message.value = "Budget topped up."
            }.onFailure {
                _message.value = it.message ?: "Could not top up budget."
            }
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            runCatching { categoryRepo.deleteCategory(categoryId) }
                .onSuccess { _message.value = "Category deleted." }
                .onFailure { _message.value = it.message ?: "Could not delete category." }
        }
    }

    fun deleteBudgetPeriod(periodId: Long, wasActive: Boolean) {
        viewModelScope.launch {
            runCatching { budgetPeriodRepo.deleteBudgetPeriod(periodId) }
                .onSuccess { _message.value = if (wasActive) "Budget cancelled." else "Budget deleted." }
                .onFailure { _message.value = it.message ?: "Could not delete budget." }
        }
    }

    fun logNormalExpense(amountMinor: Long, categoryId: Long, note: String, dateText: String) {
        viewModelScope.launch {
            runCatching {
                expenseRepo.logNormalExpense(amountMinor, categoryId, note, toEpochMillis(dateText))
            }.onSuccess {
                _message.value = "Expense logged."
            }.onFailure {
                _message.value = it.message ?: "Could not log expense."
            }
        }
    }

    fun logSplitExpense(amountMinor: Long, categoryId: Long, note: String, dateText: String, periodCount: Int) {
        viewModelScope.launch {
            runCatching {
                expenseRepo.logSplitExpense(amountMinor, categoryId, note, toEpochMillis(dateText), periodCount)
            }.onSuccess {
                _message.value = "Split expense logged."
            }.onFailure {
                _message.value = it.message ?: "Could not log split expense."
            }
        }
    }

    fun previewCurrentImpact(amountMinor: Long, splitPeriodCount: Int): Long {
        if (amountMinor <= 0) return 0
        val safeCount = splitPeriodCount.coerceIn(SplitExpensePlanner.MIN_SPLIT_PERIODS, SplitExpensePlanner.MAX_SPLIT_PERIODS)
        return SplitExpensePlanner.splitAmounts(amountMinor, safeCount).first()
    }

    suspend fun previewRecurringForPeriod(
        startDateText: String,
        endDateText: String,
        targetCategories: List<BudgetCategoryItem>,
        currencyCode: String,
    ): List<RecurringPreviewItem> = recurringRepo.previewRecurringForPeriod(
        startDate = LocalDate.parse(startDateText),
        endDate = LocalDate.parse(endDateText),
        targetCategories = targetCategories,
        currencyCode = currencyCode,
    )

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            transactionRepo.deleteTransaction(transactionId)
            _message.value = "Transaction deleted."
        }
    }

    fun editNormalExpense(transactionId: Long, amountMinor: Long, categoryId: Long, note: String, dateText: String) {
        viewModelScope.launch {
            runCatching {
                transactionRepo.editNormalExpense(transactionId, amountMinor, categoryId, note, toEpochMillis(dateText))
            }.onSuccess {
                _message.value = "Expense updated."
                selectedTransactionId.value = null
            }.onFailure {
                _message.value = it.message ?: "Could not update expense."
            }
        }
    }

    fun editTransactionNote(transactionId: Long, note: String, dateText: String) {
        viewModelScope.launch {
            runCatching {
                transactionRepo.editTransactionNote(transactionId, note, toEpochMillis(dateText))
            }.onSuccess {
                _message.value = "Expense updated."
                selectedTransactionId.value = null
            }.onFailure {
                _message.value = it.message ?: "Could not update expense."
            }
        }
    }

    fun createNextPeriod(
        name: String,
        totalAmountMinor: Long,
        currencyCode: String,
        startDateText: String,
        endDateText: String,
        copyCategoryIds: List<Long>,
        applyImpactIds: List<Long>,
        impactCategoryMapping: Map<Long, Long>,
        applyRecurringPreviewKeys: List<String>,
        recurringCategoryMapping: Map<String, Long>,
    ) {
        viewModelScope.launch {
            runCatching {
                budgetPeriodRepo.createNextPeriod(
                    name = name,
                    totalAmountMinor = totalAmountMinor,
                    currencyCode = currencyCode,
                    startDate = LocalDate.parse(startDateText),
                    endDate = LocalDate.parse(endDateText),
                    copyCategoryIds = copyCategoryIds,
                    applyImpactIds = applyImpactIds,
                    impactCategoryMapping = impactCategoryMapping,
                    applyRecurringPreviewKeys = applyRecurringPreviewKeys,
                    recurringCategoryMapping = recurringCategoryMapping,
                )
            }.onSuccess {
                _message.value = "New budget period created."
            }.onFailure {
                _message.value = it.message ?: "Could not create period."
            }
        }
    }

    fun applyPendingImpact(impactId: Long, budgetPeriodId: Long, categoryId: Long) {
        viewModelScope.launch {
            runCatching { pendingImpactService.manuallyApplyPendingImpact(impactId, budgetPeriodId, categoryId) }
                .onSuccess { _message.value = "Pending impact applied." }
                .onFailure { _message.value = it.message ?: "Could not apply impact." }
        }
    }

    fun deletePendingImpact(impactId: Long) {
        viewModelScope.launch {
            runCatching { pendingImpactService.deletePendingImpact(impactId) }
                .onSuccess { _message.value = "Pending impact deleted." }
                .onFailure { _message.value = it.message ?: "Could not delete impact." }
        }
    }

    fun exportJson(uri: Uri) {
        viewModelScope.launch {
            backupRepository.exportJson(uri)
                .onSuccess { _backupMessage.value = BackupMessage.Success("JSON backup exported.") }
                .onFailure { _backupMessage.value = BackupMessage.Error(it.message ?: "Export failed.") }
        }
    }

    fun importJson(uri: Uri) {
        viewModelScope.launch {
            backupRepository.importJson(URIResult(uri))
                .onSuccess { _backupMessage.value = BackupMessage.Success("Data restored from backup.") }
                .onFailure { _backupMessage.value = BackupMessage.Error(it.message ?: "Import failed.") }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            backupRepository.exportCsv(uri)
                .onSuccess { _backupMessage.value = BackupMessage.Success("CSV exported.") }
                .onFailure { _backupMessage.value = BackupMessage.Error(it.message ?: "CSV export failed.") }
        }
    }

    private fun toEpochMillis(dateText: String): Long {
        val date = LocalDate.parse(dateText)
        return clock.toEpochMillis(date)
    }
}

sealed class BackupMessage {
    data class Success(val text: String) : BackupMessage()
    data class Error(val text: String) : BackupMessage()
}

class BudgingViewModelFactory(
    private val dashboardRepo: DashboardRepository,
    private val budgetPeriodRepo: BudgetPeriodRepository,
    private val categoryRepo: CategoryRepository,
    private val expenseRepo: ExpenseRepository,
    private val transactionRepo: TransactionRepository,
    private val recurringRepo: RecurringRepository,
    private val pendingImpactService: PendingImpactService,
    private val backupRepository: BackupRepository,
    private val clock: AppClock,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgingViewModel(
                dashboardRepo, budgetPeriodRepo, categoryRepo, expenseRepo,
                transactionRepo, recurringRepo, pendingImpactService, backupRepository, clock,
            ) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
