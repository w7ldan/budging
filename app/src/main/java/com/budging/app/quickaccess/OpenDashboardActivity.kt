package com.budging.app.quickaccess

import android.app.Activity
import android.os.Bundle

class OpenDashboardActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(QuickAccessNavigation.dashboardIntent(this))
        finish()
    }
}
