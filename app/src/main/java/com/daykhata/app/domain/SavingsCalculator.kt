package com.daykhata.app.domain

import com.daykhata.app.data.local.Expense
import com.daykhata.app.domain.model.GoalPeriod
import com.daykhata.app.domain.model.SavingsGoal
import com.daykhata.app.domain.model.SavingsGoalProgress
import com.daykhata.app.domain.model.SavingsSummary
import com.daykhata.app.domain.model.SavingsTrendPoint
import com.daykhata.app.domain.model.Settings
import com.daykhata.app.domain.model.TrendRange
import com.daykhata.app.util.DateUtils
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pure Kotlin calculator for private savings metrics, goal progress, and trends.
 * STRICTLY PERSONAL-FACING: Never pass these outputs into home-facing code.
 */
class SavingsCalculator {

    fun calculateSavingsSummary(
        allExpenses: List<Expense>,
        periodExpenses: List<Expense>,
        settings: Settings,
        clockMillis: Long = System.currentTimeMillis()
    ): SavingsSummary {
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(clockMillis).atZone(zone).toLocalDate()

        // 1. Current Period Savings
        val currentPeriodSavings = periodExpenses.sumOf { it.homeAmount - it.realAmount }

        // 2. All-Time Savings
        val allTimeSavings = allExpenses.sumOf { it.homeAmount - it.realAmount }

        // 3. This Week's Savings
        val (weekStart, weekEnd) = DateUtils.getWeekRangeForDate(clockMillis)
        val thisWeekExpenses = allExpenses.filter { it.date in weekStart..weekEnd }
        val thisWeekSavings = thisWeekExpenses.sumOf { it.homeAmount - it.realAmount }

        // 4. This Month's Savings
        val firstDayOfMonth = now.with(TemporalAdjusters.firstDayOfMonth())
        val lastDayOfMonth = now.with(TemporalAdjusters.lastDayOfMonth())
        val monthStart = firstDayOfMonth.atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEnd = lastDayOfMonth.plusDays(1).atStartOfDay(zone).minusNanos(1_000_000).toInstant().toEpochMilli()
        val thisMonthExpenses = allExpenses.filter { it.date in monthStart..monthEnd }
        val thisMonthSavings = thisMonthExpenses.sumOf { it.homeAmount - it.realAmount }

        // 5. Profitable Savings Statistics
        val profitableExpenses = allExpenses.filter { it.homeAmount > it.realAmount }
        val profitableCount = profitableExpenses.size
        val totalProfitableSavings = profitableExpenses.sumOf { it.homeAmount - it.realAmount }
        val averageProfitable = if (profitableCount > 0) {
            (totalProfitableSavings.toFloat() / profitableCount).roundToInt()
        } else {
            0
        }

        // 6. Negative Savings (Home < Real)
        val negativeExpenses = allExpenses.filter { it.homeAmount < it.realAmount }
        val hasNegative = negativeExpenses.isNotEmpty()
        val negativeCount = negativeExpenses.size
        val totalNegativeAmount = negativeExpenses.sumOf { it.realAmount - it.homeAmount }

        // 7. Goal Progress
        val goalProgress = calculateGoalProgress(
            goal = settings.goal,
            thisWeekSavings = thisWeekSavings,
            thisMonthSavings = thisMonthSavings
        )

        return SavingsSummary(
            currentPeriodSavings = currentPeriodSavings,
            allTimeSavings = allTimeSavings,
            thisWeekSavings = thisWeekSavings,
            thisMonthSavings = thisMonthSavings,
            averageSavingsPerProfitableExpense = averageProfitable,
            profitableExpensesCount = profitableCount,
            hasNegativeSavingsExpenses = hasNegative,
            negativeSavingsCount = negativeCount,
            totalNegativeSavingsAmount = totalNegativeAmount,
            goalProgress = goalProgress
        )
    }

    fun calculateGoalProgress(
        goal: SavingsGoal,
        thisWeekSavings: Int,
        thisMonthSavings: Int
    ): SavingsGoalProgress? {
        if (!goal.isSet) return null

        val savedAmount = when (goal.period) {
            GoalPeriod.WEEKLY -> thisWeekSavings
            GoalPeriod.MONTHLY -> thisMonthSavings
            GoalPeriod.NONE -> 0
        }

        val progressRatio = if (goal.amount > 0) {
            (savedAmount.toFloat() / goal.amount.toFloat()).coerceAtLeast(0f)
        } else {
            0f
        }

        val remaining = (goal.amount - savedAmount).coerceAtLeast(0)
        val isReached = savedAmount >= goal.amount
        val extra = if (isReached) savedAmount - goal.amount else 0

        return SavingsGoalProgress(
            goal = goal,
            savedAmount = savedAmount,
            remainingAmount = remaining,
            progressRatio = progressRatio,
            isGoalReached = isReached,
            extraSaved = extra
        )
    }

    fun calculateTrendSeries(
        allExpenses: List<Expense>,
        range: TrendRange,
        clockMillis: Long = System.currentTimeMillis()
    ): List<SavingsTrendPoint> {
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(clockMillis).atZone(zone).toLocalDate()

        return when (range) {
            TrendRange.LAST_7_DAYS -> {
                // 7 consecutive days up to today
                val points = mutableListOf<SavingsTrendPoint>()
                var cumulative = 0
                for (i in 6 downTo 0) {
                    val date = now.minusDays(i.toLong())
                    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = date.plusDays(1).atStartOfDay(zone).minusNanos(1_000_000).toInstant().toEpochMilli()
                    val dayExpenses = allExpenses.filter { it.date in start..end }
                    val daySavings = dayExpenses.sumOf { it.homeAmount - it.realAmount }
                    cumulative += daySavings
                    val label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    points.add(SavingsTrendPoint(label, start, end, daySavings, cumulative))
                }
                points
            }
            TrendRange.LAST_4_WEEKS -> {
                // 4 weekly buckets ending with current week
                val currentMonday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val points = mutableListOf<SavingsTrendPoint>()
                var cumulative = 0
                for (i in 3 downTo 0) {
                    val monday = currentMonday.minusWeeks(i.toLong())
                    val sunday = monday.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    val start = monday.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = sunday.plusDays(1).atStartOfDay(zone).minusNanos(1_000_000).toInstant().toEpochMilli()
                    val weekExpenses = allExpenses.filter { it.date in start..end }
                    val weekSavings = weekExpenses.sumOf { it.homeAmount - it.realAmount }
                    cumulative += weekSavings
                    val label = if (i == 0) "This Wk" else "W-${i}"
                    points.add(SavingsTrendPoint(label, start, end, weekSavings, cumulative))
                }
                points
            }
            TrendRange.LAST_6_MONTHS -> {
                // 6 monthly buckets ending with current month
                val points = mutableListOf<SavingsTrendPoint>()
                var cumulative = 0
                for (i in 5 downTo 0) {
                    val targetMonth = now.minusMonths(i.toLong())
                    val firstDay = targetMonth.with(TemporalAdjusters.firstDayOfMonth())
                    val lastDay = targetMonth.with(TemporalAdjusters.lastDayOfMonth())
                    val start = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = lastDay.plusDays(1).atStartOfDay(zone).minusNanos(1_000_000).toInstant().toEpochMilli()
                    val monthExpenses = allExpenses.filter { it.date in start..end }
                    val monthSavings = monthExpenses.sumOf { it.homeAmount - it.realAmount }
                    cumulative += monthSavings
                    val label = firstDay.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    points.add(SavingsTrendPoint(label, start, end, monthSavings, cumulative))
                }
                points
            }
        }
    }
}
