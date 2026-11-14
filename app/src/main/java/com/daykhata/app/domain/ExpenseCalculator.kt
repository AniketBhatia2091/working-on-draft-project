package com.daykhata.app.domain

import com.daykhata.app.data.local.Expense
import com.daykhata.app.domain.model.ExpenseSummary
import com.daykhata.app.domain.model.Settings

/**
 * Pure Kotlin calculator responsible for all core financial metrics in DayKhata.
 *
 * Rules:
 * - homeSpent = sum of homeAmount
 * - realSpent = sum of realAmount
 * - homeBalance = budget - homeSpent
 * - realBalance = budget - realSpent
 * - savings = homeSpent - realSpent
 * - cutOffReached = homeBalance <= cutOffLimit (strictly uses HOME balance, never real balance)
 * - Savings is purely (homeSpent - realSpent) and is never affected by the budget.
 */
class ExpenseCalculator {

    fun calculate(expenses: List<Expense>, settings: Settings): ExpenseSummary {
        return calculate(
            expenses = expenses,
            budget = settings.budget,
            cutOffLimit = settings.cutOffLimit
        )
    }

    fun calculate(expenses: List<Expense>, budget: Int, cutOffLimit: Int): ExpenseSummary {
        val homeSpent = expenses.sumOf { it.homeAmount }
        val realSpent = expenses.sumOf { it.realAmount }
        val homeBalance = budget - homeSpent
        val realBalance = budget - realSpent
        val savings = homeSpent - realSpent
        val cutOffReached = homeBalance <= cutOffLimit

        return ExpenseSummary(
            homeSpent = homeSpent,
            realSpent = realSpent,
            homeBalance = homeBalance,
            realBalance = realBalance,
            savings = savings,
            cutOffReached = cutOffReached,
            budget = budget,
            cutOffLimit = cutOffLimit
        )
    }

    /**
     * Resolves the home amount for an expense input.
     * When homeAmount is null or empty/blank, it defaults to realAmount.
     */
    fun resolveHomeAmount(realAmount: Int, homeAmountInput: String?): Int {
        if (homeAmountInput.isNullOrBlank()) {
            return realAmount
        }
        return homeAmountInput.trim().toIntOrNull() ?: realAmount
    }

    /**
     * Resolves the home amount for an expense input when given an optional integer.
     */
    fun resolveHomeAmount(realAmount: Int, homeAmountInput: Int?): Int {
        return homeAmountInput ?: realAmount
    }

    /**
     * Calculates the total home spent for a specific day's expenses.
     * Strictly reads only homeAmount.
     */
    fun calculateTodayHomeTotal(todayExpenses: List<Expense>): Int {
        return todayExpenses.sumOf { it.homeAmount }
    }
}
