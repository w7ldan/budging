package com.budging.app.data

import android.content.Context
import androidx.room.Room
import com.budging.app.data.local.BudgingDatabase
import com.budging.app.data.repo.BudgetRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        BudgingDatabase::class.java,
        BudgingDatabase.NAME,
    ).addMigrations(
        BudgingDatabase.migration1To2,
        BudgingDatabase.migration2To3,
        BudgingDatabase.migration3To4,
    ).build()

    val budgetRepository = BudgetRepository(
        appContext = context.applicationContext,
        database = database,
        budgetPeriodDao = database.budgetPeriodDao(),
        budgetCategoryDao = database.budgetCategoryDao(),
        transactionDao = database.transactionDao(),
        budgetImpactDao = database.budgetImpactDao(),
    )
}
