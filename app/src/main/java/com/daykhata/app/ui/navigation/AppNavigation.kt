package com.daykhata.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.daykhata.app.AppContainer
import com.daykhata.app.ui.add_expense.AddExpenseScreen
import com.daykhata.app.ui.add_expense.AddExpenseViewModel
import com.daykhata.app.ui.daily_list.DailyListScreen
import com.daykhata.app.ui.daily_list.DailyListViewModel
import com.daykhata.app.ui.dashboard.DashboardScreen
import com.daykhata.app.ui.dashboard.DashboardViewModel
import com.daykhata.app.ui.settings.SettingsScreen
import com.daykhata.app.ui.settings.SettingsViewModel

@Composable
fun AppNavigation(container: AppContainer) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    val isSelected = currentDestination?.route?.startsWith(screen.route.substringBefore("?")) == true

                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route.substringBefore("?")) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Dashboard
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.Factory(
                        expenseRepository = container.expenseRepository,
                        settingsRepository = container.settingsRepository,
                        calculator = container.expenseCalculator,
                        appModeState = container.appModeState
                    )
                )
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddExpense = {
                        navController.navigate(Screen.AddExpense.createRoute())
                    },
                    onNavigateToDailyList = {
                        navController.navigate(Screen.DailyList.route)
                    }
                )
            }

            // Daily List
            composable(Screen.DailyList.route) {
                val viewModel: DailyListViewModel = viewModel(
                    factory = DailyListViewModel.Factory(
                        expenseRepository = container.expenseRepository,
                        settingsRepository = container.settingsRepository,
                        calculator = container.expenseCalculator,
                        appModeState = container.appModeState
                    )
                )
                DailyListScreen(
                    viewModel = viewModel,
                    onNavigateToEditExpense = { expenseId ->
                        navController.navigate(Screen.AddExpense.createRoute(expenseId))
                    },
                    onNavigateToAddExpense = {
                        navController.navigate(Screen.AddExpense.createRoute())
                    }
                )
            }

            // Add Expense / Edit Expense
            composable(
                route = Screen.AddExpense.route,
                arguments = listOf(
                    navArgument("expenseId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val expenseIdParam = backStackEntry.arguments?.getString("expenseId")
                val expenseId = expenseIdParam?.toLongOrNull()

                val viewModel: AddExpenseViewModel = viewModel(
                    key = "AddExpenseViewModel_$expenseId",
                    factory = AddExpenseViewModel.Factory(
                        expenseRepository = container.expenseRepository,
                        settingsRepository = container.settingsRepository,
                        calculator = container.expenseCalculator,
                        expenseIdToEdit = expenseId
                    )
                )
                AddExpenseScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Settings
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        settingsRepository = container.settingsRepository
                    )
                )
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
