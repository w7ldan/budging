package com.budging.app.quicktile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.budging.app.BudgingApp
import com.budging.app.quickaccess.QuickAccessNavigation
import com.budging.app.ui.format.formatCurrency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogExpenseTileService : TileService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onTileAdded() {
        super.onTileAdded()
        serviceScope.launch { updateTileState() }
    }

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch { updateTileState() }
    }

    override fun onClick() {
        super.onClick()
        val intent = QuickAccessNavigation.openLogExpenseActivityIntent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @SuppressLint("StartActivityAndCollapseDeprecated")
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun updateTileState() {
        val repository = (application as BudgingApp).container.dashboardRepository
        val snapshot = repository.getDashboardSnapshot()
        withContext(Dispatchers.Main) {
            qsTile?.apply {
                state = Tile.STATE_ACTIVE
                label = "Log Expense"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    subtitle = if (snapshot.hasActiveBudget) {
                        "${formatCurrency(snapshot.totalRemainingMinor, snapshot.currencyCode)} left"
                    } else {
                        "Budging"
                    }
                }
                updateTile()
            }
        }
    }
}
