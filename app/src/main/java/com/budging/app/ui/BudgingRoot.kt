package com.budging.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.budging.app.ui.component.BottomNavItemPill
import com.budging.app.ui.component.BudgetTopBar
import com.budging.app.ui.screen.BudgetPeriodListScreen
import com.budging.app.ui.screen.BudgetSetupScreen
import com.budging.app.ui.screen.CategoryDetailScreen
import com.budging.app.ui.screen.CreateNextPeriodScreen
import com.budging.app.ui.screen.DashboardScreen
import com.budging.app.ui.screen.EditTransactionScreen
import com.budging.app.ui.screen.LogExpenseScreen
import com.budging.app.ui.screen.SettingsScreen
import com.budging.app.ui.screen.SubscriptionsScreen
import com.budging.app.ui.screen.TransactionDetailScreen
import com.budging.app.ui.screen.TransactionHistoryScreen
import com.budging.app.ui.theme.BudgingTheme
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun BudgingRoot(
    viewModel: BudgingViewModel,
    externalRoute: String? = null,
    onExternalRouteConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val pagerState = rememberPagerState(pageCount = { topLevelDestinations.size })
    val scope = rememberCoroutineScope()
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val budgetSetupState by viewModel.budgetSetupState.collectAsStateWithLifecycle()
    val expenseEntryState by viewModel.expenseEntryState.collectAsStateWithLifecycle()
    val categoryDetailState by viewModel.categoryDetailState.collectAsStateWithLifecycle()
    val transactionDetailState by viewModel.transactionDetailState.collectAsStateWithLifecycle()
    val transactionHistoryState by viewModel.transactionHistoryState.collectAsStateWithLifecycle()
    val periodListState by viewModel.periodListState.collectAsStateWithLifecycle()
    val pendingImpactsState by viewModel.pendingImpactsState.collectAsStateWithLifecycle()
    val recurringTemplatesState by viewModel.recurringTemplatesState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val currentTopLevelScreen = topLevelDestinations.getOrElse(pagerState.currentPage) { Screen.Dashboard }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(externalRoute) {
        val route = externalRoute ?: return@LaunchedEffect
        val topLevelIndex = topLevelIndexForRoute(route)
        if (topLevelIndex != null) {
            navController.navigate(Screen.Home.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            pagerState.animateScrollToPage(topLevelIndex)
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        onExternalRouteConsumed()
    }

    Scaffold(
        topBar = {
            val currentRoute = destination?.route ?: Screen.Home.route
            BudgetTopBar(
                title = when (currentRoute) {
                    Screen.Home.route -> topLevelTitle(currentTopLevelScreen)
                    Screen.BudgetSetup.route -> "Set Budget"
                    Screen.LogExpense.route -> "Log Expense"
                    Screen.CategoryDetail.route -> "Category Detail"
                    Screen.Settings.route -> "Overview"
                    Screen.Subscriptions.route -> "Subscriptions"
                    Screen.TransactionHistory.route -> "Transaction History"
                    Screen.TransactionDetail.route -> "Transaction Detail"
                    Screen.EditTransaction.route -> "Edit Transaction"
                    Screen.BudgetPeriodList.route -> "Budget Periods"
                    Screen.CreateNextPeriod.route -> "Create Period"
                    else -> "Current Budget"
                },
                showBack = currentRoute != Screen.Home.route,
                onBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() },
            )
        },
        floatingActionButton = {
            if (destination?.route == Screen.Home.route && currentTopLevelScreen == Screen.Dashboard) {
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
                    .padding(horizontal = BudgingTheme.spacing.xl, vertical = BudgingTheme.spacing.sm),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                topLevelDestinations.forEach { item ->
                    BottomNavItemPill(
                        screen = item,
                        selected = currentTopLevelScreen.route == item.route,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch { pagerState.animateScrollToPage(topLevelDestinations.indexOf(item)) }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Home.route) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { page ->
                    when (topLevelDestinations[page]) {
                        Screen.Dashboard -> DashboardScreen(
                            state = dashboardState,
                            onOpenCategory = {
                                viewModel.loadCategory(it)
                                navController.navigate(Screen.CategoryDetail.route)
                            },
                            onViewAllTransactions = { navController.navigate(Screen.TransactionHistory.route) },
                            onTransactionClick = {
                                viewModel.loadTransaction(it)
                                navController.navigate(Screen.TransactionDetail.createRoute(it))
                            },
                            onCreateBudget = { navController.navigate(Screen.CreateNextPeriod.route) },
                        )
                        Screen.BudgetSetup -> BudgetSetupScreen(
                            state = budgetSetupState,
                            onSaveBudget = viewModel::saveBudgetPeriod,
                            onSaveCategory = viewModel::saveCategory,
                            onArchiveCategory = viewModel::setCategoryArchived,
                            onDeleteCategory = viewModel::deleteCategory,
                        )
                        Screen.LogExpense -> LogExpenseScreen(
                            state = expenseEntryState,
                            previewCurrentImpact = viewModel::previewCurrentImpact,
                            onSaveExpense = { amountMinor, categoryId, dateText, note, splitPeriodCount ->
                                if (splitPeriodCount <= 1) {
                                    viewModel.logNormalExpense(amountMinor, categoryId, note, dateText)
                                } else {
                                    viewModel.logSplitExpense(amountMinor, categoryId, note, dateText, splitPeriodCount)
                                }
                            },
                        )
                        Screen.Settings -> SettingsScreen(
                            backupMessage = viewModel.backupMessage.collectAsStateWithLifecycle().value,
                            onExportJson = viewModel::exportJson,
                            onImportJson = viewModel::importJson,
                            onExportCsv = viewModel::exportCsv,
                            onClearBackupMessage = viewModel::clearBackupMessage,
                            onOpenPeriodList = { navController.navigate(Screen.BudgetPeriodList.route) },
                            onOpenSubscriptions = { navController.navigate(Screen.Subscriptions.route) },
                        )
                        else -> Unit
                    }
                }
            }
            composable(Screen.CategoryDetail.route) {
                CategoryDetailScreen(
                    state = categoryDetailState,
                    onTransactionClick = {
                        viewModel.loadTransaction(it)
                        navController.navigate(Screen.TransactionDetail.createRoute(it))
                    },
                )
            }
            composable(Screen.Subscriptions.route) {
                SubscriptionsScreen(
                    templates = recurringTemplatesState,
                    defaultCurrencyCode = dashboardState.currencyCode,
                    onSaveTemplate = viewModel::saveRecurringTemplate,
                    onDeleteTemplate = viewModel::deleteRecurringTemplate,
                )
            }
            composable(Screen.TransactionHistory.route) {
                TransactionHistoryScreen(
                    transactions = transactionHistoryState,
                    currencyCode = dashboardState.currencyCode,
                    onTransactionClick = {
                        viewModel.loadTransaction(it)
                        navController.navigate(Screen.TransactionDetail.createRoute(it))
                    },
                )
            }
            composable(Screen.TransactionDetail.route) { entry ->
                val transactionId = entry.arguments?.getString("transactionId")?.toLongOrNull()
                LaunchedEffect(transactionId) { transactionId?.let(viewModel::loadTransaction) }
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
            composable(Screen.EditTransaction.route) { entry ->
                val transactionId = entry.arguments?.getString("transactionId")?.toLongOrNull()
                LaunchedEffect(transactionId) { transactionId?.let(viewModel::loadTransaction) }
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
                BudgetPeriodListScreen(periods = periodListState, onCreateNext = { navController.navigate(Screen.CreateNextPeriod.route) })
            }
            composable(Screen.CreateNextPeriod.route) {
                val activePeriod = periodListState.firstOrNull { it.isActive }
                val previousCategories = budgetSetupState.categories
                val defaultStartDate = activePeriod?.let {
                    try {
                        LocalDate.parse(budgetSetupState.endDateText).plusDays(1).toString()
                    } catch (_: Exception) {
                        LocalDate.now().plusDays(1).toString()
                    }
                } ?: LocalDate.now().plusDays(1).toString()
                val defaultEndDate = activePeriod?.let {
                    try {
                        val start = LocalDate.parse(budgetSetupState.startDateText)
                        val end = LocalDate.parse(budgetSetupState.endDateText)
                        LocalDate.parse(defaultStartDate).plusDays(end.toEpochDay() - start.toEpochDay()).toString()
                    } catch (_: Exception) {
                        LocalDate.now().plusWeeks(4).toString()
                    }
                } ?: LocalDate.now().plusWeeks(4).toString()

                CreateNextPeriodScreen(
                    defaultName = activePeriod?.let { "${it.name} (Next)" } ?: "New Budget",
                    defaultCurrencyCode = activePeriod?.currencyCode ?: "IDR",
                    defaultStartDate = defaultStartDate,
                    defaultEndDate = defaultEndDate,
                    previousCategories = previousCategories,
                    pendingImpacts = pendingImpactsState,
                    recurringTemplates = recurringTemplatesState,
                    activePeriodCurrency = activePeriod?.currencyCode ?: "IDR",
                    onDeletePendingImpact = viewModel::deletePendingImpact,
                    onSave = { name, totalAmountMinor, currencyCode, startDateText, endDateText, copyCategoryIds, applyImpactIds, impactCategoryMapping, applyRecurringPreviewKeys, recurringCategoryMapping ->
                        viewModel.createNextPeriod(
                            name,
                            totalAmountMinor,
                            currencyCode,
                            startDateText,
                            endDateText,
                            copyCategoryIds,
                            applyImpactIds,
                            impactCategoryMapping,
                            applyRecurringPreviewKeys,
                            recurringCategoryMapping,
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

private fun topLevelIndexForRoute(route: String): Int? {
    val index = topLevelDestinations.indexOfFirst { it.route == route }
    return index.takeIf { it >= 0 }
}

private fun topLevelTitle(screen: Screen): String = when (screen) {
    Screen.Dashboard -> "Current Budget"
    Screen.BudgetSetup -> "Set Budget"
    Screen.LogExpense -> "Log Expense"
    Screen.Settings -> "Overview"
    else -> "Current Budget"
}
