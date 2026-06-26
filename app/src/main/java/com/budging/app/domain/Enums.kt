package com.budging.app.domain

enum class BudgetImpactStatus(val dbValue: String) { APPLIED("APPLIED"), PENDING("PENDING") }

enum class TransactionSourceType(val dbValue: String) { MANUAL("MANUAL"), RECURRING("RECURRING") }

enum class RecurringFrequency(val dbValue: String) {
    EVERY_BUDGET_PERIOD("EVERY_BUDGET_PERIOD"),
    MONTHLY("MONTHLY");

    companion object {
        fun fromDbValue(value: String): RecurringFrequency =
            entries.first { it.dbValue == value }
    }
}
