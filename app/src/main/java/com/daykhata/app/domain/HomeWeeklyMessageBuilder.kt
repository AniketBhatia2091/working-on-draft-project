package com.daykhata.app.domain

import com.daykhata.app.domain.model.HomeDayTotal
import com.daykhata.app.util.CurrencyUtils

/**
 * Pure Kotlin message generator for family weekly summaries.
 * Strictly operates on HomeDayTotal models containing only home numbers.
 */
object HomeWeeklyMessageBuilder {

    fun buildHomeWeeklyMessage(
        weekRange: String,
        dayTotals: List<HomeDayTotal>,
        weekTotal: Int,
        homeBalance: Int
    ): String {
        val header = "Weekly Expenses – $weekRange"
        val formattedBalance = CurrencyUtils.formatIndianRupees(homeBalance)
        val formattedWeekTotal = CurrencyUtils.formatIndianRupees(weekTotal)

        if (weekTotal == 0 || dayTotals.all { it.homeTotal == 0 }) {
            return """
                |$header
                |No expenses logged this week.
                |Balance left: $formattedBalance
            """.trimMargin()
        }

        val daysLines = dayTotals.joinToString("\n") { day ->
            "${day.dayLabel} – ${CurrencyUtils.formatIndianRupees(day.homeTotal)}"
        }

        return """
            |$header
            |$daysLines
            |Week total: $formattedWeekTotal
            |Balance left: $formattedBalance
        """.trimMargin()
    }
}
