package com.budging.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun BudgingRoot(viewModel: BudgingViewModel) {
    val navController = rememberNavController()
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val categoryDetailState by viewModel.categoryDetailState.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    Scaffold(
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
                BudgetSetupScreen(state = dashboardState)
            }
            composable(Screen.LogExpense.route) {
                LogExpenseScreen()
            }
            composable(Screen.CategoryDetail.route) {
                CategoryDetailScreen(state = categoryDetailState)
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
