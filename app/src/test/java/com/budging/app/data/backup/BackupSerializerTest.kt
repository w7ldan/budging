package com.budging.app.data.backup

import com.budging.app.data.local.entity.BudgetCategoryEntity
import com.budging.app.data.local.entity.BudgetImpactEntity
import com.budging.app.data.local.entity.BudgetPeriodEntity
import com.budging.app.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupSerializerTest {

    @Test
    fun `serialize includes all required top-level fields`() {
        val backup = BackupJson(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            appName = "Budging",
            exportedAt = "2026-06-26T12:00:00Z",
            budgetPeriods = listOf(samplePeriod),
            budgetCategories = listOf(sampleCategory),
            transactions = listOf(sampleTransaction),
            budgetImpacts = listOf(sampleImpact),
        )

        val json = BackupSerializer.serialize(backup)

        assertTrue("Missing schemaVersion", json.contains("\"schemaVersion\""))
        assertTrue("Missing appName", json.contains("\"appName\""))
        assertTrue("Missing exportedAt", json.contains("\"exportedAt\""))
        assertTrue("Missing budgetPeriods", json.contains("\"budgetPeriods\""))
        assertTrue("Missing budgetCategories", json.contains("\"budgetCategories\""))
        assertTrue("Missing transactions", json.contains("\"transactions\""))
        assertTrue("Missing budgetImpacts", json.contains("\"budgetImpacts\""))
    }

    @Test
    fun `validation rejects unsupported schemaVersion`() {
        val backup = emptyBackup(schemaVersion = 999)
        val result = BackupSerializer.validate(backup)
        assertFalse("Should be invalid", result.isValid)
        assertTrue(result.errors.any { it.contains("Unsupported schemaVersion") })
    }

    @Test
    fun `validation rejects schemaVersion zero`() {
        val backup = emptyBackup(schemaVersion = 0)
        val result = BackupSerializer.validate(backup)
        assertFalse("Should be invalid", result.isValid)
    }

    @Test
    fun `validation accepts current schemaVersion`() {
        val backup = emptyBackup(schemaVersion = CURRENT_SCHEMA_VERSION)
        val result = BackupSerializer.validate(backup)
        assertTrue("Should be valid but got: ${result.errors}", result.isValid)
    }

    @Test
    fun `validation rejects category with missing period reference`() {
        val backup = emptyBackup(schemaVersion = CURRENT_SCHEMA_VERSION).copy(
            budgetCategories = listOf(sampleCategory.copy(budgetPeriodId = 999)),
        )
        val result = BackupSerializer.validate(backup)
        assertFalse("Should be invalid", result.isValid)
        assertTrue(result.errors.any { it.contains("missing period") })
    }

    @Test
    fun `validation rejects impact with missing transaction reference`() {
        val backup = emptyBackup(schemaVersion = CURRENT_SCHEMA_VERSION).copy(
            transactions = listOf(sampleTransaction),
            budgetImpacts = listOf(sampleImpact.copy(transactionId = 888)),
        )
        val result = BackupSerializer.validate(backup)
        assertFalse("Should be invalid", result.isValid)
        assertTrue(result.errors.any { it.contains("missing transaction") })
    }

    @Test
    fun `validation rejects impact with invalid status`() {
        val backup = emptyBackup(schemaVersion = CURRENT_SCHEMA_VERSION).copy(
            transactions = listOf(sampleTransaction),
            budgetImpacts = listOf(sampleImpact.copy(status = "INVALID")),
        )
        val result = BackupSerializer.validate(backup)
        assertFalse("Should be invalid", result.isValid)
        assertTrue(result.errors.any { it.contains("invalid status") })
    }

    @Test
    fun `validation accepts pending impact with null budgetPeriodId`() {
        val backup = emptyBackup(schemaVersion = CURRENT_SCHEMA_VERSION).copy(
            transactions = listOf(sampleTransaction),
            budgetImpacts = listOf(
                sampleImpact.copy(
                    status = "PENDING",
                    budgetPeriodId = null,
                    categoryId = null,
                ),
            ),
        )
        val result = BackupSerializer.validate(backup)
        assertTrue("Pending impact with null refs should be valid: ${result.errors}", result.isValid)
    }

    @Test
    fun `round-trip preserves all fields`() {
        val original = BackupJson(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            appName = "Budging",
            exportedAt = "2026-06-26T12:00:00Z",
            budgetPeriods = listOf(samplePeriod),
            budgetCategories = listOf(sampleCategory),
            transactions = listOf(sampleTransaction),
            budgetImpacts = listOf(sampleImpact, pendingImpact),
        )

        val json = BackupSerializer.serialize(original)
        val deserialized = BackupSerializer.deserialize(json)

        assertEquals(original.schemaVersion, deserialized.schemaVersion)
        assertEquals(original.budgetPeriods, deserialized.budgetPeriods)
        assertEquals(original.budgetCategories, deserialized.budgetCategories)
        assertEquals(original.transactions, deserialized.transactions)
        assertEquals(original.budgetImpacts, deserialized.budgetImpacts)
    }

    @Test
    fun `entity to json and back preserves split impact fields`() {
        val entity = BudgetImpactEntity(
            id = 10,
            transactionId = 3,
            budgetPeriodId = null,
            categoryId = null,
            sourceBudgetPeriodId = 1,
            categoryNameSnapshot = "Groceries",
            amountMinor = 25000,
            impactDate = LocalDate.of(2026, 7, 1),
            plannedPeriodOffset = 1,
            pendingPeriodStartDate = LocalDate.of(2026, 7, 1),
            status = "PENDING",
        )

        val json = entity.toJson()
        val restored = json.toEntity()

        assertEquals(entity.id, restored.id)
        assertEquals(entity.transactionId, restored.transactionId)
        assertEquals(entity.budgetPeriodId, restored.budgetPeriodId)
        assertEquals(entity.categoryId, restored.categoryId)
        assertEquals(entity.sourceBudgetPeriodId, restored.sourceBudgetPeriodId)
        assertEquals(entity.categoryNameSnapshot, restored.categoryNameSnapshot)
        assertEquals(entity.amountMinor, restored.amountMinor)
        assertEquals(entity.impactDate, restored.impactDate)
        assertEquals(entity.plannedPeriodOffset, restored.plannedPeriodOffset)
        assertEquals(entity.pendingPeriodStartDate, restored.pendingPeriodStartDate)
        assertEquals(entity.status, restored.status)
    }

    @Test
    fun `CSV export includes transaction rows`() {
        val period = samplePeriodEntity
        val category = sampleCategoryEntity
        val transaction = sampleTransactionEntity
        val impact = BudgetImpactEntity(
            id = 1,
            transactionId = 1,
            budgetPeriodId = 1,
            categoryId = 1,
            sourceBudgetPeriodId = null,
            categoryNameSnapshot = "Food",
            amountMinor = 50000,
            impactDate = LocalDate.of(2026, 6, 25),
            status = "APPLIED",
        )

        val csv = CsvExporter.export(
            periods = listOf(period),
            categories = listOf(category),
            transactions = listOf(transaction),
            impacts = listOf(impact),
        )

        assertTrue("CSV should have header row", csv.lines().first().contains("Transaction ID"))
        assertTrue("CSV should have header row", csv.lines().first().contains("Paid Date"))
        assertTrue("CSV should have header row", csv.lines().first().contains("Category"))
        assertTrue("CSV should have header row", csv.lines().first().contains("Budget Period"))
        assertTrue("CSV should contain transaction title", csv.contains("Weekend groceries"))
        assertTrue("CSV should contain status APPLIED", csv.contains("APPLIED"))
        assertTrue("CSV should have at least header + 1 data row", csv.lines().size >= 2)
    }

    @Test
    fun `CSV export includes one row per impact for split transactions`() {
        val transaction = sampleTransactionEntity.copy(splitCount = 3)
        val impact1 = BudgetImpactEntity(
            id = 1, transactionId = 1, budgetPeriodId = 1, categoryId = 1,
            sourceBudgetPeriodId = null, categoryNameSnapshot = "Food",
            amountMinor = 50000, impactDate = LocalDate.of(2026, 6, 25),
            status = "APPLIED",
        )
        val impact2 = BudgetImpactEntity(
            id = 2, transactionId = 1, budgetPeriodId = null, categoryId = null,
            sourceBudgetPeriodId = 1, categoryNameSnapshot = "Food",
            amountMinor = 50000, impactDate = LocalDate.of(2026, 6, 25),
            plannedPeriodOffset = 1, pendingPeriodStartDate = LocalDate.of(2026, 7, 1),
            status = "PENDING",
        )
        val impact3 = BudgetImpactEntity(
            id = 3, transactionId = 1, budgetPeriodId = null, categoryId = null,
            sourceBudgetPeriodId = 1, categoryNameSnapshot = "Food",
            amountMinor = 50000, impactDate = LocalDate.of(2026, 6, 25),
            plannedPeriodOffset = 2, pendingPeriodStartDate = LocalDate.of(2026, 8, 1),
            status = "PENDING",
        )

        val csv = CsvExporter.export(
            periods = listOf(samplePeriodEntity),
            categories = listOf(sampleCategoryEntity),
            transactions = listOf(transaction),
            impacts = listOf(impact1, impact2, impact3),
        )

        assertTrue("CSV should have one row per impact", csv.lines().size >= 4)
        assertEquals(2, csv.lines().count { it.contains("PENDING") })
        assertEquals(1, csv.lines().count { it.contains("APPLIED") })
    }

    // --- Helpers ---

    private val samplePeriod = PeriodJson(
        id = 1, name = "July 2026",
        startDate = "2026-07-01", endDate = "2026-07-31",
        totalAmountMinor = 5_000_000, currencyCode = "IDR",
        createdAtEpochMillis = 1000, updatedAtEpochMillis = 2000,
    )

    private val sampleCategory = CategoryJson(
        id = 1, budgetPeriodId = 1, name = "Food",
        allocatedAmountMinor = 2_000_000, displayOrder = 0, isArchived = false,
    )

    private val sampleTransaction = TransactionJson(
        id = 1, title = "Weekend groceries", note = "Market",
        amountMinor = 150_000, paidDate = "2026-07-05",
        paidAtEpochMillis = 1000, categoryId = 1L, splitCount = 1,
        createdAtEpochMillis = 1000, updatedAtEpochMillis = 1000,
    )

    private val sampleImpact = ImpactJson(
        id = 1, transactionId = 1, budgetPeriodId = 1, categoryId = 1,
        sourceBudgetPeriodId = null, categoryNameSnapshot = "Food",
        amountMinor = 150_000, impactDate = "2026-07-05",
        plannedPeriodOffset = 0, pendingPeriodStartDate = null, status = "APPLIED",
    )

    private val pendingImpact = ImpactJson(
        id = 2, transactionId = 1, budgetPeriodId = null, categoryId = null,
        sourceBudgetPeriodId = 1, categoryNameSnapshot = "Food",
        amountMinor = 100_000, impactDate = "2026-07-05",
        plannedPeriodOffset = 1, pendingPeriodStartDate = "2026-08-01",
        status = "PENDING",
    )

    private val samplePeriodEntity = BudgetPeriodEntity(
        id = 1, name = "July 2026",
        startDate = LocalDate.of(2026, 7, 1), endDate = LocalDate.of(2026, 7, 31),
        totalAmountMinor = 5_000_000, currencyCode = "IDR",
        createdAtEpochMillis = 1000, updatedAtEpochMillis = 2000,
    )

    private val sampleCategoryEntity = BudgetCategoryEntity(
        id = 1, budgetPeriodId = 1, name = "Food",
        allocatedAmountMinor = 2_000_000, displayOrder = 0, isArchived = false,
    )

    private val sampleTransactionEntity = TransactionEntity(
        id = 1, title = "Weekend groceries", note = "Market",
        amountMinor = 150_000, paidDate = LocalDate.of(2026, 7, 5),
        paidAtEpochMillis = 1000, categoryId = 1L, splitCount = 1,
        createdAtEpochMillis = 1000, updatedAtEpochMillis = 1000,
    )

    private fun emptyBackup(schemaVersion: Int) = BackupJson(
        schemaVersion = schemaVersion,
        appName = "Budging",
        exportedAt = "2026-01-01T00:00:00Z",
        budgetPeriods = emptyList(),
        budgetCategories = emptyList(),
        transactions = emptyList(),
        budgetImpacts = emptyList(),
    )
}
