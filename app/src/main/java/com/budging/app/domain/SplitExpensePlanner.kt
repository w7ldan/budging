package com.budging.app.domain

object SplitExpensePlanner {
    const val MIN_SPLIT_PERIODS = 1
    const val MAX_SPLIT_PERIODS = 24

    fun splitAmounts(totalAmountMinor: Long, periodCount: Int): List<Long> {
        require(totalAmountMinor > 0) { "Expense amount must be positive." }
        require(periodCount in MIN_SPLIT_PERIODS..MAX_SPLIT_PERIODS) {
            "Split period count must be between $MIN_SPLIT_PERIODS and $MAX_SPLIT_PERIODS."
        }

        val base = totalAmountMinor / periodCount
        val remainder = (totalAmountMinor % periodCount).toInt()
        return List(periodCount) { index ->
            base + if (index < remainder) 1 else 0
        }
    }
}
