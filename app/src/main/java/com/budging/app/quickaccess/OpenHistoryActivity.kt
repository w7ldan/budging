package com.budging.app.quickaccess

import android.app.Activity
import android.os.Bundle

class OpenHistoryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(QuickAccessNavigation.historyIntent(this))
        finish()
    }
}
