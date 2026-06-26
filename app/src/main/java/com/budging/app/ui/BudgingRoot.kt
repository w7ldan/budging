package com.budging.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budging.app.ui.screen.BudgetSetupScreen
import com.budging.app.ui.screen.CategoryDetailScreen
import com.budging.app.ui.screen.DashboardScreen
import com.budging.app.ui.screen.LogExpenseScreen
import com.budging.app.ui.screen.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgingRoot(viewModel: BudgingViewModel) {
    val navController = rememberNavController()
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val budgetSetupState by viewModel.budgetSetupState.collectAsStateWithLifecycle()
    val expenseEntryState by viewModel.expenseEntryState.collectAsStateWithLifecycle()
    val categoryDetailState by viewModel.categoryDetailState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (destination?.route) {
                            Screen.BudgetSetup.route -> "Set Budget"
                            Screen.LogExpense.route -> "Log Expense"
                            Screen.CategoryDetail.route -> "Category Detail"
                            Screen.Settings.route -> "Settings"
                            else -> "Budging"
                        },
                    )
                },
            )
        },
        floatingActionButton = {
            if (destination?.route == Screen.Dashboard.route) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.LogExpense.route) },
                ) {
                    Text("+")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { item ->
                    NavigationBarItem(
                        selected = destination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(item.label) },
                        icon = {},
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    state = dashboardState,
                    onOpenCategory = { categoryId ->
                        viewModel.loadCategory(categoryId)
                        navController.navigate(Screen.CategoryDetail.route)
                    },
                )
            }
            composable(Screen.BudgetSetup.route) {
                BudgetSetupScreen(
                    state = budgetSetupState,
                    onSaveBudget = { name, totalAmountMinor, currencyCode, startDateText, endDateText ->
                        viewModel.saveBudgetPeriod(
                            name = name,
                            totalAmountMinor = totalAmountMinor,
                            currencyCode = currencyCode,
                            startDateText = startDateText,
                            endDateText = endDateText,
                        )
                    },
                    onSaveCategory = viewModel::saveCategory,
                    onArchiveCategory = viewModel::setCategoryArchived,
                    onDeleteCategory = viewModel::deleteCategory,
                )
            }
            composable(Screen.LogExpense.route) {
                LogExpenseScreen(
                    state = expenseEntryState,
                    onSaveExpense = { amountMinor, categoryId, dateText, note ->
                        viewModel.logExpense(
                            amountMinor = amountMinor,
                            categoryId = categoryId,
                            note = note,
                            dateText = dateText,
                        )
                    },
                )
            }
            composable(Screen.CategoryDetail.route) {
                CategoryDetailScreen(
                    state = categoryDetailState,
                    onDeleteTransaction = viewModel::deleteTransaction,
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

private val topLevelDestinations = listOf(
    Screen.Dashboard,
    Screen.BudgetSetup,
    Screen.LogExpense,
    Screen.Settings,
)
