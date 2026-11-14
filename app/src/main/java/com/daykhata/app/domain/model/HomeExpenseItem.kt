package com.daykhata.app.domain.model

import com.daykhata.app.data.local.Expense

/**
 * Isolated model strictly representing home-facing expense data.
 * This ensures private attributes (realAmount, personalLabel) are impossible to leak by type.
 */
data class HomeExpenseItem(
    val homeLabel: String,
    val homeAmount: Int
)

fun Expense.toHomeExpenseItem(): HomeExpenseItem {
    return HomeExpenseItem(
        homeLabel = this.homeLabel,
        homeAmount = this.homeAmount
    )
}
