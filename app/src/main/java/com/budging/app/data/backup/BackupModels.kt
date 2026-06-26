package com.budging.app.data.backup

import com.budging.app.data.local.entity.BudgetCategoryEntity
import com.budging.app.data.local.entity.BudgetImpactEntity
import com.budging.app.data.local.entity.BudgetPeriodEntity
import com.budging.app.data.local.entity.TransactionEntity
import java.time.LocalDate

const val CURRENT_SCHEMA_VERSION = 1

data class BackupJson(
    val schemaVersion: Int,
    val appName: String,
    val exportedAt: String,
    val budgetPeriods: List<PeriodJson>,
    val budgetCategories: List<CategoryJson>,
    val transactions: List<TransactionJson>,
    val budgetImpacts: List<ImpactJson>,
)

data class PeriodJson(
    val id: Long,
    val name: String,
    val startDate: String,
    val endDate: String,
    val totalAmountMinor: Long,
    val currencyCode: String,
    val isActive: Boolean = true,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class CategoryJson(
    val id: Long,
    val budgetPeriodId: Long,
    val name: String,
    val allocatedAmountMinor: Long,
    val displayOrder: Int,
    val isArchived: Boolean,
)

data class TransactionJson(
    val id: Long,
    val title: String,
    val note: String?,
    val amountMinor: Long,
    val paidDate: String,
    val paidAtEpochMillis: Long,
    val categoryId: Long?,
    val splitCount: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class ImpactJson(
    val id: Long,
    val transactionId: Long,
    val budgetPeriodId: Long?,
    val categoryId: Long?,
    val sourceBudgetPeriodId: Long?,
    val categoryNameSnapshot: String,
    val amountMinor: Long,
    val impactDate: String,
    val plannedPeriodOffset: Int,
    val pendingPeriodStartDate: String?,
    val status: String,
)

// --- Entity ↔ JSON mappers ---

fun BudgetPeriodEntity.toJson() = PeriodJson(
    id = id,
    name = name,
    startDate = startDate.toString(),
    endDate = endDate.toString(),
    totalAmountMinor = totalAmountMinor,
    currencyCode = currencyCode,
    isActive = isActive,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

fun PeriodJson.toEntity() = BudgetPeriodEntity(
    id = id,
    name = name,
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    totalAmountMinor = totalAmountMinor,
    currencyCode = currencyCode,
    isActive = isActive,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

fun BudgetCategoryEntity.toJson() = CategoryJson(
    id = id,
    budgetPeriodId = budgetPeriodId,
    name = name,
    allocatedAmountMinor = allocatedAmountMinor,
    displayOrder = displayOrder,
    isArchived = isArchived,
)

fun CategoryJson.toEntity() = BudgetCategoryEntity(
    id = id,
    budgetPeriodId = budgetPeriodId,
    name = name,
    allocatedAmountMinor = allocatedAmountMinor,
    displayOrder = displayOrder,
    isArchived = isArchived,
)

fun TransactionEntity.toJson() = TransactionJson(
    id = id,
    title = title,
    note = note,
    amountMinor = amountMinor,
    paidDate = paidDate.toString(),
    paidAtEpochMillis = paidAtEpochMillis,
    categoryId = categoryId,
    splitCount = splitCount,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

fun TransactionJson.toEntity() = TransactionEntity(
    id = id,
    title = title,
    note = note,
    amountMinor = amountMinor,
    paidDate = LocalDate.parse(paidDate),
    paidAtEpochMillis = paidAtEpochMillis,
    categoryId = categoryId,
    splitCount = splitCount,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

fun BudgetImpactEntity.toJson() = ImpactJson(
    id = id,
    transactionId = transactionId,
    budgetPeriodId = budgetPeriodId,
    categoryId = categoryId,
    sourceBudgetPeriodId = sourceBudgetPeriodId,
    categoryNameSnapshot = categoryNameSnapshot,
    amountMinor = amountMinor,
    impactDate = impactDate.toString(),
    plannedPeriodOffset = plannedPeriodOffset,
    pendingPeriodStartDate = pendingPeriodStartDate?.toString(),
    status = status,
)

fun ImpactJson.toEntity() = BudgetImpactEntity(
    id = id,
    transactionId = transactionId,
    budgetPeriodId = budgetPeriodId,
    categoryId = categoryId,
    sourceBudgetPeriodId = sourceBudgetPeriodId,
    categoryNameSnapshot = categoryNameSnapshot,
    amountMinor = amountMinor,
    impactDate = LocalDate.parse(impactDate),
    plannedPeriodOffset = plannedPeriodOffset,
    pendingPeriodStartDate = pendingPeriodStartDate?.let(LocalDate::parse),
    status = status,
)
