package com.budging.app.data.model

data class BudgetSetupState(
    val activePeriodId: Long? = null,
    val periodName: String = "",
    val totalAmountMinor: Long = 0,
    val currencyCode: String = "IDR",
    val startDateText: String = "",
    val endDateText: String = "",
    val categories: List<BudgetCategoryItem> = emptyList(),
    val unallocatedAmountMinor: Long = 0,
    val hasActiveBudget: Boolean = false,
)

data class BudgetCategoryItem(
    val id: Long,
    val name: String,
    val iconKey: String,
    val allocatedAmountMinor: Long,
    val spentAmountMinor: Long,
    val isArchived: Boolean,
    val hasTransactions: Boolean,
)
