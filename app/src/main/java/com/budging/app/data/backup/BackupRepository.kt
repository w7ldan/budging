package com.budging.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.budging.app.data.local.BudgingDatabase
import com.budging.app.quickaccess.QuickAccessUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant

class BackupRepository(
    private val appContext: Context,
    private val database: BudgingDatabase,
) {
    private val periodDao get() = database.budgetPeriodDao()
    private val categoryDao get() = database.budgetCategoryDao()
    private val transactionDao get() = database.transactionDao()
    private val impactDao get() = database.budgetImpactDao()

    suspend fun exportJson(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val backup = BackupJson(
                schemaVersion = CURRENT_SCHEMA_VERSION,
                appName = "Budging",
                exportedAt = Instant.now().toString(),
                budgetPeriods = periodDao.getAll().map { it.toJson() },
                budgetCategories = categoryDao.getAll().map { it.toJson() },
                transactions = transactionDao.getAll().map { it.toJson() },
                budgetImpacts = impactDao.getAll().map { it.toJson() },
            )
            val json = BackupSerializer.serialize(backup)
            appContext.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Could not open output stream for export")
        }
    }

    suspend fun importJson(uri: URIResult): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = uri.read(appContext)
            val backup = BackupSerializer.deserialize(json)
            val validation = BackupSerializer.validate(backup)
            if (!validation.isValid) {
                throw IllegalStateException(validation.errors.joinToString("\n"))
            }

            val periods = backup.budgetPeriods.map { it.toEntity() }
            val categories = backup.budgetCategories.map { it.toEntity() }
            val transactions = backup.transactions.map { it.toEntity() }
            val impacts = backup.budgetImpacts.map { it.toEntity() }

            database.withTransaction {
                // Delete in FK-safe order
                impactDao.deleteAll()
                transactionDao.deleteAll()
                categoryDao.deleteAll()
                periodDao.deleteAll()

                // Insert in FK-safe order
                periods.forEach { periodDao.upsert(it) }
                categories.forEach { categoryDao.upsert(it) }
                transactionDao.insertAll(transactions)
                impactDao.insertAll(impacts)
            }

            QuickAccessUpdater.refresh(appContext)
        }
    }

    suspend fun exportCsv(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val csv = CsvExporter.export(
                periods = periodDao.getAll(),
                categories = categoryDao.getAll(),
                transactions = transactionDao.getAll(),
                impacts = impactDao.getAll(),
            )
            appContext.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(csv.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Could not open output stream for CSV export")
        }
    }
}

// ponytail: wraps a uri + mime so we can validate the content before passing to the import path;
// the file picker returns the uri but we need to know it's json.
class URIResult(val uri: Uri) {
    fun read(context: Context): String {
        context.contentResolver.openInputStream(uri)?.use { input ->
            return BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        } ?: throw IllegalStateException("Could not read file")
    }
}