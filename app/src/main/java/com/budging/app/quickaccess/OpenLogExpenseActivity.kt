package com.budging.app.quickaccess

import android.app.Activity
import android.os.Bundle

class OpenLogExpenseActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(QuickAccessNavigation.logExpenseIntent(this))
        finish()
    }
}
