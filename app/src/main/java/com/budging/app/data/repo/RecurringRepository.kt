package com.budging.app.data.repo

import android.content.Context
import com.budging.app.data.local.dao.BudgetCategoryDao
import com.budging.app.data.local.dao.RecurringExpenseTemplateDao
import com.budging.app.data.local.dao.TransactionDao
import com.budging.app.data.local.entity.RecurringExpenseTemplateEntity
import com.budging.app.data.model.BudgetCategoryItem
import com.budging.app.data.model.PendingMatchStatus
import com.budging.app.data.model.RecurringPreviewItem
import com.budging.app.data.model.RecurringTemplateDraft
import com.budging.app.data.model.RecurringTemplateItem
import com.budging.app.domain.AppClock
import com.budging.app.domain.RECURRING_APPLY_MODE_CONFIRM
import com.budging.app.domain.RecurringExpensePlanner
import com.budging.app.domain.RecurringFrequency
import com.budging.app.domain.TransactionSourceType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringRepository(
    private val appContext: Context,
    private val recurringTemplateDao: RecurringExpenseTemplateDao,
    private val budgetCategoryDao: BudgetCategoryDao,
    private val transactionDao: TransactionDao,
    private val expenseRepository: ExpenseRepository,
    private val clock: AppClock,
) {
    fun observeRecurringTemplates(): Flow<List<RecurringTemplateItem>> =
        recurringTemplateDao.observeAll().flatMapLatest { templates ->
            flowOf(
                templates.map {
                    RecurringTemplateItem(
                        id = it.id,
                        title = it.title,
                        amountMinor = it.amountMinor,
                        currencyCode = it.currencyCode,
                        categoryNameSnapshot = it.categoryNameSnapshot,
                        iconKey = it.iconKey,
                        note = it.note,
                        frequency = it.frequency,
                        startDate = it.startDate,
                        endDate = it.endDate,
                        dayOfMonth = it.dayOfMonth,
                        isActive = it.isActive,
                    )
                },
            )
        }

    suspend fun saveRecurringTemplate(draft: RecurringTemplateDraft) {
        require(draft.title.isNotBlank()) { "Subscription name is required." }
        require(draft.amountMinor > 0) { "Subscription amount must be positive." }
        require(draft.categoryNameSnapshot.isNotBlank()) { "Default category is required." }
        require(RecurringFrequency.fromDbValue(draft.frequency) in RecurringFrequency.entries) {
            "Choose a valid recurring frequency."
        }
        if (draft.frequency == RecurringFrequency.MONTHLY.dbValue) {
            require((draft.dayOfMonth ?: 0) in 1..31) { "Monthly day must be between 1 and 31." }
        }
        draft.endDate?.let { require(!it.isBefore(draft.startDate)) { "End date must be on or after start date." } }

        val existing = draft.templateId?.let { recurringTemplateDao.getById(it) }
        val now = clock.nowMillis()
        recurringTemplateDao.upsert(
            RecurringExpenseTemplateEntity(
                id = existing?.id ?: 0,
                title = draft.title.trim(),
                amountMinor = draft.amountMinor,
                currencyCode = draft.currencyCode.trim().uppercase().ifBlank { "IDR" },
                categoryNameSnapshot = draft.categoryNameSnapshot.trim(),
                iconKey = draft.iconKey,
                note = draft.note?.trim()?.ifBlank { null },
                frequency = draft.frequency,
                startDate = draft.startDate,
                endDate = draft.endDate,
                dayOfMonth = draft.dayOfMonth,
                applyMode = RECURRING_APPLY_MODE_CONFIRM,
                isActive = draft.isActive,
                createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                updatedAtEpochMillis = now,
            ),
        )
        refreshQuickAccess(appContext)
    }

    suspend fun deleteRecurringTemplate(templateId: Long) {
        recurringTemplateDao.deleteById(templateId)
        refreshQuickAccess(appContext)
    }

    suspend fun previewRecurringForPeriod(
        startDate: LocalDate,
        endDate: LocalDate,
        targetCategories: List<BudgetCategoryItem>,
        currencyCode: String,
    ): List<RecurringPreviewItem> {
        val categoriesByName = targetCategories.groupBy { it.name.lowercase() }
        return recurringTemplateDao.getAll()
            .filter { it.isActive }
            .flatMap { template ->
                RecurringExpensePlanner.occurrencesForPeriod(template, startDate, endDate).map { occurrence ->
                    val matches = categoriesByName[template.categoryNameSnapshot.lowercase()].orEmpty()
                    val matchStatus = when {
                        matches.isEmpty() -> PendingMatchStatus.NO_MATCH
                        matches.size == 1 -> PendingMatchStatus.MATCHED
                        else -> PendingMatchStatus.AMBIGUOUS
                    }
                    RecurringPreviewItem(
                        previewKey = "${template.id}:${occurrence.occurrenceDate}",
                        templateId = template.id,
                        title = template.title,
                        amountMinor = template.amountMinor,
                        currencyCode = currencyCode,
                        categoryNameSnapshot = template.categoryNameSnapshot,
                        iconKey = template.iconKey,
                        occurrenceDate = occurrence.occurrenceDate,
                        matchStatus = matchStatus,
                        matchingCategoryId = matches.singleOrNull()?.id,
                        matchingCategoryName = matches.singleOrNull()?.name,
                    )
                }
            }
            .sortedWith(compareBy({ it.occurrenceDate }, { it.title.lowercase() }))
    }

    suspend fun applyRecurringTemplatesForPeriod(
        newPeriodId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
        currencyCode: String,
        targetCategoryIds: Set<Long>,
        applyRecurringPreviewKeys: Set<String>,
        translatedRecurringMapping: Map<String, Long>,
    ) {
        if (applyRecurringPreviewKeys.isEmpty()) return
        val targetCategories = budgetCategoryDao.getAll()
            .filter { it.budgetPeriodId == newPeriodId && it.id in targetCategoryIds }
        val previews = previewRecurringForPeriod(
            startDate = startDate,
            endDate = endDate,
            targetCategories = targetCategories.map {
                BudgetCategoryItem(it.id, it.name, it.iconKey, it.allocatedAmountMinor, 0L, it.isArchived, false)
            },
            currencyCode = currencyCode,
        ).associateBy { it.previewKey }

        applyRecurringPreviewKeys.forEach { previewKey ->
            val preview = previews[previewKey] ?: return@forEach
            val template = recurringTemplateDao.getById(preview.templateId) ?: return@forEach
            val categoryId = translatedRecurringMapping[preview.previewKey]
                ?: preview.matchingCategoryId
                ?: return@forEach
            if (transactionDao.findRecurringTransactionId(template.id, preview.occurrenceDate) != null) return@forEach
            val category = budgetCategoryDao.getById(categoryId) ?: return@forEach
            expenseRepository.insertAppliedTransaction(
                title = template.title,
                note = template.note.orEmpty(),
                amountMinor = template.amountMinor,
                paidDate = preview.occurrenceDate,
                paidAtEpochMillis = preview.occurrenceDate.atStartOfDay(clock.zoneId()).toInstant().toEpochMilli(),
                category = category,
                budgetPeriodId = newPeriodId,
                sourceType = TransactionSourceType.RECURRING.dbValue,
                recurringTemplateId = template.id,
                sourceOccurrenceDate = preview.occurrenceDate,
            )
        }
    }
}
