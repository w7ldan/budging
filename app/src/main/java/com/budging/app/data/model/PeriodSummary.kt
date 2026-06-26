package com.budging.app.data.model

import java.time.LocalDate

data class PeriodSummary(
    val id: Long,
    val name: String,
    val dateRangeLabel: String,
    val totalAmountMinor: Long,
    val spentAmountMinor: Long,
    val remainingAmountMinor: Long,
    val currencyCode: String,
    val isActive: Boolean,
    val categoryCount: Int,
)

data class PendingImpactDetail(
    val impactId: Long,
    val transactionId: Long,
    val transactionTitle: String,
    val amountMinor: Long,
    val categoryNameSnapshot: String,
    val plannedPeriodOffset: Int,
    val sourcePeriodName: String?,
    val matchingCategoryId: Long?,
    val matchingCategoryName: String?,
    val matchStatus: PendingMatchStatus,
)

enum class PendingMatchStatus {
    MATCHED,
    NO_MATCH,
    AMBIGUOUS,
}

data class RecurringTemplateItem(
    val id: Long,
    val title: String,
    val amountMinor: Long,
    val currencyCode: String,
    val categoryNameSnapshot: String,
    val iconKey: String?,
    val note: String?,
    val frequency: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val dayOfMonth: Int?,
    val isActive: Boolean,
)

data class RecurringTemplateDraft(
    val templateId: Long? = null,
    val title: String,
    val amountMinor: Long,
    val currencyCode: String,
    val categoryNameSnapshot: String,
    val iconKey: String?,
    val note: String?,
    val frequency: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val dayOfMonth: Int?,
    val isActive: Boolean,
)

data class RecurringPreviewItem(
    val previewKey: String,
    val templateId: Long,
    val title: String,
    val amountMinor: Long,
    val currencyCode: String,
    val categoryNameSnapshot: String,
    val iconKey: String?,
    val occurrenceDate: LocalDate,
    val matchStatus: PendingMatchStatus,
    val matchingCategoryId: Long?,
    val matchingCategoryName: String?,
)
