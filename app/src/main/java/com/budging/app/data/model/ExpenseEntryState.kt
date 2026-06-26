package com.budging.app.data.model

data class ExpenseEntryState(
    val hasActiveBudget: Boolean,
    val currencyCode: String,
    val budgetName: String,
    val dateRangeLabel: String,
    val categories: List<ExpenseCategoryOption>,
) {
    companion object {
        val Empty = ExpenseEntryState(
            hasActiveBudget = false,
            currencyCode = "IDR",
            budgetName = "",
            dateRangeLabel = "",
            categories = emptyList(),
        )
    }
}

data class ExpenseCategoryOption(
    val id: Long,
    val name: String,
    val remainingAmountMinor: Long,
)
