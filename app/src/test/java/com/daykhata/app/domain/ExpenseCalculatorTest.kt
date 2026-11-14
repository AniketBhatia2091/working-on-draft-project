package com.daykhata.app.domain

import com.daykhata.app.data.local.Expense
import com.daykhata.app.domain.model.PeriodType
import com.daykhata.app.domain.model.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExpenseCalculatorTest {

    private lateinit var calculator: ExpenseCalculator

    @Before
    fun setup() {
        calculator = ExpenseCalculator()
    }

    @Test
    fun `core calculations compute correctly with basic expenses`() {
        // Given
        val expenses = listOf(
            Expense(id = 1, date = 1000L, realAmount = 300, homeAmount = 500, homeLabel = "Groceries", personalLabel = "Snacks"),
            Expense(id = 2, date = 2000L, realAmount = 700, homeAmount = 1000, homeLabel = "Fuel", personalLabel = "Fuel + tip")
        )
        val budget = 15000
        val cutOffLimit = 2000

        // When
        val summary = calculator.calculate(expenses, budget, cutOffLimit)

        // Then
        assertEquals(1500, summary.homeSpent) // 500 + 1000
        assertEquals(1000, summary.realSpent) // 300 + 700
        assertEquals(13500, summary.homeBalance) // 15000 - 1500
        assertEquals(14000, summary.realBalance) // 15000 - 1000
        assertEquals(500, summary.savings) // 1500 - 1000
        assertFalse(summary.cutOffReached) // 13500 > 2000
    }

    @Test
    fun `blank or null home amount defaults to real amount`() {
        // When home amount is resolved with empty or null string
        val resolvedFromNull = calculator.resolveHomeAmount(realAmount = 450, homeAmountInput = null as String?)
        val resolvedFromBlank = calculator.resolveHomeAmount(realAmount = 450, homeAmountInput = "   ")
        val resolvedFromInvalid = calculator.resolveHomeAmount(realAmount = 450, homeAmountInput = "abc")
        val resolvedFromIntNull = calculator.resolveHomeAmount(realAmount = 450, homeAmountInput = null as Int?)
        val resolvedWithExplicit = calculator.resolveHomeAmount(realAmount = 450, homeAmountInput = "600")

        // Then
        assertEquals(450, resolvedFromNull)
        assertEquals(450, resolvedFromBlank)
        assertEquals(450, resolvedFromInvalid)
        assertEquals(450, resolvedFromIntNull)
        assertEquals(600, resolvedWithExplicit)

        // Using Expense.create factory
        val expense = Expense.create(
            id = 1,
            date = 1000L,
            realAmount = 500,
            homeAmount = null,
            homeLabel = "Lunch"
        )
        assertEquals(500, expense.homeAmount)
        assertEquals(500, expense.realAmount)
    }

    @Test
    fun `cut-off triggers on home balance only, real balance never triggers it`() {
        val budget = 10000
        val cutOffLimit = 2000

        // Case A: Real spent is very high (realBalance = 1000 <= 2000), but Home spent is low (homeBalance = 7000 > 2000)
        // Cut-off must NOT trigger because home balance is safe!
        val expensesWhereRealIsExceeded = listOf(
            Expense(id = 1, date = 1000L, realAmount = 9000, homeAmount = 3000, homeLabel = "Rent")
        )
        val summaryA = calculator.calculate(expensesWhereRealIsExceeded, budget, cutOffLimit)
        assertEquals(7000, summaryA.homeBalance)
        assertEquals(1000, summaryA.realBalance)
        assertFalse("Cut-off should NOT trigger when home balance is above limit, even if real balance is low", summaryA.cutOffReached)

        // Case B: Home spent reaches cut-off (homeBalance = 1500 <= 2000), even if real spent is low (realBalance = 8000)
        // Cut-off MUST trigger because home balance is <= cutOffLimit
        val expensesWhereHomeReachesCutoff = listOf(
            Expense(id = 2, date = 2000L, realAmount = 2000, homeAmount = 8500, homeLabel = "Family trip")
        )
        val summaryB = calculator.calculate(expensesWhereHomeReachesCutoff, budget, cutOffLimit)
        assertEquals(1500, summaryB.homeBalance)
        assertEquals(8000, summaryB.realBalance)
        assertTrue("Cut-off MUST trigger when home balance is <= cut-off limit", summaryB.cutOffReached)

        // Case C: Exact boundary: homeBalance == cutOffLimit
        val expensesExactBoundary = listOf(
            Expense(id = 3, date = 3000L, realAmount = 5000, homeAmount = 8000, homeLabel = "Appliances")
        )
        val summaryC = calculator.calculate(expensesExactBoundary, budget, cutOffLimit)
        assertEquals(2000, summaryC.homeBalance)
        assertTrue("Cut-off triggers when home balance equals cut-off limit", summaryC.cutOffReached)
    }

    @Test
    fun `savings is never affected by the budget`() {
        val expenses = listOf(
            Expense(id = 1, date = 1000L, realAmount = 400, homeAmount = 600, homeLabel = "Dinner"),
            Expense(id = 2, date = 2000L, realAmount = 100, homeAmount = 300, homeLabel = "Coffee")
        )

        // Test with varying budgets
        val summaryBudget10k = calculator.calculate(expenses, budget = 10000, cutOffLimit = 1000)
        val summaryBudget50k = calculator.calculate(expenses, budget = 50000, cutOffLimit = 1000)
        val summaryBudget0 = calculator.calculate(expenses, budget = 0, cutOffLimit = 1000)
        val summaryBudgetNegative = calculator.calculate(expenses, budget = -5000, cutOffLimit = 1000)

        val expectedSavings = (600 + 300) - (400 + 100) // 900 - 500 = 400

        assertEquals(expectedSavings, summaryBudget10k.savings)
        assertEquals(expectedSavings, summaryBudget50k.savings)
        assertEquals(expectedSavings, summaryBudget0.savings)
        assertEquals(expectedSavings, summaryBudgetNegative.savings)
    }

    @Test
    fun `today home total reads strictly home amount`() {
        val todayExpenses = listOf(
            Expense(id = 1, date = 1000L, realAmount = 150, homeAmount = 200, homeLabel = "Milk", personalLabel = "Milk + chocolate"),
            Expense(id = 2, date = 2000L, realAmount = 300, homeAmount = 350, homeLabel = "Bread", personalLabel = "Bakery")
        )

        val todayHomeTotal = calculator.calculateTodayHomeTotal(todayExpenses)
        assertEquals(550, todayHomeTotal) // 200 + 350
    }
}
