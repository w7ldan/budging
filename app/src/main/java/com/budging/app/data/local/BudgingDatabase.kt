package com.budging.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 1,
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

        fun seedCallback(): Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                val today = LocalDate.now()
                val start = today.minusDays(3).toEpochDay()
                val end = today.plusDays(10).toEpochDay()
                val nextPeriodStart = today.plusDays(11).toEpochDay()
                val now = System.currentTimeMillis()

                db.execSQL(
                    """
                    INSERT INTO budget_periods(id, name, start_date_epoch, end_date_epoch, total_amount_minor, created_at_epoch_millis, updated_at_epoch_millis)
                    VALUES (1, 'Main Budget', $start, $end, 2450000, $now, $now)
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    INSERT INTO budget_categories(id, budget_period_id, name, allocated_amount_minor, display_order)
                    VALUES
                    (1, 1, 'Food', 900000, 0),
                    (2, 1, 'Transport', 350000, 1),
                    (3, 1, 'Gym', 300000, 2),
                    (4, 1, 'Fun', 400000, 3)
                    """.trimIndent(),
                )

                val tx1Date = today.minusDays(1).toEpochDay()
                val tx2Date = today.toEpochDay()
                db.execSQL(
                    """
                    INSERT INTO transactions(id, title, note, amount_minor, paid_date_epoch, category_id, split_count, created_at_epoch_millis, updated_at_epoch_millis)
                    VALUES
                    (1, 'Groceries', 'Weekly restock', 180000, $tx1Date, 1, 1, $now, $now),
                    (2, 'Gym Membership', '3-period even split', 900000, $tx2Date, 3, 3, $now, $now)
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    INSERT INTO budget_impacts(id, transaction_id, budget_period_id, category_id, amount_minor, impact_date_epoch, pending_period_start_epoch, status)
                    VALUES
                    (1, 1, 1, 1, 180000, $tx1Date, NULL, 'APPLIED'),
                    (2, 2, 1, 3, 300000, $tx2Date, NULL, 'APPLIED'),
                    (3, 2, NULL, 3, 300000, $tx2Date, $nextPeriodStart, 'PENDING')
                    """.trimIndent(),
                )
            }
        }
    }
}
