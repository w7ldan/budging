package com.budging.app.domain

import com.budging.app.data.local.entity.RecurringExpenseTemplateEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringExpensePlannerTest {
    @Test
    fun everyBudgetPeriod_producesOneOccurrence() {
        val template = template(frequency = RECURRING_FREQUENCY_EVERY_BUDGET_PERIOD)

        val occurrences = RecurringExpensePlanner.occurrencesForPeriod(
            template = template,
            periodStart = LocalDate.of(2026, 7, 10),
            periodEnd = LocalDate.of(2026, 7, 20),
        )

        assertEquals(1, occurrences.size)
        assertEquals(LocalDate.of(2026, 7, 10), occurrences.single().occurrenceDate)
    }

    @Test
    fun monthly_appliesInsideArbitraryPeriod() {
        val template = template(
            frequency = RECURRING_FREQUENCY_MONTHLY,
            dayOfMonth = 15,
        )

        val occurrences = RecurringExpensePlanner.occurrencesForPeriod(
            template = template,
            periodStart = LocalDate.of(2026, 8, 10),
            periodEnd = LocalDate.of(2026, 8, 21),
        )

        assertEquals(listOf(LocalDate.of(2026, 8, 15)), occurrences.map { it.occurrenceDate })
    }

    @Test
    fun monthly_doesNotApplyOutsideArbitraryPeriod() {
        val template = template(
            frequency = RECURRING_FREQUENCY_MONTHLY,
            dayOfMonth = 15,
        )

        val occurrences = RecurringExpensePlanner.occurrencesForPeriod(
            template = template,
            periodStart = LocalDate.of(2026, 8, 16),
            periodEnd = LocalDate.of(2026, 8, 25),
        )

        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun monthly_day31_clampsToShortMonth() {
        val template = template(
            frequency = RECURRING_FREQUENCY_MONTHLY,
            dayOfMonth = 31,
        )

        val occurrences = RecurringExpensePlanner.occurrencesForPeriod(
            template = template,
            periodStart = LocalDate.of(2026, 2, 1),
            periodEnd = LocalDate.of(2026, 2, 28),
        )

        assertEquals(listOf(LocalDate.of(2026, 2, 28)), occurrences.map { it.occurrenceDate })
    }

    @Test
    fun holidayPeriod_stillMatchesMonthlyOccurrence() {
        val template = template(
            frequency = RECURRING_FREQUENCY_MONTHLY,
            dayOfMonth = 15,
        )

        val occurrences = RecurringExpensePlanner.occurrencesForPeriod(
            template = template,
            periodStart = LocalDate.of(2026, 12, 12),
            periodEnd = LocalDate.of(2026, 12, 18),
        )

        assertEquals(listOf(LocalDate.of(2026, 12, 15)), occurrences.map { it.occurrenceDate })
    }

    private fun template(
        frequency: String,
        dayOfMonth: Int? = null,
    ) = RecurringExpenseTemplateEntity(
        id = 1,
        title = "Netflix",
        amountMinor = 100_000,
        currencyCode = "IDR",
        categoryNameSnapshot = "Fun",
        iconKey = "fun",
        note = null,
        frequency = frequency,
        startDate = LocalDate.of(2026, 1, 1),
        endDate = null,
        dayOfMonth = dayOfMonth,
        applyMode = RECURRING_APPLY_MODE_CONFIRM,
        isActive = true,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
    )
}
