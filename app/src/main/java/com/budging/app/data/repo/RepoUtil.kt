package com.budging.app.data.repo

import android.content.Context
import com.budging.app.quickaccess.QuickAccessUpdater

internal suspend fun refreshQuickAccess(context: Context) {
    QuickAccessUpdater.refresh(context)
}
