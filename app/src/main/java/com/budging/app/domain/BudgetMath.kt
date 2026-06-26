package com.budging.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object BudgetMath {
    fun daysRemainingInclusive(today: LocalDate, endDate: LocalDate): Long {
        if (endDate.isBefore(today)) return 0
        return ChronoUnit.DAYS.between(today, endDate) + 1
    }

    fun safeDailySpend(remainingAmountMinor: Long, daysRemainingInclusive: Long): Long {
        if (daysRemainingInclusive <= 0) return 0
        return remainingAmountMinor / daysRemainingInclusive
    }
}
