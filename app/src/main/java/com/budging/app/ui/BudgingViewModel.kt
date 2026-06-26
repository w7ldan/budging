package com.budging.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.budging.app.data.model.BudgetSetupState
import com.budging.app.data.model.CategoryDetailState
import com.budging.app.data.model.DashboardState
import com.budging.app.data.model.ExpenseEntryState
import com.budging.app.data.repo.BudgetRepository
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

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loadCategory(categoryId: Long) {
        selectedCategoryId.value = categoryId
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
            }.onSuccess {
                _message.value = "Budget period saved."
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

    fun logExpense(
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
                budgetRepository.logExpense(
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

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            budgetRepository.deleteTransaction(transactionId)
            _message.value = "Transaction deleted."
        }
    }
}

class BudgingViewModelFactory(
    private val budgetRepository: BudgetRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgingViewModel(budgetRepository) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
