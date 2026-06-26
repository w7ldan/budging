package com.budging.app.quickaccess

import android.content.Context
import android.content.Intent
import com.budging.app.MainActivity
import com.budging.app.ui.Screen

object QuickAccessNavigation {
    private const val EXTRA_ROUTE = "com.budging.app.extra.ROUTE"

    fun dashboardIntent(context: Context): Intent = baseIntent(context, Screen.Dashboard.route)

    fun logExpenseIntent(context: Context): Intent = baseIntent(context, Screen.LogExpense.route)

    fun openDashboardActivityIntent(context: Context): Intent =
        Intent(context, OpenDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

    fun openLogExpenseActivityIntent(context: Context): Intent =
        Intent(context, OpenLogExpenseActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

    fun routeFromIntent(intent: Intent?): String? = when (intent?.getStringExtra(EXTRA_ROUTE)) {
        Screen.Dashboard.route -> Screen.Dashboard.route
        Screen.LogExpense.route -> Screen.LogExpense.route
        else -> null
    }

    private fun baseIntent(context: Context, route: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ROUTE, route)
        }
}
