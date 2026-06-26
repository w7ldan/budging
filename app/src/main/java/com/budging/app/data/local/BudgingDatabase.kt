package com.budging.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.budging.app.data.local.dao.BudgetCategoryDao
import com.budging.app.data.local.dao.BudgetImpactDao
import com.budging.app.data.local.dao.BudgetPeriodDao
import com.budging.app.data.local.dao.TransactionDao
import com.budging.app.data.local.entity.BudgetCategoryEntity
import com.budging.app.data.local.entity.BudgetImpactEntity
import com.budging.app.data.local.entity.BudgetPeriodEntity
import com.budging.app.data.local.entity.TransactionEntity
import java.time.LocalDate

@Database(
    entities = [
        BudgetPeriodEntity::class,
        BudgetCategoryEntity::class,
        TransactionEntity::class,
        BudgetImpactEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(RoomTypeConverters::class)
abstract class BudgingDatabase : RoomDatabase() {
    abstract fun budgetPeriodDao(): BudgetPeriodDao
    abstract fun budgetCategoryDao(): BudgetCategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetImpactDao(): BudgetImpactDao

    companion object {
        const val NAME = "budging.db"

        val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budget_periods ADD COLUMN currency_code TEXT NOT NULL DEFAULT 'IDR'")
                db.execSQL("ALTER TABLE budget_categories ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN paid_at_epoch_millis INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    UPDATE transactions
                    SET paid_at_epoch_millis = created_at_epoch_millis
                    WHERE paid_at_epoch_millis = 0
                    """.trimIndent(),
                )
            }
        }
    }
}
