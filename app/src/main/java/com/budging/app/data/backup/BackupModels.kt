package com.budging.app.data.backup

import com.budging.app.data.local.entity.BudgetCategoryEntity
import com.budging.app.data.local.entity.BudgetImpactEntity
import com.budging.app.data.local.entity.BudgetPeriodEntity
import com.budging.app.data.local.entity.RecurringExpenseTemplateEntity
import com.budging.app.data.local.entity.TransactionEntity
import java.time.LocalDate

const val CURRENT_SCHEMA_VERSION = 2

data class BackupJson(
    val schemaVersion: Int,
    val appName: String,
    val exportedAt: String,
    val budgetPeriods: List<PeriodJson>,
    val budgetCategories: List<CategoryJson>,
    val transactions: List<TransactionJson>,
    val budgetImpacts: List<ImpactJson>,
    val recurringExpenseTemplates: List<RecurringTemplateJson> = emptyList(),
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
    val iconKey: String = "other",
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
    val sourceType: String = "MANUAL",
    val recurringTemplateId: Long? = null,
    val sourceOccurrenceDate: String? = null,
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

data class RecurringTemplateJson(
    val id: Long,
    val title: String,
    val amountMinor: Long,
    val currencyCode: String,
    val categoryNameSnapshot: String,
    val iconKey: String? = null,
    val note: String?,
    val frequency: String,
    val startDate: String,
    val endDate: String?,
    val dayOfMonth: Int?,
    val applyMode: String,
    val isActive: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
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
    iconKey = iconKey,
    allocatedAmountMinor = allocatedAmountMinor,
    displayOrder = displayOrder,
    isArchived = isArchived,
)

fun CategoryJson.toEntity() = BudgetCategoryEntity(
    id = id,
    budgetPeriodId = budgetPeriodId,
    name = name,
    iconKey = iconKey,
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
    sourceType = sourceType,
    recurringTemplateId = recurringTemplateId,
    sourceOccurrenceDate = sourceOccurrenceDate?.toString(),
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
    sourceType = sourceType,
    recurringTemplateId = recurringTemplateId,
    sourceOccurrenceDate = sourceOccurrenceDate?.let(LocalDate::parse),
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

fun RecurringExpenseTemplateEntity.toJson() = RecurringTemplateJson(
    id = id,
    title = title,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    categoryNameSnapshot = categoryNameSnapshot,
    iconKey = iconKey,
    note = note,
    frequency = frequency,
    startDate = startDate.toString(),
    endDate = endDate?.toString(),
    dayOfMonth = dayOfMonth,
    applyMode = applyMode,
    isActive = isActive,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

fun RecurringTemplateJson.toEntity() = RecurringExpenseTemplateEntity(
    id = id,
    title = title,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    categoryNameSnapshot = categoryNameSnapshot,
    iconKey = iconKey,
    note = note,
    frequency = frequency,
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let(LocalDate::parse),
    dayOfMonth = dayOfMonth,
    applyMode = applyMode,
    isActive = isActive,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)
