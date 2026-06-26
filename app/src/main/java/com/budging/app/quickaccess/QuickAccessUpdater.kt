package com.budging.app.quickaccess

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import com.budging.app.widget.BudgingStatusWidget
import com.budging.app.quicktile.LogExpenseTileService
import androidx.glance.appwidget.updateAll

object QuickAccessUpdater {
    suspend fun refresh(context: Context) {
        BudgingStatusWidget().updateAll(context)
        TileService.requestListeningState(
            context,
            ComponentName(context, LogExpenseTileService::class.java),
        )
    }
}
