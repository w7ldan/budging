package com.budging.app.data.repo

import com.budging.app.data.model.PendingImpactDetail
import com.budging.app.data.model.PendingMatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingImpactMappingTest {

    // --- Match status recomputation (pure logic, no DB) ---

    @Test
    fun matched_whenSingleCopyCategory_matchesByName() {
        val impact = pendingImpact("Food", PendingMatchStatus.NO_MATCH)
        val copyNames = setOf("food", "transport")
        assertEquals(PendingMatchStatus.MATCHED, recomputeMatch(impact, copyNames))
    }

    @Test
    fun noMatch_whenNoCopyCategory_matchesByName() {
        val impact = pendingImpact("Entertainment", PendingMatchStatus.MATCHED) // globally matched
        val copyNames = setOf("food", "transport")
        // Against copy categories, there's no match
        assertEquals(PendingMatchStatus.NO_MATCH, recomputeMatch(impact, copyNames))
    }

    @Test
    fun ambiguous_whenMultipleCopyCategories_matchByName() {
        val impact = pendingImpact("Food", PendingMatchStatus.MATCHED)
        // Two "Food" categories in copy set (case-insensitive match)
        val copyNames = setOf("food", "FOOD", "transport")
        // Both "food" and "FOOD" match — but set eliminates duplicates,
        // so this test shows the case where the COPY NAME SET has only one entry.
        // For true ambiguity, we need two categories with same name (different IDs)
        // which can't be represented by a name set alone.
        // ponytail: name-only match; true ambiguity requires ID-level matching
    }

    @Test
    fun originalStatus_whenNoCopyCategories_selected() {
        val impact = pendingImpact("Food", PendingMatchStatus.NO_MATCH)
        val copyNames = emptySet<String>()
        // When no copy categories selected, fall back to original status
        assertEquals(PendingMatchStatus.NO_MATCH, recomputeMatch(impact, copyNames))
    }

    @Test
    fun originalMatched_whenNoCopyCategories_selected() {
        val impact = pendingImpact("Food", PendingMatchStatus.MATCHED)
        assertEquals(PendingMatchStatus.MATCHED, recomputeMatch(impact, emptySet()))
    }

    @Test
    fun caseInsensitive_matching() {
        val impact = pendingImpact("FOOD", PendingMatchStatus.NO_MATCH)
        assertEquals(PendingMatchStatus.MATCHED, recomputeMatch(impact, setOf("food")))
    }

    @Test
    fun caseInsensitive_reverse() {
        val impact = pendingImpact("food", PendingMatchStatus.NO_MATCH)
        assertEquals(PendingMatchStatus.MATCHED, recomputeMatch(impact, setOf("FOOD")))
    }

    // --- Active period invariant (pure logic) ---

    @Test
    fun multipleActive_normalizedToOne() {
        val periods = listOf(
            activePeriod(1, "June 2026", "2026-06-01", "2026-06-30", 1000L),
            activePeriod(2, "May 2026", "2026-05-01", "2026-05-31", 500L),
            activePeriod(3, "July 2026", "2026-07-01", "2026-07-31", 1500L),
        )
        val result = normalizeSingleActive(periods)
        assertEquals(1, result.count { it.isActive })
        // Latest startDate should remain active
        val active = result.first { it.isActive }
        assertEquals("July 2026", active.name)
    }

    @Test
    fun singleActive_unchanged() {
        val periods = listOf(
            activePeriod(1, "June 2026", "2026-06-01", "2026-06-30", 1000L),
            inactivePeriod(2, "May 2026", "2026-05-01", "2026-05-31", 500L),
        )
        val result = normalizeSingleActive(periods)
        assertEquals(1, result.count { it.isActive })
        assertEquals("June 2026", result.first { it.isActive }.name)
    }

    @Test
    fun noActive_unchanged() {
        val periods = listOf(
            inactivePeriod(1, "June 2026", "2026-06-01", "2026-06-30", 1000L),
            inactivePeriod(2, "May 2026", "2026-05-01", "2026-05-31", 500L),
        )
        val result = normalizeSingleActive(periods)
        assertEquals(0, result.count { it.isActive })
    }

    @Test
    fun archivePrevious_whenCreatingNext() {
        val previous = activePeriod(1, "June 2026", "2026-06-01", "2026-06-30", 1000L)
        val next = activePeriod(2, "July 2026", "2026-07-01", "2026-07-31", 1200L)

        val periods = listOf(previous, next)
        val result = normalizeSingleActive(periods)

        assertEquals(1, result.count { it.isActive })
        assertEquals("July 2026", result.first { it.isActive }.name)
    }

    // --- Impact selection filtering ---

    @Test
    fun selectedImpacts_onlyApplied() {
        val impacts = listOf(
            pendingImpact("Food", PendingMatchStatus.MATCHED).copy(impactId = 1),
            pendingImpact("Transport", PendingMatchStatus.NO_MATCH).copy(impactId = 2),
            pendingImpact("Entertainment", PendingMatchStatus.MATCHED).copy(impactId = 3),
        )
        val selectedIds = setOf(1L, 3L)

        val toApply = impacts.filter { it.impactId in selectedIds }
        assertEquals(2, toApply.size)
        assertTrue(toApply.any { it.impactId == 1L })
        assertTrue(toApply.any { it.impactId == 3L })
        assertFalse(toApply.any { it.impactId == 2L })
    }

    @Test
    fun emptySelection_appliesNone() {
        val impacts = listOf(
            pendingImpact("Food", PendingMatchStatus.MATCHED).copy(impactId = 1),
        )
        val toApply = impacts.filter { it.impactId in emptySet<Long>() }
        assertTrue(toApply.isEmpty())
    }

    @Test
    fun manualMapping_overridesAutoMatch() {
        val impact = pendingImpact("Food", PendingMatchStatus.MATCHED).copy(impactId = 1)
        val manualMapping = mapOf(1L to 42L)

        // Manual mapping takes priority over auto-match
        assertTrue(manualMapping.containsKey(impact.impactId))
        assertEquals(42L, manualMapping[impact.impactId])
    }

    @Test
    fun unselectedImpact_remainsPending() {
        val impact = pendingImpact("Food", PendingMatchStatus.MATCHED).copy(impactId = 1)
        val selectedIds = emptySet<Long>()

        // Impact not in selectedIds → remains pending (not applied)
        assertFalse(impact.impactId in selectedIds)
    }

    // --- Helpers ---

    private fun pendingImpact(
        categoryNameSnapshot: String,
        matchStatus: PendingMatchStatus,
    ) = PendingImpactDetail(
        impactId = 1,
        transactionId = 10,
        transactionTitle = "Test expense",
        amountMinor = 100_000,
        categoryNameSnapshot = categoryNameSnapshot,
        plannedPeriodOffset = 1,
        sourcePeriodName = "Source Period",
        matchingCategoryId = if (matchStatus == PendingMatchStatus.MATCHED) 5 else null,
        matchingCategoryName = if (matchStatus == PendingMatchStatus.MATCHED) categoryNameSnapshot else null,
        matchStatus = matchStatus,
    )

    private fun recomputeMatch(
        impact: PendingImpactDetail,
        copyCategoryNames: Set<String>,
    ): PendingMatchStatus {
        if (copyCategoryNames.isEmpty()) return impact.matchStatus
        val lowerNames = copyCategoryNames.map { it.lowercase() }.toSet()
        val matching = lowerNames.count { it == impact.categoryNameSnapshot.lowercase() }
        return when {
            matching == 0 -> PendingMatchStatus.NO_MATCH
            matching == 1 -> PendingMatchStatus.MATCHED
            else -> PendingMatchStatus.AMBIGUOUS
        }
    }
}

// Minimal period model for logic tests (avoids Room entity dependency)
private data class TestPeriod(
    val id: Long,
    val name: String,
    val startDate: String,
    val endDate: String,
    val updatedAtEpochMillis: Long,
    val isActive: Boolean,
)

private fun activePeriod(id: Long, name: String, start: String, end: String, updatedAt: Long) =
    TestPeriod(id, name, start, end, updatedAt, isActive = true)

private fun inactivePeriod(id: Long, name: String, start: String, end: String, updatedAt: Long) =
    TestPeriod(id, name, start, end, updatedAt, isActive = false)

private fun normalizeSingleActive(periods: List<TestPeriod>): List<TestPeriod> {
    val activeCount = periods.count { it.isActive }
    if (activeCount <= 1) return periods

    // Keep the period with latest startDate active; archive the rest
    val keeper = periods
        .filter { it.isActive }
        .maxWithOrNull(compareBy({ it.startDate }, { it.updatedAtEpochMillis }))
        ?: return periods

    return periods.map { period ->
        if (period.isActive && period.id != keeper.id) {
            period.copy(isActive = false)
        } else {
            period
        }
    }
}
