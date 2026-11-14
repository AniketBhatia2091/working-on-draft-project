package com.daykhata.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object DailyList : Screen("daily_list", "Daily List", Icons.AutoMirrored.Filled.List)
    data object AddExpense : Screen("add_expense?expenseId={expenseId}", "Add", Icons.Default.AddCircle) {
        fun createRoute(expenseId: Long? = null): String {
            return if (expenseId != null) "add_expense?expenseId=$expenseId" else "add_expense"
        }
    }
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.AddExpense,
    Screen.DailyList,
    Screen.Settings
)
