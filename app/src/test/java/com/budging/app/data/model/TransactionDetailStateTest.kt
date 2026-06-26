package com.budging.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionDetailStateTest {
    @Test
    fun isNormal_true_whenSplitCountIsOne() {
        val state = TransactionDetailState(
            transactionId = 1,
            title = "Coffee",
            note = null,
            amountMinor = 50_000,
            paidDateLabel = "26 Jun 2026",
            paidDateIso = "2026-06-26",
            categoryId = 1,
            categoryName = "Food",
            splitCount = 1,
            currencyCode = "IDR",
            impacts = listOf(
                ImpactDetail(
                    impactId = 1,
                    amountMinor = 50_000,
                    categoryName = "Food",
                    periodName = "June 2026",
                    status = "APPLIED",
                    impactDateLabel = "26 Jun",
                ),
            ),
            isSplit = false,
        )
        assertTrue(state.isNormal)
    }

    @Test
    fun isNormal_false_whenSplitCountGreaterThanOne() {
        val state = TransactionDetailState(
            transactionId = 2,
            title = "Subscription",
            note = null,
            amountMinor = 300_000,
            paidDateLabel = "26 Jun 2026",
            paidDateIso = "2026-06-26",
            categoryId = 1,
            categoryName = "Entertainment",
            splitCount = 3,
            currencyCode = "IDR",
            impacts = listOf(
                ImpactDetail(1, 100_000, "Entertainment", "June", "APPLIED", "26 Jun"),
                ImpactDetail(2, 100_000, "Entertainment", null, "PENDING", "26 Jun"),
                ImpactDetail(3, 100_000, "Entertainment", null, "PENDING", "26 Jun"),
            ),
            isSplit = true,
        )
        assertFalse(state.isNormal)
    }

    @Test
    fun splitDetection_usesSplitCount_notImpactsSize() {
        // A split expense always has multiple impacts but the flag
        // is derived from splitCount, not impacts.size
        val state = TransactionDetailState(
            transactionId = 3,
            title = "Split Item",
            note = null,
            amountMinor = 200_000,
            paidDateLabel = "26 Jun 2026",
            paidDateIso = "2026-06-26",
            categoryId = 1,
            categoryName = "Food",
            splitCount = 2,
            currencyCode = "IDR",
            impacts = listOf(
                ImpactDetail(1, 100_000, "Food", "June", "APPLIED", "26 Jun"),
                ImpactDetail(2, 100_000, "Food", null, "PENDING", "26 Jun"),
            ),
            isSplit = true,
        )
        assertTrue(state.isSplit)
        assertEquals(2, state.impacts.size)
    }

    @Test
    fun impactDetail_holdsAppliedAndPendingStatus() {
        val applied = ImpactDetail(
            impactId = 1,
            amountMinor = 100_000,
            categoryName = "Food",
            periodName = "June 2026",
            status = "APPLIED",
            impactDateLabel = "26 Jun",
        )
        val pending = ImpactDetail(
            impactId = 2,
            amountMinor = 100_000,
            categoryName = "Food",
            periodName = null,
            status = "PENDING",
            impactDateLabel = "26 Jun",
        )

        assertEquals("APPLIED", applied.status)
        assertEquals("PENDING", pending.status)
        assertEquals("June 2026", applied.periodName)
        assertEquals(null, pending.periodName)
    }
}
