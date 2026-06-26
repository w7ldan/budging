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
    data object Subscriptions : Screen("subscriptions", "Subscriptions")
    data object TransactionHistory : Screen("transaction_history", "History")
    data object TransactionDetail : Screen("transaction_detail/{transactionId}", "Detail") {
        fun createRoute(transactionId: Long) = "transaction_detail/$transactionId"
    }
    data object EditTransaction : Screen("edit_transaction/{transactionId}", "Edit") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
    data object BudgetPeriodList : Screen("budget_periods", "Periods")
    data object CreateNextPeriod : Screen("create_next_period", "Create Next")
}
