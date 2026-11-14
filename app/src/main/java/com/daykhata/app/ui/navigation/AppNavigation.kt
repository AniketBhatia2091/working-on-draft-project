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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daykhata.app.domain.model.ViewMode
import com.daykhata.app.ui.dashboard.DashboardScreen
import com.daykhata.app.ui.dashboard.DashboardViewModel
import com.daykhata.app.ui.savings.SavingsScreen
import com.daykhata.app.ui.savings.SavingsViewModel
import com.daykhata.app.ui.settings.SettingsScreen
import com.daykhata.app.ui.settings.SettingsViewModel
import com.daykhata.app.ui.weekly.WeeklyScreen
import com.daykhata.app.ui.weekly.WeeklyViewModel

@Composable
fun AppNavigation(container: AppContainer) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val appMode by container.appModeState.mode.collectAsStateWithLifecycle()
    val isPersonalMode = appMode == ViewMode.PERSONAL

    Scaffold(
        bottomBar = {
            NavigationBar {
                getBottomNavItems(isPersonalMode).forEach { screen ->
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
                        navController.navigate(Screen.DailyList.createRoute())
                    }
                )
            }

            // Daily List
            composable(
                route = Screen.DailyList.route,
                arguments = listOf(
                    navArgument("date") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val dateParam = backStackEntry.arguments?.getString("date")
                val initialDate = dateParam?.toLongOrNull()

                val viewModel: DailyListViewModel = viewModel(
                    key = "DailyListViewModel_$initialDate",
                    factory = DailyListViewModel.Factory(
                        expenseRepository = container.expenseRepository,
                        settingsRepository = container.settingsRepository,
                        calculator = container.expenseCalculator,
                        appModeState = container.appModeState,
                        initialDate = initialDate
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

            // Weekly Log
            composable(Screen.Weekly.route) {
                val viewModel: WeeklyViewModel = viewModel(
                    factory = WeeklyViewModel.Factory(
                        expenseRepository = container.expenseRepository,
                        settingsRepository = container.settingsRepository,
                        expenseCalculator = container.expenseCalculator,
                        weeklyCalculator = container.weeklySummaryCalculator,
                        appModeState = container.appModeState
                    )
                )
                WeeklyScreen(
                    viewModel = viewModel,
                    onNavigateToDailyDate = { dateMillis ->
                        navController.navigate(Screen.DailyList.createRoute(dateMillis))
                    }
                )
            }

            // Savings (Personal-Only)
            composable(Screen.Savings.route) {
                val viewModel: SavingsViewModel = viewModel(
                    factory = SavingsViewModel.Factory(
                        expenseRepository = container.expenseRepository,
                        settingsRepository = container.settingsRepository,
                        savingsCalculator = container.savingsCalculator,
                        appModeState = container.appModeState
                    )
                )
                SavingsScreen(
                    viewModel = viewModel,
                    onExitToDashboard = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
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
