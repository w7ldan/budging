package com.budging.app.data.backup

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
)

object BackupSerializer {
    fun serialize(backup: BackupJson): String = JSONObject().apply {
        put("schemaVersion", backup.schemaVersion)
        put("appName", backup.appName)
        put("exportedAt", backup.exportedAt)
        put("budgetPeriods", periodsToJson(backup.budgetPeriods))
        put("budgetCategories", categoriesToJson(backup.budgetCategories))
        put("transactions", transactionsToJson(backup.transactions))
        put("budgetImpacts", impactsToJson(backup.budgetImpacts))
    }.toString(2)

    fun deserialize(json: String): BackupJson {
        val root = JSONObject(json)
        return BackupJson(
            schemaVersion = root.getInt("schemaVersion"),
            appName = root.optString("appName", ""),
            exportedAt = root.optString("exportedAt", ""),
            budgetPeriods = parsePeriods(root.getJSONArray("budgetPeriods")),
            budgetCategories = parseCategories(root.getJSONArray("budgetCategories")),
            transactions = parseTransactions(root.getJSONArray("transactions")),
            budgetImpacts = parseImpacts(root.getJSONArray("budgetImpacts")),
        )
    }

    fun validate(backup: BackupJson): ValidationResult {
        val errors = mutableListOf<String>()

        if (backup.schemaVersion < 1 || backup.schemaVersion > CURRENT_SCHEMA_VERSION) {
            errors.add("Unsupported schemaVersion: ${backup.schemaVersion}. Supported: 1..$CURRENT_SCHEMA_VERSION")
            return ValidationResult(false, errors)
        }

        if (backup.exportedAt.isBlank()) errors.add("exportedAt is required")

        // Collect valid IDs for reference checking
        val periodIds = backup.budgetPeriods.map { it.id }.toSet()
        val categoryIds = backup.budgetCategories.map { it.id }.toSet()
        val transactionIds = backup.transactions.map { it.id }.toSet()

        // Validate periods
        backup.budgetPeriods.forEach { p ->
            if (p.id == 0L) errors.add("BudgetPeriod id must be non-zero")
            if (p.name.isBlank()) errors.add("BudgetPeriod ${p.id}: name is required")
            if (p.totalAmountMinor < 0) errors.add("BudgetPeriod ${p.id}: negative totalAmountMinor")
            if (p.currencyCode.isBlank()) errors.add("BudgetPeriod ${p.id}: currencyCode is required")
        }

        // Validate categories
        backup.budgetCategories.forEach { c ->
            if (c.id == 0L) errors.add("BudgetCategory id must be non-zero")
            if (c.name.isBlank()) errors.add("BudgetCategory ${c.id}: name is required")
            if (c.budgetPeriodId !in periodIds) {
                errors.add("BudgetCategory ${c.id}: references missing period ${c.budgetPeriodId}")
            }
        }

        // Validate transactions
        backup.transactions.forEach { t ->
            if (t.id == 0L) errors.add("Transaction id must be non-zero")
            if (t.title.isBlank()) errors.add("Transaction ${t.id}: title is required")
            if (t.paidDate.isBlank()) errors.add("Transaction ${t.id}: paidDate is required")
        }

        // Validate impacts
        backup.budgetImpacts.forEach { i ->
            if (i.transactionId !in transactionIds) {
                errors.add("BudgetImpact ${i.id}: references missing transaction ${i.transactionId}")
            }
            if (i.budgetPeriodId != null && i.budgetPeriodId !in periodIds) {
                errors.add("BudgetImpact ${i.id}: references missing period ${i.budgetPeriodId}")
            }
            if (i.categoryId != null && i.categoryId !in categoryIds) {
                errors.add("BudgetImpact ${i.id}: references missing category ${i.categoryId}")
            }
            if (i.status !in setOf("APPLIED", "PENDING")) {
                errors.add("BudgetImpact ${i.id}: invalid status '${i.status}'")
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    // --- Private helpers ---

    private fun periodsToJson(list: List<PeriodJson>) = JSONArray().apply {
        list.forEach { p ->
            put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("startDate", p.startDate)
                put("endDate", p.endDate)
                put("totalAmountMinor", p.totalAmountMinor)
                put("currencyCode", p.currencyCode)
                put("createdAtEpochMillis", p.createdAtEpochMillis)
                put("updatedAtEpochMillis", p.updatedAtEpochMillis)
            })
        }
    }

    private fun categoriesToJson(list: List<CategoryJson>) = JSONArray().apply {
        list.forEach { c ->
            put(JSONObject().apply {
                put("id", c.id)
                put("budgetPeriodId", c.budgetPeriodId)
                put("name", c.name)
                put("allocatedAmountMinor", c.allocatedAmountMinor)
                put("displayOrder", c.displayOrder)
                put("isArchived", c.isArchived)
            })
        }
    }

    private fun transactionsToJson(list: List<TransactionJson>) = JSONArray().apply {
        list.forEach { t ->
            put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("note", t.note ?: JSONObject.NULL)
                put("amountMinor", t.amountMinor)
                put("paidDate", t.paidDate)
                put("paidAtEpochMillis", t.paidAtEpochMillis)
                put("categoryId", t.categoryId ?: JSONObject.NULL)
                put("splitCount", t.splitCount)
                put("createdAtEpochMillis", t.createdAtEpochMillis)
                put("updatedAtEpochMillis", t.updatedAtEpochMillis)
            })
        }
    }

    private fun impactsToJson(list: List<ImpactJson>) = JSONArray().apply {
        list.forEach { i ->
            put(JSONObject().apply {
                put("id", i.id)
                put("transactionId", i.transactionId)
                put("budgetPeriodId", i.budgetPeriodId ?: JSONObject.NULL)
                put("categoryId", i.categoryId ?: JSONObject.NULL)
                put("sourceBudgetPeriodId", i.sourceBudgetPeriodId ?: JSONObject.NULL)
                put("categoryNameSnapshot", i.categoryNameSnapshot)
                put("amountMinor", i.amountMinor)
                put("impactDate", i.impactDate)
                put("plannedPeriodOffset", i.plannedPeriodOffset)
                put("pendingPeriodStartDate", i.pendingPeriodStartDate ?: JSONObject.NULL)
                put("status", i.status)
            })
        }
    }

    // --- Parsers ---

    private fun parsePeriods(arr: JSONArray): List<PeriodJson> =
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            PeriodJson(
                id = o.getLong("id"),
                name = o.getString("name"),
                startDate = o.getString("startDate"),
                endDate = o.getString("endDate"),
                totalAmountMinor = o.getLong("totalAmountMinor"),
                currencyCode = o.getString("currencyCode"),
                createdAtEpochMillis = o.getLong("createdAtEpochMillis"),
                updatedAtEpochMillis = o.getLong("updatedAtEpochMillis"),
            )
        }

    private fun parseCategories(arr: JSONArray): List<CategoryJson> =
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CategoryJson(
                id = o.getLong("id"),
                budgetPeriodId = o.getLong("budgetPeriodId"),
                name = o.getString("name"),
                allocatedAmountMinor = o.getLong("allocatedAmountMinor"),
                displayOrder = o.getInt("displayOrder"),
                isArchived = o.getBoolean("isArchived"),
            )
        }

    private fun parseTransactions(arr: JSONArray): List<TransactionJson> =
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TransactionJson(
                id = o.getLong("id"),
                title = o.getString("title"),
                note = if (o.isNull("note")) null else o.getString("note"),
                amountMinor = o.getLong("amountMinor"),
                paidDate = o.getString("paidDate"),
                paidAtEpochMillis = o.getLong("paidAtEpochMillis"),
                categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                splitCount = o.getInt("splitCount"),
                createdAtEpochMillis = o.getLong("createdAtEpochMillis"),
                updatedAtEpochMillis = o.getLong("updatedAtEpochMillis"),
            )
        }

    private fun parseImpacts(arr: JSONArray): List<ImpactJson> =
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ImpactJson(
                id = o.getLong("id"),
                transactionId = o.getLong("transactionId"),
                budgetPeriodId = if (o.isNull("budgetPeriodId")) null else o.getLong("budgetPeriodId"),
                categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                sourceBudgetPeriodId = if (o.isNull("sourceBudgetPeriodId")) null else o.getLong("sourceBudgetPeriodId"),
                categoryNameSnapshot = o.getString("categoryNameSnapshot"),
                amountMinor = o.getLong("amountMinor"),
                impactDate = o.getString("impactDate"),
                plannedPeriodOffset = o.getInt("plannedPeriodOffset"),
                pendingPeriodStartDate = if (o.isNull("pendingPeriodStartDate")) null else o.getString("pendingPeriodStartDate"),
                status = o.getString("status"),
            )
        }
}
