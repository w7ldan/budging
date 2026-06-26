package com.budging.app.domain

import com.budging.app.data.local.entity.RecurringExpenseTemplateEntity
import java.time.LocalDate
import java.time.YearMonth

val RECURRING_FREQUENCY_EVERY_BUDGET_PERIOD = RecurringFrequency.EVERY_BUDGET_PERIOD.dbValue
val RECURRING_FREQUENCY_MONTHLY = RecurringFrequency.MONTHLY.dbValue
const val RECURRING_APPLY_MODE_CONFIRM = "CONFIRM"
val TRANSACTION_SOURCE_MANUAL = TransactionSourceType.MANUAL.dbValue
val TRANSACTION_SOURCE_RECURRING = TransactionSourceType.RECURRING.dbValue

data class RecurringOccurrence(
    val templateId: Long,
    val occurrenceDate: LocalDate,
)

object RecurringExpensePlanner {
    fun occurrencesForPeriod(
        template: RecurringExpenseTemplateEntity,
        periodStart: LocalDate,
        periodEnd: LocalDate,
    ): List<RecurringOccurrence> {
        if (!template.isActive) return emptyList()
        if (periodEnd.isBefore(template.startDate)) return emptyList()
        val effectiveEnd = template.endDate
        if (effectiveEnd != null && periodStart.isAfter(effectiveEnd)) return emptyList()

        return when (RecurringFrequency.fromDbValue(template.frequency)) {
            RecurringFrequency.EVERY_BUDGET_PERIOD -> {
                listOf(
                    RecurringOccurrence(
                        templateId = template.id,
                        occurrenceDate = maxOf(periodStart, template.startDate),
                    ),
                )
            }

            RecurringFrequency.MONTHLY -> monthlyOccurrences(template, periodStart, periodEnd, effectiveEnd)
        }
    }

    private fun monthlyOccurrences(
        template: RecurringExpenseTemplateEntity,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        templateEnd: LocalDate?,
    ): List<RecurringOccurrence> {
        val day = (template.dayOfMonth ?: template.startDate.dayOfMonth).coerceIn(1, 31)
        val firstMonth = YearMonth.from(periodStart)
        val lastMonth = YearMonth.from(periodEnd)
        val results = mutableListOf<RecurringOccurrence>()
        var month = firstMonth
        while (!month.isAfter(lastMonth)) {
            val occurrenceDate = month.atDay(day.coerceAtMost(month.lengthOfMonth()))
            if (!occurrenceDate.isBefore(periodStart) &&
                !occurrenceDate.isAfter(periodEnd) &&
                !occurrenceDate.isBefore(template.startDate) &&
                (templateEnd == null || !occurrenceDate.isAfter(templateEnd))
            ) {
                results += RecurringOccurrence(template.id, occurrenceDate)
            }
            month = month.plusMonths(1)
        }
        return results
    }
}
