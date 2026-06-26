package com.budging.app.data.backup

import com.budging.app.data.local.entity.BudgetCategoryEntity
import com.budging.app.data.local.entity.BudgetImpactEntity
import com.budging.app.data.local.entity.BudgetPeriodEntity
import com.budging.app.data.local.entity.TransactionEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvExporter {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun export(
        periods: List<BudgetPeriodEntity>,
        categories: List<BudgetCategoryEntity>,
        transactions: List<TransactionEntity>,
        impacts: List<BudgetImpactEntity>,
    ): String {
        val periodById = periods.associateBy { it.id }
        val categoryById = categories.associateBy { it.id }
        val impactsByTx = impacts.groupBy { it.transactionId }

        val header = listOf(
            "Transaction ID", "Title", "Paid Date", "Amount (minor)", "Currency",
            "Category", "Budget Period", "Note",
            "Split Count", "Source Type", "Recurring Template ID", "Source Occurrence Date",
            "Impact Amount (minor)", "Status", "Budget Impact ID",
        )

        val rows = transactions.sortedByDescending { it.paidAtEpochMillis }.map { tx ->
            val txImpacts = impactsByTx[tx.id] ?: emptyList()
            val mainImpact = txImpacts.firstOrNull { it.status == "APPLIED" } ?: txImpacts.firstOrNull()
            val cat = mainImpact?.categoryId?.let { categoryById[it] }
            val period = mainImpact?.budgetPeriodId?.let { periodById[it] }

            txImpacts.map { impact ->
                listOf(
                    tx.id.toString(),
                    tx.title,
                    dateTimeFormatter.format(
                        Instant.ofEpochMilli(tx.paidAtEpochMillis).atZone(ZoneId.systemDefault()),
                    ),
                    tx.amountMinor.toString(),
                    period?.currencyCode ?: "",
                    cat?.name ?: impact.categoryNameSnapshot,
                    "${period?.name ?: "—"} (${period?.startDate?.toString() ?: "?"} to ${period?.endDate?.toString() ?: "?"})",
                    tx.note ?: "",
                    tx.splitCount.toString(),
                    tx.sourceType,
                    tx.recurringTemplateId?.toString().orEmpty(),
                    tx.sourceOccurrenceDate?.toString().orEmpty(),
                    impact.amountMinor.toString(),
                    impact.status,
                    impact.id.toString(),
                )
            }
        }.flatten()

        return buildString {
            appendLine(header.joinToString(",") { escapeCsv(it) })
            rows.forEach { appendLine(it.joinToString(",") { escapeCsv(it) }) }
        }
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }
}
