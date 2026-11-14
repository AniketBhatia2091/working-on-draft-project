package com.daykhata.app.domain.model

import java.time.DayOfWeek

/**
 * Isolated model representing home-facing daily total in a week.
 */
data class HomeDayTotal(
    val dayOfWeek: DayOfWeek,
    val dateMillis: Long,
    val dayLabel: String, // e.g. "Mon"
    val homeTotal: Int
)

/**
 * Model representing private personal daily total in a week.
 */
data class PersonalDayTotal(
    val dayOfWeek: DayOfWeek,
    val dateMillis: Long,
    val dayLabel: String,
    val realTotal: Int,
    val homeTotal: Int,
    val savings: Int
)

/**
 * Safe home-only weekly summary for family mode.
 */
data class HomeWeekSummary(
    val weekRangeLabel: String,
    val startMillis: Long,
    val endMillis: Long,
    val dayTotals: List<HomeDayTotal>,
    val weekHomeTotal: Int,
    val averagePerDay: Int,
    val highestDay: HomeDayTotal?,
    val homeBalance: Int
)

/**
 * Private weekly summary for personal mode.
 */
data class PersonalWeekSummary(
    val weekRangeLabel: String,
    val startMillis: Long,
    val endMillis: Long,
    val dayTotals: List<PersonalDayTotal>,
    val weekRealTotal: Int,
    val weekHomeTotal: Int,
    val weekSavings: Int,
    val averagePerDay: Int,
    val highestDay: PersonalDayTotal?,
    val homeBalance: Int,
    val realBalance: Int
)

/**
 * Isolated chart models guaranteeing type safety:
 * HomeWeekChartData contains strictly home amounts.
 */
data class HomeBarData(
    val dayLabel: String,
    val homeAmount: Int
)

data class HomeWeekChartData(
    val bars: List<HomeBarData>,
    val maxAmount: Int
)

data class PersonalBarData(
    val dayLabel: String,
    val realAmount: Int,
    val homeAmount: Int,
    val savings: Int
)

data class PersonalWeekChartData(
    val bars: List<PersonalBarData>,
    val maxAmount: Int
)
