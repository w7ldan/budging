package com.budging.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.budging.app.data.local.dao.BudgetCategoryDao
import com.budging.app.data.local.dao.BudgetImpactDao
import com.budging.app.data.local.dao.BudgetPeriodDao
import com.budging.app.data.local.dao.RecurringExpenseTemplateDao
import com.budging.app.data.local.dao.TransactionDao
import com.budging.app.data.local.entity.BudgetCategoryEntity
import com.budging.app.data.local.entity.BudgetImpactEntity
import com.budging.app.data.local.entity.BudgetPeriodEntity
import com.budging.app.data.local.entity.RecurringExpenseTemplateEntity
import com.budging.app.data.local.entity.TransactionEntity
import java.time.LocalDate

@Database(
    entities = [
        BudgetPeriodEntity::class,
        BudgetCategoryEntity::class,
        TransactionEntity::class,
        BudgetImpactEntity::class,
        RecurringExpenseTemplateEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
@TypeConverters(RoomTypeConverters::class)
abstract class BudgingDatabase : RoomDatabase() {
    abstract fun budgetPeriodDao(): BudgetPeriodDao
    abstract fun budgetCategoryDao(): BudgetCategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetImpactDao(): BudgetImpactDao
    abstract fun recurringExpenseTemplateDao(): RecurringExpenseTemplateDao

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

        val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    DELETE FROM budget_impacts
                    WHERE transaction_id IN (
                        SELECT id FROM transactions
                        WHERE title IN ('Groceries', 'Gym Membership')
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    DELETE FROM transactions
                    WHERE title IN ('Groceries', 'Gym Membership')
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    DELETE FROM budget_categories
                    WHERE budget_period_id IN (
                        SELECT id FROM budget_periods WHERE name = 'Main Budget'
                    )
                    """.trimIndent(),
                )
                db.execSQL("DELETE FROM budget_periods WHERE name = 'Main Budget'")
            }
        }

        val migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budget_impacts ADD COLUMN source_budget_period_id INTEGER")
                db.execSQL("ALTER TABLE budget_impacts ADD COLUMN category_name_snapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE budget_impacts ADD COLUMN planned_period_offset INTEGER NOT NULL DEFAULT 0")
            }
        }

        val migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budget_periods ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1")
                // ponytail: keep only the latest period active after migration;
                // if a user had multiple periods before v5, DEFAULT 1 would make all active.
                db.execSQL(
                    """
                    UPDATE budget_periods SET is_active = 0
                    WHERE is_active = 1
                      AND id != (
                          SELECT id FROM budget_periods
                          WHERE is_active = 1
                          ORDER BY start_date_epoch DESC, updated_at_epoch_millis DESC
                          LIMIT 1
                      )
                    """.trimIndent(),
                )
            }
        }

        val migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budget_categories ADD COLUMN icon_key TEXT NOT NULL DEFAULT 'other'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN source_type TEXT NOT NULL DEFAULT 'MANUAL'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN recurring_template_id INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN source_occurrence_date_epoch INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_recurring_template_id ON transactions(recurring_template_id)")
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_transactions_recurring_template_id_source_occurrence_date_epoch
                    ON transactions(recurring_template_id, source_occurrence_date_epoch)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recurring_expense_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        amount_minor INTEGER NOT NULL,
                        currency_code TEXT NOT NULL,
                        category_name_snapshot TEXT NOT NULL,
                        icon_key TEXT,
                        note TEXT,
                        frequency TEXT NOT NULL,
                        start_date_epoch INTEGER NOT NULL,
                        end_date_epoch INTEGER,
                        day_of_month INTEGER,
                        apply_mode TEXT NOT NULL DEFAULT 'CONFIRM',
                        is_active INTEGER NOT NULL DEFAULT 1,
                        created_at_epoch_millis INTEGER NOT NULL,
                        updated_at_epoch_millis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
