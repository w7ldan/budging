package com.budging.app.data.model

import com.budging.app.data.local.entity.BudgetPeriodEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodLifecycleTest {
    @Test
    fun period_defaultsToActive() {
        val period = BudgetPeriodEntity(
            name = "Test",
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 30),
            totalAmountMinor = 5_000_000,
            currencyCode = "IDR",
            createdAtEpochMillis = 0,
            updatedAtEpochMillis = 0,
        )
        assertTrue(period.isActive)
    }

    @Test
    fun period_canBeArchived() {
        val period = BudgetPeriodEntity(
            name = "Test",
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 30),
            totalAmountMinor = 5_000_000,
            isActive = false,
            createdAtEpochMillis = 0,
            updatedAtEpochMillis = 0,
        )
        assertFalse(period.isActive)
    }

    @Test
    fun pendingMatchStatus_values() {
        assertEquals("MATCHED", PendingMatchStatus.MATCHED.name)
        assertEquals("NO_MATCH", PendingMatchStatus.NO_MATCH.name)
        assertEquals("AMBIGUOUS", PendingMatchStatus.AMBIGUOUS.name)
    }

    @Test
    fun periodSummary_activeDetection() {
        val active = PeriodSummary(
            id = 1,
            name = "June 2026",
            dateRangeLabel = "01 Jun 2026 - 30 Jun 2026",
            totalAmountMinor = 5_000_000,
            spentAmountMinor = 2_000_000,
            remainingAmountMinor = 3_000_000,
            currencyCode = "IDR",
            isActive = true,
            categoryCount = 3,
        )
        val archived = active.copy(id = 2, isActive = false)

        assertTrue(active.isActive)
        assertFalse(archived.isActive)
    }

    @Test
    fun pendingImpactDetail_matchStatusValues() {
        val matched = PendingImpactDetail(
            impactId = 1,
            transactionId = 10,
            transactionTitle = "Groceries",
            amountMinor = 100_000,
            categoryNameSnapshot = "Food",
            plannedPeriodOffset = 1,
            sourcePeriodName = "June 2026",
            matchingCategoryId = 5,
            matchingCategoryName = "Food",
            matchStatus = PendingMatchStatus.MATCHED,
        )
        val noMatch = matched.copy(
            impactId = 2,
            matchingCategoryId = null,
            matchingCategoryName = null,
            matchStatus = PendingMatchStatus.NO_MATCH,
        )
        val ambiguous = matched.copy(
            impactId = 3,
            matchingCategoryId = null,
            matchingCategoryName = null,
            matchStatus = PendingMatchStatus.AMBIGUOUS,
        )

        assertEquals(PendingMatchStatus.MATCHED, matched.matchStatus)
        assertEquals(5L, matched.matchingCategoryId)
        assertEquals(PendingMatchStatus.NO_MATCH, noMatch.matchStatus)
        assertEquals(null, noMatch.matchingCategoryId)
        assertEquals(PendingMatchStatus.AMBIGUOUS, ambiguous.matchStatus)
    }

    @Test
    fun pendingImpactDetail_matchedHasCategoryInfo() {
        val detail = PendingImpactDetail(
            impactId = 1,
            transactionId = 10,
            transactionTitle = "Split expense",
            amountMinor = 50_000,
            categoryNameSnapshot = "Food",
            plannedPeriodOffset = 2,
            sourcePeriodName = "June",
            matchingCategoryId = 5,
            matchingCategoryName = "Food",
            matchStatus = PendingMatchStatus.MATCHED,
        )
        assertTrue(
            detail.matchStatus == PendingMatchStatus.MATCHED &&
                detail.matchingCategoryId != null &&
                detail.matchingCategoryName != null,
        )
    }

    @Test
    fun pendingImpactDetail_unmatchedHasNullCategory() {
        val detail = PendingImpactDetail(
            impactId = 1,
            transactionId = 10,
            transactionTitle = "Split expense",
            amountMinor = 50_000,
            categoryNameSnapshot = "UnknownCategory",
            plannedPeriodOffset = 1,
            sourcePeriodName = "June",
            matchingCategoryId = null,
            matchingCategoryName = null,
            matchStatus = PendingMatchStatus.NO_MATCH,
        )
        assertTrue(
            detail.matchStatus != PendingMatchStatus.MATCHED &&
                detail.matchingCategoryId == null,
        )
    }
}
