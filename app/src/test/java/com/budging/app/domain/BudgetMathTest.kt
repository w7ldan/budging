package com.budging.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetMathTest {
    @Test
    fun safeDailySpend_usesInclusiveDays() {
        val today = LocalDate.of(2026, 6, 26)
        val endDate = LocalDate.of(2026, 7, 9)

        val days = BudgetMath.daysRemainingInclusive(today, endDate)
        val daily = BudgetMath.safeDailySpend(2_450_000, days)

        assertEquals(14, days)
        assertEquals(175_000, daily)
    }
}
