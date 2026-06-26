package com.budging.app.data

import android.content.Context
import androidx.room.Room
import com.budging.app.data.local.BudgingDatabase
import com.budging.app.data.backup.BackupRepository
import com.budging.app.data.repo.BudgetPeriodRepository
import com.budging.app.data.repo.CategoryRepository
import com.budging.app.data.repo.DashboardRepository
import com.budging.app.data.repo.ExpenseRepository
import com.budging.app.data.repo.PendingImpactService
import com.budging.app.data.repo.RecurringRepository
import com.budging.app.data.repo.TransactionRepository
import com.budging.app.domain.AppClock

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val clock = AppClock.System

    private val database = Room.databaseBuilder(
        context,
        BudgingDatabase::class.java,
        BudgingDatabase.NAME,
    ).addMigrations(
        BudgingDatabase.migration1To2,
        BudgingDatabase.migration2To3,
        BudgingDatabase.migration3To4,
        BudgingDatabase.migration4To5,
        BudgingDatabase.migration5To6,
    ).build()

    val expenseRepository = ExpenseRepository(
        appContext = appContext,
        database = database,
        budgetPeriodDao = database.budgetPeriodDao(),
        budgetCategoryDao = database.budgetCategoryDao(),
        transactionDao = database.transactionDao(),
        budgetImpactDao = database.budgetImpactDao(),
        clock = clock,
    )

    val pendingImpactService = PendingImpactService(
        appContext = appContext,
        budgetImpactDao = database.budgetImpactDao(),
        budgetPeriodDao = database.budgetPeriodDao(),
        budgetCategoryDao = database.budgetCategoryDao(),
        transactionDao = database.transactionDao(),
    )

    val recurringRepository = RecurringRepository(
        appContext = appContext,
        recurringTemplateDao = database.recurringExpenseTemplateDao(),
        budgetCategoryDao = database.budgetCategoryDao(),
        transactionDao = database.transactionDao(),
        expenseRepository = expenseRepository,
        clock = clock,
    )

    val budgetPeriodRepository = BudgetPeriodRepository(
        appContext = appContext,
        database = database,
        budgetPeriodDao = database.budgetPeriodDao(),
        budgetCategoryDao = database.budgetCategoryDao(),
        budgetImpactDao = database.budgetImpactDao(),
        transactionDao = database.transactionDao(),
        pendingImpactService = pendingImpactService,
        recurringRepository = recurringRepository,
        clock = clock,
    )

    val categoryRepository = CategoryRepository(
        appContext = appContext,
        budgetPeriodDao = database.budgetPeriodDao(),
        budgetCategoryDao = database.budgetCategoryDao(),
        budgetImpactDao = database.budgetImpactDao(),
    )

    val transactionRepository = TransactionRepository(
        appContext = appContext,
        database = database,
        transactionDao = database.transactionDao(),
        budgetImpactDao = database.budgetImpactDao(),
        budgetPeriodDao = database.budgetPeriodDao(),
        budgetCategoryDao = database.budgetCategoryDao(),
        clock = clock,
    )

    val dashboardRepository = DashboardRepository(
        budgetPeriodDao = database.budgetPeriodDao(),
        budgetCategoryDao = database.budgetCategoryDao(),
        budgetImpactDao = database.budgetImpactDao(),
        transactionDao = database.transactionDao(),
        clock = clock,
    )

    val backupRepository = BackupRepository(
        appContext = appContext,
        database = database,
    )
}
