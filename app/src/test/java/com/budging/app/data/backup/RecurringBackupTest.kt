package com.budging.app.data.backup

import com.budging.app.ui.component.resolveCategoryIconKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringBackupTest {
    @Test
    fun recurringTemplatesRoundTripWithIcons() {
        val backup = BackupJson(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            appName = "Budging",
            exportedAt = "2026-06-26T12:00:00Z",
            budgetPeriods = emptyList(),
            budgetCategories = listOf(
                CategoryJson(
                    id = 1,
                    budgetPeriodId = 2,
                    name = "Travel",
                    iconKey = "travel",
                    allocatedAmountMinor = 1000,
                    displayOrder = 0,
                    isArchived = false,
                ),
            ),
            transactions = emptyList(),
            budgetImpacts = emptyList(),
            recurringExpenseTemplates = listOf(
                RecurringTemplateJson(
                    id = 9,
                    title = "Netflix",
                    amountMinor = 100_000,
                    currencyCode = "IDR",
                    categoryNameSnapshot = "Fun",
                    iconKey = "fun",
                    note = "Streaming",
                    frequency = "MONTHLY",
                    startDate = "2026-01-01",
                    endDate = null,
                    dayOfMonth = 15,
                    applyMode = "CONFIRM",
                    isActive = true,
                    createdAtEpochMillis = 1,
                    updatedAtEpochMillis = 2,
                ),
            ),
        )

        val restored = BackupSerializer.deserialize(BackupSerializer.serialize(backup))

        assertEquals("travel", restored.budgetCategories.single().iconKey)
        assertEquals("fun", restored.recurringExpenseTemplates.single().iconKey)
    }

    @Test
    fun unknownIconKeyFallsBackFromName() {
        assertEquals("coffee", resolveCategoryIconKey("Coffee Run", "missing"))
        assertEquals("other", resolveCategoryIconKey("Completely Custom", "missing"))
        assertTrue(resolveCategoryIconKey("Travel Fund", null).isNotBlank())
    }
}
