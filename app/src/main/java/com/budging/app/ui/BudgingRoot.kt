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
import com.budging.app.ui.screen.BudgetPeriodListScreen
import com.budging.app.ui.screen.BudgetSetupScreen
import com.budging.app.ui.screen.CategoryDetailScreen
import com.budging.app.ui.screen.CreateNextPeriodScreen
import com.budging.app.ui.screen.DashboardScreen
import com.budging.app.ui.screen.EditTransactionScreen
import com.budging.app.ui.screen.LogExpenseScreen
import com.budging.app.ui.screen.SettingsScreen
import com.budging.app.ui.screen.TransactionDetailScreen
import com.budging.app.ui.screen.TransactionHistoryScreen
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
    val transactionDetailState by viewModel.transactionDetailState.collectAsStateWithLifecycle()
    val transactionHistoryState by viewModel.transactionHistoryState.collectAsStateWithLifecycle()
    val periodListState by viewModel.periodListState.collectAsStateWithLifecycle()
    val pendingImpactsState by viewModel.pendingImpactsState.collectAsStateWithLifecycle()
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
                    Screen.TransactionHistory.route -> "Transaction History"
                    Screen.TransactionDetail.route -> "Transaction Detail"
                    Screen.EditTransaction.route -> "Edit Transaction"
                    Screen.BudgetPeriodList.route -> "Budget Periods"
                    Screen.CreateNextPeriod.route -> "Create Period"
                    else -> "Current Budget"
                },
                showBack = destination?.route in listOf(
                    Screen.CategoryDetail.route,
                    Screen.TransactionHistory.route,
                    Screen.TransactionDetail.route,
                    Screen.EditTransaction.route,
                    Screen.BudgetPeriodList.route,
                    Screen.CreateNextPeriod.route,
                ),
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
                    onViewAllTransactions = {
                        navController.navigate(Screen.TransactionHistory.route)
                    },
                    onTransactionClick = { transactionId ->
                        viewModel.loadTransaction(transactionId)
                        navController.navigate(Screen.TransactionDetail.createRoute(transactionId))
                    },
                    onCreateBudget = {
                        navController.navigate(Screen.CreateNextPeriod.route)
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
                    onTransactionClick = { transactionId ->
                        viewModel.loadTransaction(transactionId)
                        navController.navigate(Screen.TransactionDetail.createRoute(transactionId))
                    },
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    backupMessage = viewModel.backupMessage.collectAsStateWithLifecycle().value,
                    onExportJson = viewModel::exportJson,
                    onImportJson = viewModel::importJson,
                    onExportCsv = viewModel::exportCsv,
                    onClearBackupMessage = viewModel::clearBackupMessage,
                    onOpenPeriodList = {
                        navController.navigate(Screen.BudgetPeriodList.route)
                    },
                )
            }
            composable(Screen.TransactionHistory.route) {
                TransactionHistoryScreen(
                    transactions = transactionHistoryState,
                    currencyCode = dashboardState.currencyCode,
                    onTransactionClick = { transactionId ->
                        viewModel.loadTransaction(transactionId)
                        navController.navigate(Screen.TransactionDetail.createRoute(transactionId))
                    },
                )
            }
            composable(Screen.TransactionDetail.route) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId")?.toLongOrNull()
                LaunchedEffect(transactionId) {
                    transactionId?.let { viewModel.loadTransaction(it) }
                }
                TransactionDetailScreen(
                    state = transactionDetailState,
                    onEdit = {
                        val id = transactionDetailState?.transactionId ?: return@TransactionDetailScreen
                        navController.navigate(Screen.EditTransaction.createRoute(id))
                    },
                    onDelete = {
                        val id = transactionDetailState?.transactionId ?: return@TransactionDetailScreen
                        viewModel.deleteTransaction(id)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.EditTransaction.route) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId")?.toLongOrNull()
                LaunchedEffect(transactionId) {
                    transactionId?.let { viewModel.loadTransaction(it) }
                }
                EditTransactionScreen(
                    state = transactionDetailState,
                    categories = expenseEntryState.categories,
                    onSaveNormal = { amountMinor, categoryId, note, dateText ->
                        val id = transactionDetailState?.transactionId ?: return@EditTransactionScreen
                        viewModel.editNormalExpense(id, amountMinor, categoryId, note, dateText)
                        navController.popBackStack()
                    },
                    onSaveNote = { note, dateText ->
                        val id = transactionDetailState?.transactionId ?: return@EditTransactionScreen
                        viewModel.editTransactionNote(id, note, dateText)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.BudgetPeriodList.route) {
                BudgetPeriodListScreen(
                    periods = periodListState,
                    onCreateNext = {
                        navController.navigate(Screen.CreateNextPeriod.route)
                    },
                )
            }
            composable(Screen.CreateNextPeriod.route) {
                val activePeriod = periodListState.firstOrNull { it.isActive }
                val previousCategories = budgetSetupState.categories
                val defaultStartDate = activePeriod?.let {
                    try {
                        val endDate = java.time.LocalDate.parse(budgetSetupState.endDateText)
                        endDate.plusDays(1).toString()
                    } catch (_: Exception) {
                        java.time.LocalDate.now().plusDays(1).toString()
                    }
                } ?: java.time.LocalDate.now().plusDays(1).toString()
                val defaultEndDate = activePeriod?.let { period ->
                    try {
                        val start = java.time.LocalDate.parse(budgetSetupState.startDateText)
                        val end = java.time.LocalDate.parse(budgetSetupState.endDateText)
                        val duration = end.toEpochDay() - start.toEpochDay()
                        val newStart = java.time.LocalDate.parse(defaultStartDate)
                        newStart.plusDays(duration).toString()
                    } catch (_: Exception) {
                        java.time.LocalDate.now().plusWeeks(4).toString()
                    }
                } ?: java.time.LocalDate.now().plusWeeks(4).toString()

                CreateNextPeriodScreen(
                    defaultName = activePeriod?.let { "${it.name} (Next)" } ?: "New Budget",
                    defaultCurrencyCode = activePeriod?.currencyCode ?: "IDR",
                    defaultStartDate = defaultStartDate,
                    defaultEndDate = defaultEndDate,
                    previousCategories = previousCategories,
                    pendingImpacts = pendingImpactsState,
                    activePeriodCurrency = activePeriod?.currencyCode ?: "IDR",
                    onSave = { name: String, totalAmountMinor: Long, currencyCode: String, startDateText: String, endDateText: String, copyCategoryIds: List<Long>, applyImpactIds: List<Long>, impactCategoryMapping: Map<Long, Long> ->
                        viewModel.createNextPeriod(
                            name, totalAmountMinor, currencyCode,
                            startDateText, endDateText,
                            copyCategoryIds, applyImpactIds, impactCategoryMapping,
                        )
                        navController.popBackStack()
                    },
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
