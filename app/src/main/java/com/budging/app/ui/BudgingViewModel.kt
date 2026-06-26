package com.budging.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.budging.app.data.model.CategoryDetailState
import com.budging.app.data.model.DashboardState
import com.budging.app.data.repo.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class BudgingViewModel(
    private val budgetRepository: BudgetRepository,
) : ViewModel() {
    val dashboardState: StateFlow<DashboardState> = budgetRepository.observeDashboard()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardState.Empty,
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

    fun loadCategory(categoryId: Long) {
        selectedCategoryId.value = categoryId
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
