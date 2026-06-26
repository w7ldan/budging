package com.budging.app.ui

sealed class Screen(
    val route: String,
    val label: String,
) {
    data object Dashboard : Screen("dashboard", "Dashboard")
    data object BudgetSetup : Screen("budget_setup", "Budget")
    data object LogExpense : Screen("log_expense", "Log")
    data object CategoryDetail : Screen("category_detail", "Category")
    data object Settings : Screen("settings", "Settings")
}
