package com.budging.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.budging.app.data.backup.BackupRepository
import com.budging.app.data.backup.URIResult
import com.budging.app.data.local.query.TransactionHistoryRow
import com.budging.app.data.model.BudgetSetupState
import com.budging.app.data.model.CategoryDetailState
import com.budging.app.data.model.DashboardState
import com.budging.app.data.model.ExpenseEntryState
import com.budging.app.data.model.TransactionDetailState
import com.budging.app.data.repo.BudgetRepository
import com.budging.app.domain.SplitExpensePlanner
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
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
    private val budgetRepository: BudgetRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {
    val dashboardState: StateFlow<DashboardState> = budgetRepository.observeDashboard()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardState.Empty,
        )

    val budgetSetupState: StateFlow<BudgetSetupState> = budgetRepository.observeBudgetSetup()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetSetupState(
                startDateText = LocalDate.now().toString(),
                endDateText = LocalDate.now().toString(),
            ),
        )

    val expenseEntryState: StateFlow<ExpenseEntryState> = budgetRepository.observeExpenseEntry()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExpenseEntryState.Empty,
        )

    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    val categoryDetailState: StateFlow<CategoryDetailState?> = selectedCategoryId
        .flatMapLatest { categoryId ->
            if (categoryId == null) {
                flowOf(null)
            } else {
                budgetRepository.observeCategoryDetail(categoryId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val selectedTransactionId = MutableStateFlow<Long?>(null)
    val transactionDetailState: StateFlow<TransactionDetailState?> = selectedTransactionId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else flowOf(budgetRepository.getTransactionDetail(id))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val transactionHistoryState: StateFlow<List<TransactionHistoryRow>> =
        budgetRepository.observeAllTransactions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loadCategory(categoryId: Long) {
        selectedCategoryId.value = categoryId
    }

    fun loadTransaction(transactionId: Long) {
        selectedTransactionId.value = transactionId
    }

    fun clearTransactionDetail() {
        selectedTransactionId.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    fun saveBudgetPeriod(
        name: String,
        totalAmountMinor: Long,
        currencyCode: String,
        startDateText: String,
        endDateText: String,
    ) {
        viewModelScope.launch {
            runCatching {
                val startDate = LocalDate.parse(startDateText)
                val endDate = LocalDate.parse(endDateText)
                budgetRepository.saveActiveBudgetPeriod(
                    name = name,
                    totalAmountMinor = totalAmountMinor,
                    currencyCode = currencyCode,
                    startDate = startDate,
                    endDate = endDate,
                )
            }.onSuccess { pendingResult ->
                _message.value = buildString {
                    append("Budget period saved.")
                    if (pendingResult.appliedCount > 0) {
                        append(" Applied ${pendingResult.appliedCount} pending impact")
                        if (pendingResult.appliedCount > 1) append("s")
                        append(".")
                    }
                    if (pendingResult.unresolvedCount > 0) {
                        append(" ${pendingResult.unresolvedCount} pending impact")
                        if (pendingResult.unresolvedCount > 1) append("s")
                        append(" still need a matching future category.")
                    }
                }
            }.onFailure {
                _message.value = it.message ?: "Could not save budget period."
            }
        }
    }

    fun saveCategory(categoryId: Long?, name: String, allocatedAmountMinor: Long) {
        viewModelScope.launch {
            runCatching {
                budgetRepository.saveCategory(
                    categoryId = categoryId,
                    name = name,
                    allocatedAmountMinor = allocatedAmountMinor,
                )
            }.onSuccess {
                _message.value = if (categoryId == null) "Category added." else "Category updated."
            }.onFailure {
                _message.value = it.message ?: "Could not save category."
            }
        }
    }

    fun setCategoryArchived(categoryId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            budgetRepository.setCategoryArchived(categoryId, isArchived)
            _message.value = if (isArchived) "Category archived." else "Category restored."
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            runCatching {
                budgetRepository.deleteCategory(categoryId)
            }.onSuccess {
                _message.value = "Category deleted."
            }.onFailure {
                _message.value = it.message ?: "Could not delete category."
            }
        }
    }

    fun logNormalExpense(
        amountMinor: Long,
        categoryId: Long,
        note: String,
        dateText: String,
    ) {
        viewModelScope.launch {
            runCatching {
                val date = LocalDate.parse(dateText)
                val paidAt = if (date == LocalDate.now()) {
                    System.currentTimeMillis()
                } else {
                    LocalDateTime.of(date, LocalTime.NOON)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }
                budgetRepository.logNormalExpense(
                    amountMinor = amountMinor,
                    categoryId = categoryId,
                    note = note,
                    paidAtEpochMillis = paidAt,
                )
            }.onSuccess {
                _message.value = "Expense logged."
            }.onFailure {
                _message.value = it.message ?: "Could not log expense."
            }
        }
    }

    fun logSplitExpense(
        amountMinor: Long,
        categoryId: Long,
        note: String,
        dateText: String,
        periodCount: Int,
    ) {
        viewModelScope.launch {
            runCatching {
                val date = LocalDate.parse(dateText)
                val paidAt = if (date == LocalDate.now()) {
                    System.currentTimeMillis()
                } else {
                    LocalDateTime.of(date, LocalTime.NOON)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }
                budgetRepository.logSplitExpense(
                    amountMinor = amountMinor,
                    categoryId = categoryId,
                    note = note,
                    paidAtEpochMillis = paidAt,
                    periodCount = periodCount,
                )
            }.onSuccess {
                _message.value = "Split expense logged."
            }.onFailure {
                _message.value = it.message ?: "Could not log split expense."
            }
        }
    }

    fun previewCurrentImpact(amountMinor: Long, splitPeriodCount: Int): Long {
        if (amountMinor <= 0) return 0
        val safeCount = splitPeriodCount.coerceIn(
            SplitExpensePlanner.MIN_SPLIT_PERIODS,
            SplitExpensePlanner.MAX_SPLIT_PERIODS,
        )
        return SplitExpensePlanner.splitAmounts(amountMinor, safeCount).first()
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            budgetRepository.deleteTransaction(transactionId)
            _message.value = "Transaction deleted."
        }
    }

    fun editNormalExpense(
        transactionId: Long,
        amountMinor: Long,
        categoryId: Long,
        note: String,
        dateText: String,
    ) {
        viewModelScope.launch {
            runCatching {
                val date = LocalDate.parse(dateText)
                val paidAt = if (date == LocalDate.now()) {
                    System.currentTimeMillis()
                } else {
                    LocalDateTime.of(date, LocalTime.NOON)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }
                budgetRepository.editNormalExpense(
                    transactionId = transactionId,
                    amountMinor = amountMinor,
                    categoryId = categoryId,
                    note = note,
                    paidAtEpochMillis = paidAt,
                )
            }.onSuccess {
                _message.value = "Expense updated."
                selectedTransactionId.value = null
            }.onFailure {
                _message.value = it.message ?: "Could not update expense."
            }
        }
    }

    fun editTransactionNote(
        transactionId: Long,
        note: String,
        dateText: String,
    ) {
        viewModelScope.launch {
            runCatching {
                val date = LocalDate.parse(dateText)
                val paidAt = if (date == LocalDate.now()) {
                    System.currentTimeMillis()
                } else {
                    LocalDateTime.of(date, LocalTime.NOON)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }
                budgetRepository.editTransactionNote(
                    transactionId = transactionId,
                    note = note,
                    paidAtEpochMillis = paidAt,
                )
            }.onSuccess {
                _message.value = "Expense updated."
                selectedTransactionId.value = null
            }.onFailure {
                _message.value = it.message ?: "Could not update expense."
            }
        }
    }

    // --- Backup ---

    private val _backupMessage = MutableStateFlow<BackupMessage?>(null)
    val backupMessage: StateFlow<BackupMessage?> = _backupMessage

    fun clearBackupMessage() {
        _backupMessage.value = null
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
}

sealed class BackupMessage {
    data class Success(val text: String) : BackupMessage()
    data class Error(val text: String) : BackupMessage()
}

class BudgingViewModelFactory(
    private val budgetRepository: BudgetRepository,
    private val backupRepository: BackupRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgingViewModel(budgetRepository, backupRepository) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
