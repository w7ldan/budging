package com.budging.app.ui.screen

import android.content.ComponentName
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.service.quicksettings.TileService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.budging.app.R
import com.budging.app.quickaccess.QuickAccessUpdater
import com.budging.app.quicktile.LogExpenseTileService
import com.budging.app.ui.BackupMessage
import com.budging.app.ui.component.BudgetConfirmDialog
import com.budging.app.ui.component.BudgetNoticeDialog
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.theme.BudgingTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    backupMessage: BackupMessage?,
    onExportJson: (Uri) -> Unit,
    onImportJson: (Uri) -> Unit,
    onExportCsv: (Uri) -> Unit,
    onClearBackupMessage: () -> Unit,
    onOpenPeriodList: () -> Unit = {},
    onOpenSubscriptions: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val spacing = BudgingTheme.spacing
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var noticeText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(backupMessage) {
        backupMessage?.let { msg ->
            noticeText = when (msg) {
                is BackupMessage.Success -> msg.text
                is BackupMessage.Error -> msg.text
            }
            onClearBackupMessage()
        }
    }

    val createJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { onExportJson(it) } }

    val openJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let { onExportCsv(it) } }

    noticeText?.let { text ->
        BudgetNoticeDialog(
            title = "Budging",
            body = text,
            onDismiss = { noticeText = null },
        )
    }

    pendingImportUri?.let { uri ->
        BudgetConfirmDialog(
            title = "Restore Backup",
            body = "This will replace all current local data with data from the backup file. This cannot be undone.",
            confirmLabel = "Replace Data",
            destructive = true,
            onDismiss = { pendingImportUri = null },
            onConfirm = {
                onImportJson(uri)
                pendingImportUri = null
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        // --- Periods section ---
        SectionHeader(title = "Budget Periods")

        BudgetScaffoldCard {
            OutlinedButton(
                onClick = onOpenPeriodList,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("View All Budget Periods")
            }
            OutlinedButton(
                onClick = onOpenSubscriptions,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Manage Subscriptions")
            }
        }

        // --- Quick Access section (existing) ---
        SectionHeader(eyebrow = "Extras", title = "Quick Access")

        BudgetScaffoldCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Text(
                    "Keep Budging one tap away. Add a Quick Settings tile for instant access to Log Expense, or refresh the home-screen widget after a budget change.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Button(
                        onClick = {
                            val manager = context.getSystemService(android.app.StatusBarManager::class.java)
                            if (manager == null) {
                                noticeText = "Status bar service unavailable on this device."
                                return@Button
                            }
                            try {
                                val icon = Icon.createWithResource(context, R.drawable.ic_tile_log_expense)
                                manager.requestAddTileService(
                                    ComponentName(context, LogExpenseTileService::class.java),
                                    "Log Expense",
                                    icon,
                                    context.mainExecutor,
                                ) { resultCode ->
                                    val msg = when (resultCode) {
                                        android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                                            "Tile added — find Log Expense in Quick Settings"
                                        android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                                            "Tile already added"
                                        android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                                            "Tile add was denied"
                                        else -> "Could not add tile (result: $resultCode)"
                                    }
                                    noticeText = msg
                                }
                            } catch (_: Exception) {
                                noticeText = "This device does not support adding tiles from the app. Open Quick Settings edit mode, find Log Expense under apps, then drag it into active tiles."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Add Quick Settings Tile")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            try {
                                TileService.requestListeningState(context, ComponentName(context, LogExpenseTileService::class.java))
                            } catch (_: Exception) { /* best effort */ }
                            noticeText = "Open Quick Settings edit mode, find Log Expense under apps, then drag it into active tiles."
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Add Quick Settings Tile")
                    }
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            QuickAccessUpdater.refresh(context)
                            noticeText = "Widget refreshed"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Refresh Widget")
                }
            }
        }

        // --- Backup & Restore section ---
        SectionHeader(eyebrow = "Data", title = "Backup & Restore")

        BudgetScaffoldCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Text(
                    "Files stay on this device unless you choose to store or share them elsewhere.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = { createJsonLauncher.launch("budging_backup.json") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("Export JSON Backup")
                }

                OutlinedButton(
                    onClick = { openJsonLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Import JSON Backup")
                }

                OutlinedButton(
                    onClick = { createCsvLauncher.launch("budging_transactions.csv") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Export CSV")
                }
            }
        }
    }
}
