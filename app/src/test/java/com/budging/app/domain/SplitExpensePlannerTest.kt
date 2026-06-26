package com.budging.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SplitExpensePlannerTest {
    @Test
    fun splitAmounts_evenlyDivides_whenAmountIsClean() {
        val impacts = SplitExpensePlanner.splitAmounts(
            totalAmountMinor = 900_000,
            periodCount = 3,
        )

        assertEquals(listOf(300_000L, 300_000L, 300_000L), impacts)
    }

    @Test
    fun splitAmounts_distributesRemainderToEarliestImpacts() {
        val impacts = SplitExpensePlanner.splitAmounts(
            totalAmountMinor = 100_000,
            periodCount = 3,
        )

        assertEquals(listOf(33_334L, 33_333L, 33_333L), impacts)
    }

    @Test
    fun splitAmounts_totalAlwaysMatchesOriginalAmount() {
        val impacts = SplitExpensePlanner.splitAmounts(
            totalAmountMinor = 245_001,
            periodCount = 7,
        )

        assertEquals(245_001L, impacts.sum())
        assertEquals(7, impacts.size)
    }

    @Test
    fun splitAmounts_firstImpactIsCurrentPeriodImpactPreview() {
        val impacts = SplitExpensePlanner.splitAmounts(
            totalAmountMinor = 500_000,
            periodCount = 4,
        )

        assertEquals(125_000L, impacts.first())
    }
}
