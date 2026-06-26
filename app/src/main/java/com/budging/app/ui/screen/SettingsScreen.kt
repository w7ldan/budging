package com.budging.app.ui.screen

import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import android.widget.Toast
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.budging.app.R
import com.budging.app.quickaccess.QuickAccessUpdater
import com.budging.app.quicktile.LogExpenseTileService
import com.budging.app.ui.component.BudgetScaffoldCard
import com.budging.app.ui.component.SectionHeader
import com.budging.app.ui.theme.BudgingTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val spacing = BudgingTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
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
                                Toast.makeText(context, "Status bar service unavailable on this device.", Toast.LENGTH_SHORT).show()
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
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) {
                                Toast.makeText(context, "This device does not support adding tiles from the app. Open Quick Settings edit mode, find Log Expense under apps, then drag it into active tiles.", Toast.LENGTH_LONG).show()
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
                            Toast.makeText(context, "Open Quick Settings edit mode, find Log Expense under apps, then drag it into active tiles.", Toast.LENGTH_LONG).show()
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
                            Toast.makeText(context, "Widget refreshed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Refresh Widget")
                }
            }
        }
    }
}
