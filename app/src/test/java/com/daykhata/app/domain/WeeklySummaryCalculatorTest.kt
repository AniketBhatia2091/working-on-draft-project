package com.daykhata.app.domain

import com.daykhata.app.data.local.Expense
import com.daykhata.app.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.util.Calendar

class WeeklySummaryCalculatorTest {

    private lateinit var calculator: WeeklySummaryCalculator

    @Before
    fun setup() {
        calculator = WeeklySummaryCalculator()
    }

    private fun getWeekRange(year: Int, month: Int, day: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        return DateUtils.getWeekRangeForDate(cal.timeInMillis)
    }

    @Test
    fun `normal week computes accurate daily totals, week totals, average and highest day`() {
        // Week of Mon 10 Nov 2025 to Sun 16 Nov 2025
        val (startWeek, endWeek) = getWeekRange(2025, Calendar.NOVEMBER, 12)
        val days = DateUtils.getDaysOfWeekForRange(startWeek)

        val monMillis = days[0].second
        val tueMillis = days[1].second
        val wedMillis = days[2].second

        val expenses = listOf(
            Expense(id = 1, date = monMillis + 3600000, realAmount = 200, homeAmount = 230, homeLabel = "Groceries", personalLabel = "Snacks"),
            Expense(id = 2, date = tueMillis + 3600000, realAmount = 150, homeAmount = 180, homeLabel = "Transport", personalLabel = "Cab"),
            Expense(id = 3, date = wedMillis + 3600000, realAmount = 300, homeAmount = 400, homeLabel = "Dinner", personalLabel = "Drinks")
        )

        val homeSummary = calculator.calculateHomeSummary(expenses, startWeek, endWeek, homeBalance = 13550)
        val personalSummary = calculator.calculatePersonalSummary(expenses, startWeek, endWeek, homeBalance = 13550, realBalance = 14000)

        // Home summary checks
        assertEquals(7, homeSummary.dayTotals.size)
        assertEquals(810, homeSummary.weekHomeTotal) // 230 + 180 + 400
        assertEquals(116, homeSummary.averagePerDay) // 810 / 7 = 115.7 -> 116
        assertNotNull(homeSummary.highestDay)
        assertEquals(DayOfWeek.WEDNESDAY, homeSummary.highestDay?.dayOfWeek)
        assertEquals(400, homeSummary.highestDay?.homeTotal)

        // Personal summary checks
        assertEquals(650, personalSummary.weekRealTotal) // 200 + 150 + 300
        assertEquals(810, personalSummary.weekHomeTotal)
        assertEquals(160, personalSummary.weekSavings) // 810 - 650
        assertEquals(93, personalSummary.averagePerDay) // 650 / 7 = 92.8 -> 93
        assertEquals(DayOfWeek.WEDNESDAY, personalSummary.highestDay?.dayOfWeek)
        assertEquals(300, personalSummary.highestDay?.realTotal)
    }

    @Test
    fun `empty week computes zero totals and null highest day`() {
        val (startWeek, endWeek) = getWeekRange(2025, Calendar.NOVEMBER, 12)
        val expenses = emptyList<Expense>()

        val homeSummary = calculator.calculateHomeSummary(expenses, startWeek, endWeek, homeBalance = 15000)
        val personalSummary = calculator.calculatePersonalSummary(expenses, startWeek, endWeek, homeBalance = 15000, realBalance = 15000)

        assertEquals(0, homeSummary.weekHomeTotal)
        assertEquals(0, homeSummary.averagePerDay)
        assertNull(homeSummary.highestDay)

        assertEquals(0, personalSummary.weekRealTotal)
        assertEquals(0, personalSummary.weekSavings)
        assertNull(personalSummary.highestDay)
    }

    @Test
    fun `single-day week handles single day expenses correctly`() {
        val (startWeek, endWeek) = getWeekRange(2025, Calendar.NOVEMBER, 12)
        val days = DateUtils.getDaysOfWeekForRange(startWeek)
        val fridayMillis = days[4].second

        val expenses = listOf(
            Expense(id = 1, date = fridayMillis + 1000, realAmount = 500, homeAmount = 700, homeLabel = "Shopping")
        )

        val homeSummary = calculator.calculateHomeSummary(expenses, startWeek, endWeek, homeBalance = 14300)

        assertEquals(700, homeSummary.weekHomeTotal)
        assertEquals(100, homeSummary.averagePerDay) // 700 / 7 = 100
        assertEquals(DayOfWeek.FRIDAY, homeSummary.highestDay?.dayOfWeek)
        assertEquals(700, homeSummary.highestDay?.homeTotal)
    }

    @Test
    fun `week crossing month boundary groups across months correctly`() {
        // Week crossing October to November 2025: Mon 27 Oct 2025 to Sun 2 Nov 2025
        val (startWeek, endWeek) = getWeekRange(2025, Calendar.OCTOBER, 29)
        val days = DateUtils.getDaysOfWeekForRange(startWeek)

        val monOct27 = days[0].second // Oct 27
        val satNov1 = days[5].second  // Nov 1
        val sunNov2 = days[6].second  // Nov 2

        val expenses = listOf(
            Expense(id = 1, date = monOct27 + 3600000, realAmount = 100, homeAmount = 120, homeLabel = "Oct expense"),
            Expense(id = 2, date = satNov1 + 3600000, realAmount = 300, homeAmount = 350, homeLabel = "Nov expense 1"),
            Expense(id = 3, date = sunNov2 + 3600000, realAmount = 200, homeAmount = 250, homeLabel = "Nov expense 2")
        )

        val homeSummary = calculator.calculateHomeSummary(expenses, startWeek, endWeek, homeBalance = 5000)

        assertEquals(720, homeSummary.weekHomeTotal) // 120 + 350 + 250
        assertEquals(103, homeSummary.averagePerDay)
        assertEquals(DayOfWeek.SATURDAY, homeSummary.highestDay?.dayOfWeek)
        assertEquals(350, homeSummary.highestDay?.homeTotal)
    }

    @Test
    fun `highest-day tie behavior picks the earliest day in the week`() {
        val (startWeek, endWeek) = getWeekRange(2025, Calendar.NOVEMBER, 12)
        val days = DateUtils.getDaysOfWeekForRange(startWeek)

        val mondayMillis = days[0].second
        val fridayMillis = days[4].second

        // Both Monday and Friday have exact same highest home amount of 500
        val expenses = listOf(
            Expense(id = 1, date = mondayMillis + 1000, realAmount = 400, homeAmount = 500, homeLabel = "Mon lunch"),
            Expense(id = 2, date = fridayMillis + 1000, realAmount = 400, homeAmount = 500, homeLabel = "Fri dinner")
        )

        val homeSummary = calculator.calculateHomeSummary(expenses, startWeek, endWeek, homeBalance = 10000)
        val personalSummary = calculator.calculatePersonalSummary(expenses, startWeek, endWeek, homeBalance = 10000, realBalance = 10000)

        // Must pick Monday (earliest day)
        assertEquals("Tie must pick earliest day (Monday)", DayOfWeek.MONDAY, homeSummary.highestDay?.dayOfWeek)
        assertEquals("Tie must pick earliest day (Monday)", DayOfWeek.MONDAY, personalSummary.highestDay?.dayOfWeek)
    }

    @Test
    fun `savings is never affected by the budget in weekly calculations`() {
        val (startWeek, endWeek) = getWeekRange(2025, Calendar.NOVEMBER, 12)
        val days = DateUtils.getDaysOfWeekForRange(startWeek)
        val monMillis = days[0].second

        val expenses = listOf(
            Expense(id = 1, date = monMillis + 1000, realAmount = 300, homeAmount = 500, homeLabel = "Fuel")
        )

        val summaryBudget10k = calculator.calculatePersonalSummary(expenses, startWeek, endWeek, homeBalance = 9500, realBalance = 9700)
        val summaryBudget50k = calculator.calculatePersonalSummary(expenses, startWeek, endWeek, homeBalance = 49500, realBalance = 49700)
        val summaryBudgetZero = calculator.calculatePersonalSummary(expenses, startWeek, endWeek, homeBalance = -500, realBalance = -300)

        val expectedSavings = 500 - 300 // 200

        assertEquals(expectedSavings, summaryBudget10k.weekSavings)
        assertEquals(expectedSavings, summaryBudget50k.weekSavings)
        assertEquals(expectedSavings, summaryBudgetZero.weekSavings)
    }
}
