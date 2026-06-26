package com.budging.app.data.repo

import android.content.Context
import com.budging.app.data.local.dao.BudgetCategoryDao
import com.budging.app.data.local.dao.BudgetImpactDao
import com.budging.app.data.local.dao.BudgetPeriodDao
import com.budging.app.data.local.entity.BudgetCategoryEntity

class CategoryRepository(
    private val appContext: Context,
    private val budgetPeriodDao: BudgetPeriodDao,
    private val budgetCategoryDao: BudgetCategoryDao,
    private val budgetImpactDao: BudgetImpactDao,
) {
    suspend fun saveCategory(
        categoryId: Long?,
        name: String,
        allocatedAmountMinor: Long,
        iconKey: String,
    ) {
        require(name.isNotBlank()) { "Category name is required." }
        require(allocatedAmountMinor > 0) { "Category allocation must be positive." }

        val activePeriod = budgetPeriodDao.getActive()
            ?: throw IllegalArgumentException("Create an active budget before adding categories.")
        val existing = categoryId?.let { budgetCategoryDao.getById(it) }
        val allocatedWithoutCurrent = budgetCategoryDao.getAllocatedSumForActive(activePeriod.id) -
            (existing?.takeIf { !it.isArchived }?.allocatedAmountMinor ?: 0L)
        require(allocatedWithoutCurrent + allocatedAmountMinor <= activePeriod.totalAmountMinor) {
            "Category allocations cannot exceed the total budget."
        }

        val category = BudgetCategoryEntity(
            id = existing?.id ?: 0,
            budgetPeriodId = activePeriod.id,
            name = name.trim(),
            iconKey = iconKey,
            allocatedAmountMinor = allocatedAmountMinor,
            displayOrder = existing?.displayOrder ?: (budgetCategoryDao.getMaxDisplayOrder(activePeriod.id) + 1),
            isArchived = existing?.isArchived ?: false,
        )
        budgetCategoryDao.upsert(category)
        refreshQuickAccess(appContext)
    }

    suspend fun setCategoryArchived(categoryId: Long, isArchived: Boolean) {
        budgetCategoryDao.setArchived(categoryId, isArchived)
        refreshQuickAccess(appContext)
    }

    suspend fun deleteCategory(categoryId: Long) {
        require(budgetImpactDao.countForCategory(categoryId) == 0) {
            "Category already has transactions. Archive it instead of deleting."
        }
        budgetCategoryDao.deleteById(categoryId)
        refreshQuickAccess(appContext)
    }
}
