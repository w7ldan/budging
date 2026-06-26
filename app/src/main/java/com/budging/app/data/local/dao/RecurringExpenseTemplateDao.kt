package com.budging.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budging.app.data.local.entity.RecurringExpenseTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseTemplateDao {
    @Query("SELECT * FROM recurring_expense_templates ORDER BY is_active DESC, title COLLATE NOCASE, id DESC")
    fun observeAll(): Flow<List<RecurringExpenseTemplateEntity>>

    @Query("SELECT * FROM recurring_expense_templates ORDER BY id")
    suspend fun getAll(): List<RecurringExpenseTemplateEntity>

    @Query("SELECT * FROM recurring_expense_templates WHERE id = :templateId LIMIT 1")
    suspend fun getById(templateId: Long): RecurringExpenseTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: RecurringExpenseTemplateEntity): Long

    @Query("DELETE FROM recurring_expense_templates WHERE id = :templateId")
    suspend fun deleteById(templateId: Long)

    @Query("DELETE FROM recurring_expense_templates")
    suspend fun deleteAll()
}
