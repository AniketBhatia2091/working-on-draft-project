package com.daykhata.app.domain

import com.daykhata.app.data.local.Expense
import com.daykhata.app.domain.model.GoalPeriod
import com.daykhata.app.domain.model.SavingsGoal
import com.daykhata.app.domain.model.Settings
import com.daykhata.app.domain.model.TrendRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Modifier
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class SavingsCalculatorTest {

    private lateinit var calculator: SavingsCalculator
    private val zone = ZoneId.systemDefault()

    @Before
    fun setup() {
        calculator = SavingsCalculator()
    }

    private fun getEpoch(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    @Test
    fun `normal data computes correct savings totals across all time and periods`() {
        // Assume today is 15 Nov 2026 (Sunday)
        val todayMillis = getEpoch(2026, Calendar.NOVEMBER, 15)

        val expenses = listOf(
            // Today (Week & Month)
            Expense(id = 1, date = todayMillis, realAmount = 300, homeAmount = 500, homeLabel = "Groceries"), // +200
            // Earlier this week (Wed 11 Nov)
            Expense(id = 2, date = getEpoch(2026, Calendar.NOVEMBER, 11), realAmount = 150, homeAmount = 250, homeLabel = "Snacks"), // +100
            // Earlier this month (Sun 1 Nov)
            Expense(id = 3, date = getEpoch(2026, Calendar.NOVEMBER, 1), realAmount = 800, homeAmount = 1200, homeLabel = "Utilities"), // +400
            // Last month (Oct 20)
            Expense(id = 4, date = getEpoch(2026, Calendar.OCTOBER, 20), realAmount = 1000, homeAmount = 1500, homeLabel = "Shopping") // +500
        )

        val settings = Settings(
            savingsGoal = 500,
            goalPeriod = GoalPeriod.WEEKLY
        )

        val summary = calculator.calculateSavingsSummary(
            allExpenses = expenses,
            periodExpenses = expenses.filter { it.date >= getEpoch(2026, Calendar.NOVEMBER, 1) },
            settings = settings,
            clockMillis = todayMillis
        )

        assertEquals(700, summary.currentPeriodSavings) // 200 + 100 + 400
        assertEquals(1200, summary.allTimeSavings) // 200 + 100 + 400 + 500
        assertEquals(300, summary.thisWeekSavings) // 200 + 100
        assertEquals(700, summary.thisMonthSavings) // 200 + 100 + 400
        assertEquals(4, summary.profitableExpensesCount)
        assertEquals(300, summary.averageSavingsPerProfitableExpense) // 1200 / 4
        assertFalse(summary.hasNegativeSavingsExpenses)
    }

    @Test
    fun `no expenses yields zeros and no goal progress error`() {
        val todayMillis = getEpoch(2026, Calendar.NOVEMBER, 15)
        val settings = Settings(savingsGoal = 2000, goalPeriod = GoalPeriod.MONTHLY)

        val summary = calculator.calculateSavingsSummary(
            allExpenses = emptyList(),
            periodExpenses = emptyList(),
            settings = settings,
            clockMillis = todayMillis
        )

        assertEquals(0, summary.allTimeSavings)
        assertEquals(0, summary.currentPeriodSavings)
        assertEquals(0, summary.thisWeekSavings)
        assertEquals(0, summary.thisMonthSavings)
        assertEquals(0, summary.profitableExpensesCount)
        assertEquals(0, summary.averageSavingsPerProfitableExpense)
        assertFalse(summary.hasNegativeSavingsExpenses)

        val goalProgress = summary.goalProgress
        assertNotNull(goalProgress)
        assertEquals(0, goalProgress?.savedAmount)
        assertEquals(2000, goalProgress?.remainingAmount)
        assertEquals(0f, goalProgress?.progressRatio)
        assertFalse(goalProgress?.isGoalReached ?: true)
    }

    @Test
    fun `home equals real produces zero savings`() {
        val todayMillis = getEpoch(2026, Calendar.NOVEMBER, 15)
        val expenses = listOf(
            Expense(id = 1, date = todayMillis, realAmount = 500, homeAmount = 500, homeLabel = "Bill")
        )
        val settings = Settings()

        val summary = calculator.calculateSavingsSummary(
            allExpenses = expenses,
            periodExpenses = expenses,
            settings = settings,
            clockMillis = todayMillis
        )

        assertEquals(0, summary.allTimeSavings)
        assertEquals(0, summary.profitableExpensesCount)
        assertEquals(0, summary.averageSavingsPerProfitableExpense)
        assertFalse(summary.hasNegativeSavingsExpenses)
    }

    @Test
    fun `negative savings when home less than real is accurately tracked and noted`() {
        val todayMillis = getEpoch(2026, Calendar.NOVEMBER, 15)
        val expenses = listOf(
            Expense(id = 1, date = todayMillis, realAmount = 800, homeAmount = 500, homeLabel = "Overspent dinner"), // -300
            Expense(id = 2, date = todayMillis, realAmount = 300, homeAmount = 400, homeLabel = "Profitable fuel")    // +100
        )
        val settings = Settings()

        val summary = calculator.calculateSavingsSummary(
            allExpenses = expenses,
            periodExpenses = expenses,
            settings = settings,
            clockMillis = todayMillis
        )

        assertEquals(-200, summary.allTimeSavings) // -300 + 100
        assertTrue(summary.hasNegativeSavingsExpenses)
        assertEquals(1, summary.negativeSavingsCount)
        assertEquals(300, summary.totalNegativeSavingsAmount)

        // Profitable average should ONLY consider the profitable expense (id=2)
        assertEquals(1, summary.profitableExpensesCount)
        assertEquals(100, summary.averageSavingsPerProfitableExpense)
    }

    @Test
    fun `goal reached and exceeded calculates extra saved amount`() {
        val todayMillis = getEpoch(2026, Calendar.NOVEMBER, 15)
        val expenses = listOf(
            Expense(id = 1, date = todayMillis, realAmount = 1000, homeAmount = 2500, homeLabel = "Big save") // +1500
        )
        // Goal is 1000 for the week
        val settings = Settings(savingsGoal = 1000, goalPeriod = GoalPeriod.WEEKLY)

        val summary = calculator.calculateSavingsSummary(
            allExpenses = expenses,
            periodExpenses = expenses,
            settings = settings,
            clockMillis = todayMillis
        )

        val progress = summary.goalProgress
        assertNotNull(progress)
        assertTrue(progress?.isGoalReached == true)
        assertEquals(1500, progress?.savedAmount)
        assertEquals(0, progress?.remainingAmount)
        assertEquals(500, progress?.extraSaved) // 1500 - 1000 = 500 extra
        assertEquals(1.5f, progress?.progressRatio ?: 0f, 0.01f)
    }

    @Test
    fun `goal not set returns null progress`() {
        val todayMillis = getEpoch(2026, Calendar.NOVEMBER, 15)
        val expenses = listOf(
            Expense(id = 1, date = todayMillis, realAmount = 100, homeAmount = 200, homeLabel = "Item")
        )
        val settings = Settings(savingsGoal = 0, goalPeriod = GoalPeriod.NONE)

        val summary = calculator.calculateSavingsSummary(
            allExpenses = expenses,
            periodExpenses = expenses,
            settings = settings,
            clockMillis = todayMillis
        )

        assertNull(summary.goalProgress)
    }

    @Test
    fun `weekly vs monthly goal periods evaluate against corresponding timeframe`() {
        val todayMillis = getEpoch(2026, Calendar.NOVEMBER, 15) // Sunday of week 2
        val earlierInMonthMillis = getEpoch(2026, Calendar.NOVEMBER, 2) // Week 1 (different week, same month)

        val expenses = listOf(
            Expense(id = 1, date = earlierInMonthMillis, realAmount = 100, homeAmount = 400, homeLabel = "W1 save"), // +300
            Expense(id = 2, date = todayMillis, realAmount = 100, homeAmount = 300, homeLabel = "W2 save")          // +200
        )

        // Weekly goal: should only see +200
        val weeklySettings = Settings(savingsGoal = 500, goalPeriod = GoalPeriod.WEEKLY)
        val weeklySummary = calculator.calculateSavingsSummary(expenses, expenses, weeklySettings, todayMillis)
        assertEquals(200, weeklySummary.goalProgress?.savedAmount)

        // Monthly goal: should see both (+300 + 200 = 500)
        val monthlySettings = Settings(savingsGoal = 500, goalPeriod = GoalPeriod.MONTHLY)
        val monthlySummary = calculator.calculateSavingsSummary(expenses, expenses, monthlySettings, todayMillis)
        assertEquals(500, monthlySummary.goalProgress?.savedAmount)
        assertTrue(monthlySummary.goalProgress?.isGoalReached == true)
    }

    @Test
    fun `trend ranges handle boundaries across months and years`() {
        // Date: Jan 2, 2026
        val jan2Millis = getEpoch(2026, Calendar.JANUARY, 2)

        val expenses = listOf(
            Expense(id = 1, date = getEpoch(2025, Calendar.DECEMBER, 31), realAmount = 200, homeAmount = 400, homeLabel = "NYE"), // +200 in Dec 2025
            Expense(id = 2, date = getEpoch(2026, Calendar.JANUARY, 2), realAmount = 100, homeAmount = 250, homeLabel = "New Year")   // +150 in Jan 2026
        )

        // 7 days trend
        val daysTrend = calculator.calculateTrendSeries(expenses, TrendRange.LAST_7_DAYS, jan2Millis)
        assertEquals(7, daysTrend.size)

        // 4 weeks trend
        val weeksTrend = calculator.calculateTrendSeries(expenses, TrendRange.LAST_4_WEEKS, jan2Millis)
        assertEquals(4, weeksTrend.size)

        // 6 months trend across year boundary (Aug 2025 .. Jan 2026)
        val monthsTrend = calculator.calculateTrendSeries(expenses, TrendRange.LAST_6_MONTHS, jan2Millis)
        assertEquals(6, monthsTrend.size)
        assertEquals("Jan", monthsTrend.last().label)
        assertEquals("Dec", monthsTrend[monthsTrend.size - 2].label)
        assertEquals(150, monthsTrend.last().savings)
        assertEquals(200, monthsTrend[monthsTrend.size - 2].savings)
    }

    @Test
    fun `savings is unaffected by the budget`() {
        val todayMillis = getEpoch(2026, Calendar.NOVEMBER, 15)
        val expenses = listOf(
            Expense(id = 1, date = todayMillis, realAmount = 400, homeAmount = 750, homeLabel = "Test")
        )

        val summaryBudget10k = calculator.calculateSavingsSummary(expenses, expenses, Settings(budget = 10000), todayMillis)
        val summaryBudget50k = calculator.calculateSavingsSummary(expenses, expenses, Settings(budget = 50000), todayMillis)
        val summaryBudgetZero = calculator.calculateSavingsSummary(expenses, expenses, Settings(budget = 0), todayMillis)

        val expectedSavings = 350

        assertEquals(expectedSavings, summaryBudget10k.allTimeSavings)
        assertEquals(expectedSavings, summaryBudget50k.allTimeSavings)
        assertEquals(expectedSavings, summaryBudgetZero.allTimeSavings)
    }

    @Test
    fun `safety structural test proves home message builders never accept SavingsSummary or savings models`() {
        val homeMessageBuilderClass = HomeMessageBuilder::class.java
        val homeWeeklyBuilderClass = HomeWeeklyMessageBuilder::class.java

        // Inspect all declared methods on HomeMessageBuilder
        for (method in homeMessageBuilderClass.declaredMethods) {
            for (paramType in method.parameterTypes) {
                assertFalse(
                    "HomeMessageBuilder must not accept SavingsSummary or savings types! Found parameter: ${paramType.simpleName}",
                    paramType.name.contains("Savings")
                )
            }
        }

        // Inspect all declared methods on HomeWeeklyMessageBuilder
        for (method in homeWeeklyBuilderClass.declaredMethods) {
            for (paramType in method.parameterTypes) {
                assertFalse(
                    "HomeWeeklyMessageBuilder must not accept SavingsSummary or savings types! Found parameter: ${paramType.simpleName}",
                    paramType.name.contains("Savings")
                )
            }
        }
    }
}
