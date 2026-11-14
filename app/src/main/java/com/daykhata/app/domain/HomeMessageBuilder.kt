package com.daykhata.app.domain

import com.daykhata.app.domain.model.HomeExpenseItem
import com.daykhata.app.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure Kotlin message generator for family-facing summaries.
 * Strictly operates on HomeExpenseItem to prevent any leakage of private amounts or labels.
 */
object HomeMessageBuilder {

    fun buildHomeMessage(
        dateMillis: Long,
        items: List<HomeExpenseItem>,
        homeBalance: Int
    ): String {
        val dateFormat = SimpleDateFormat("d MMM", Locale.ENGLISH)
        val dateString = dateFormat.format(Date(dateMillis))

        val header = "Daily Expenses – $dateString"
        val formattedBalance = CurrencyUtils.formatIndianRupees(homeBalance)

        if (items.isEmpty()) {
            return """
                |$header
                |No expenses logged today.
                |Balance left: $formattedBalance
            """.trimMargin()
        }

        val itemsLines = items.joinToString("\n") { item ->
            val formattedAmount = CurrencyUtils.formatIndianRupees(item.homeAmount)
            "${item.homeLabel} – $formattedAmount"
        }

        val totalToday = items.sumOf { it.homeAmount }
        val formattedTotalToday = CurrencyUtils.formatIndianRupees(totalToday)

        return """
            |$header
            |$itemsLines
            |Total today: $formattedTotalToday
            |Balance left: $formattedBalance
        """.trimMargin()
    }
}
