package com.daykhata.app.domain

import com.daykhata.app.data.local.Expense
import com.daykhata.app.domain.model.HomeBarData
import com.daykhata.app.domain.model.HomeDayTotal
import com.daykhata.app.domain.model.HomeWeekChartData
import com.daykhata.app.domain.model.HomeWeekSummary
import com.daykhata.app.domain.model.PersonalBarData
import com.daykhata.app.domain.model.PersonalDayTotal
import com.daykhata.app.domain.model.PersonalWeekChartData
import com.daykhata.app.domain.model.PersonalWeekSummary
import com.daykhata.app.util.DateUtils
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pure Kotlin calculator for weekly summaries and charts.
 * Operates strictly with separate Home and Personal models to guarantee type safety.
 */
class WeeklySummaryCalculator {

    fun calculateHomeSummary(
        expenses: List<Expense>,
        startOfWeekMillis: Long,
        endOfWeekMillis: Long,
        homeBalance: Int
    ): HomeWeekSummary {
        val days = DateUtils.getDaysOfWeekForRange(startOfWeekMillis)

        val dayTotals = days.map { (dayOfWeek, dayMillis) ->
            val startOfDay = DateUtils.getStartOfDay(dayMillis)
            val endOfDay = DateUtils.getEndOfDay(dayMillis)
            val dayExpenses = expenses.filter { it.date in startOfDay..endOfDay }
            val homeTotal = dayExpenses.sumOf { it.homeAmount }
            val label = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

            HomeDayTotal(
                dayOfWeek = dayOfWeek,
                dateMillis = dayMillis,
                dayLabel = label,
                homeTotal = homeTotal
            )
        }

        val weekHomeTotal = dayTotals.sumOf { it.homeTotal }
        val averagePerDay = (weekHomeTotal.toFloat() / 7f).roundToInt()

        // Highest spending day: picks earliest day on tie, null if all days are 0
        val highestDay = if (weekHomeTotal > 0) {
            dayTotals.maxByOrNull { it.homeTotal }
        } else {
            null
        }

        val weekRangeLabel = DateUtils.formatWeekRange(startOfWeekMillis, endOfWeekMillis)

        return HomeWeekSummary(
            weekRangeLabel = weekRangeLabel,
            startMillis = startOfWeekMillis,
            endMillis = endOfWeekMillis,
            dayTotals = dayTotals,
            weekHomeTotal = weekHomeTotal,
            averagePerDay = averagePerDay,
            highestDay = highestDay,
            homeBalance = homeBalance
        )
    }

    fun calculatePersonalSummary(
        expenses: List<Expense>,
        startOfWeekMillis: Long,
        endOfWeekMillis: Long,
        homeBalance: Int,
        realBalance: Int
    ): PersonalWeekSummary {
        val days = DateUtils.getDaysOfWeekForRange(startOfWeekMillis)

        val dayTotals = days.map { (dayOfWeek, dayMillis) ->
            val startOfDay = DateUtils.getStartOfDay(dayMillis)
            val endOfDay = DateUtils.getEndOfDay(dayMillis)
            val dayExpenses = expenses.filter { it.date in startOfDay..endOfDay }
            val realTotal = dayExpenses.sumOf { it.realAmount }
            val homeTotal = dayExpenses.sumOf { it.homeAmount }
            val savings = homeTotal - realTotal
            val label = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

            PersonalDayTotal(
                dayOfWeek = dayOfWeek,
                dateMillis = dayMillis,
                dayLabel = label,
                realTotal = realTotal,
                homeTotal = homeTotal,
                savings = savings
            )
        }

        val weekRealTotal = dayTotals.sumOf { it.realTotal }
        val weekHomeTotal = dayTotals.sumOf { it.homeTotal }
        val weekSavings = weekHomeTotal - weekRealTotal
        val averagePerDay = (weekRealTotal.toFloat() / 7f).roundToInt()

        // Highest spending day (real): picks earliest day on tie, null if all days are 0
        val highestDay = if (weekRealTotal > 0) {
            dayTotals.maxByOrNull { it.realTotal }
        } else {
            null
        }

        val weekRangeLabel = DateUtils.formatWeekRange(startOfWeekMillis, endOfWeekMillis)

        return PersonalWeekSummary(
            weekRangeLabel = weekRangeLabel,
            startMillis = startOfWeekMillis,
            endMillis = endOfWeekMillis,
            dayTotals = dayTotals,
            weekRealTotal = weekRealTotal,
            weekHomeTotal = weekHomeTotal,
            weekSavings = weekSavings,
            averagePerDay = averagePerDay,
            highestDay = highestDay,
            homeBalance = homeBalance,
            realBalance = realBalance
        )
    }

    fun buildHomeChartData(homeSummary: HomeWeekSummary): HomeWeekChartData {
        val bars = homeSummary.dayTotals.map { day ->
            HomeBarData(
                dayLabel = day.dayLabel,
                homeAmount = day.homeTotal
            )
        }
        val maxAmount = bars.maxOfOrNull { it.homeAmount } ?: 0
        return HomeWeekChartData(bars = bars, maxAmount = maxAmount)
    }

    fun buildPersonalChartData(personalSummary: PersonalWeekSummary): PersonalWeekChartData {
        val bars = personalSummary.dayTotals.map { day ->
            PersonalBarData(
                dayLabel = day.dayLabel,
                realAmount = day.realTotal,
                homeAmount = day.homeTotal,
                savings = day.savings
            )
        }
        val maxAmount = bars.maxOfOrNull { maxOf(it.homeAmount, it.realAmount) } ?: 0
        return PersonalWeekChartData(bars = bars, maxAmount = maxAmount)
    }
}
