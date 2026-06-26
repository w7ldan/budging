package com.budging.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
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
import com.budging.app.ui.component.BottomNavItemPill
import com.budging.app.ui.component.BudgetTopBar
import com.budging.app.ui.theme.BudgingTheme
import androidx.compose.material3.MaterialTheme

@Composable
fun BudgingRoot(
    viewModel: BudgingViewModel,
    externalRoute: String? = null,
    onExternalRouteConsumed: () -> Unit = {},
) {
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

    LaunchedEffect(externalRoute) {
        val route = externalRoute ?: return@LaunchedEffect
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        onExternalRouteConsumed()
    }

    Scaffold(
        topBar = {
            BudgetTopBar(
                title = when (destination?.route) {
                    Screen.BudgetSetup.route -> "Set Budget"
                    Screen.LogExpense.route -> "Log Expense"
                    Screen.CategoryDetail.route -> "Category Detail"
                    Screen.Settings.route -> "Overview"
                    else -> "Current Budget"
                },
                showBack = destination?.route == Screen.CategoryDetail.route,
                onBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
            )
        },
        floatingActionButton = {
            if (destination?.route == Screen.Dashboard.route) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.LogExpense.route) },
                    shape = RoundedCornerShape(999.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log expense")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = BudgingTheme.spacing.xl, vertical = BudgingTheme.spacing.md),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                topLevelDestinations.forEach { item ->
                    BottomNavItemPill(
                        screen = item,
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
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars),
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
                    previewCurrentImpact = viewModel::previewCurrentImpact,
                    onSaveExpense = { amountMinor, categoryId, dateText, note, splitPeriodCount ->
                        if (splitPeriodCount <= 1) {
                            viewModel.logNormalExpense(
                                amountMinor = amountMinor,
                                categoryId = categoryId,
                                note = note,
                                dateText = dateText,
                            )
                        } else {
                            viewModel.logSplitExpense(
                                amountMinor = amountMinor,
                                categoryId = categoryId,
                                note = note,
                                dateText = dateText,
                                periodCount = splitPeriodCount,
                            )
                        }
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
                SettingsScreen(
                    backupMessage = viewModel.backupMessage.collectAsStateWithLifecycle().value,
                    onExportJson = viewModel::exportJson,
                    onImportJson = viewModel::importJson,
                    onExportCsv = viewModel::exportCsv,
                    onClearBackupMessage = viewModel::clearBackupMessage,
                )
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
