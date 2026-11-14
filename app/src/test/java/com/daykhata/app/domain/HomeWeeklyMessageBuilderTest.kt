package com.daykhata.app.domain

import com.daykhata.app.domain.model.HomeDayTotal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class HomeWeeklyMessageBuilderTest {

    @Test
    fun `normal week builds properly formatted message with Indian currency grouping`() {
        val weekRange = "10 Nov to 16 Nov"
        val dayTotals = listOf(
            HomeDayTotal(DayOfWeek.MONDAY, 1000L, "Mon", 230),
            HomeDayTotal(DayOfWeek.TUESDAY, 2000L, "Tue", 180),
            HomeDayTotal(DayOfWeek.WEDNESDAY, 3000L, "Wed", 350),
            HomeDayTotal(DayOfWeek.THURSDAY, 4000L, "Thu", 120),
            HomeDayTotal(DayOfWeek.FRIDAY, 5000L, "Fri", 250),
            HomeDayTotal(DayOfWeek.SATURDAY, 6000L, "Sat", 200),
            HomeDayTotal(DayOfWeek.SUNDAY, 7000L, "Sun", 120)
        )
        val weekTotal = 1450
        val homeBalance = 13550

        val message = HomeWeeklyMessageBuilder.buildHomeWeeklyMessage(
            weekRange = weekRange,
            dayTotals = dayTotals,
            weekTotal = weekTotal,
            homeBalance = homeBalance
        )

        val expected = """
            Weekly Expenses – 10 Nov to 16 Nov
            Mon – ₹230
            Tue – ₹180
            Wed – ₹350
            Thu – ₹120
            Fri – ₹250
            Sat – ₹200
            Sun – ₹120
            Week total: ₹1,450
            Balance left: ₹13,550
        """.trimIndent()

        assertEquals(expected, message)
    }

    @Test
    fun `empty week builds message indicating no expenses this week`() {
        val weekRange = "10 Nov to 16 Nov"
        val dayTotals = listOf(
            HomeDayTotal(DayOfWeek.MONDAY, 1000L, "Mon", 0),
            HomeDayTotal(DayOfWeek.TUESDAY, 2000L, "Tue", 0),
            HomeDayTotal(DayOfWeek.WEDNESDAY, 3000L, "Wed", 0),
            HomeDayTotal(DayOfWeek.THURSDAY, 4000L, "Thu", 0),
            HomeDayTotal(DayOfWeek.FRIDAY, 5000L, "Fri", 0),
            HomeDayTotal(DayOfWeek.SATURDAY, 6000L, "Sat", 0),
            HomeDayTotal(DayOfWeek.SUNDAY, 7000L, "Sun", 0)
        )
        val weekTotal = 0
        val homeBalance = 15000

        val message = HomeWeeklyMessageBuilder.buildHomeWeeklyMessage(
            weekRange = weekRange,
            dayTotals = dayTotals,
            weekTotal = weekTotal,
            homeBalance = homeBalance
        )

        val expected = """
            Weekly Expenses – 10 Nov to 16 Nov
            No expenses logged this week.
            Balance left: ₹15,000
        """.trimIndent()

        assertEquals(expected, message)
    }

    @Test
    fun `leak test proves weekly message never contains real amounts or private text`() {
        val weekRange = "10 Nov to 16 Nov"

        // Suppose real spent is 950 on Mon, but home spent is 250
        val dayTotals = listOf(
            HomeDayTotal(DayOfWeek.MONDAY, 1000L, "Mon", 250),
            HomeDayTotal(DayOfWeek.TUESDAY, 2000L, "Tue", 150),
            HomeDayTotal(DayOfWeek.WEDNESDAY, 3000L, "Wed", 0),
            HomeDayTotal(DayOfWeek.THURSDAY, 4000L, "Thu", 0),
            HomeDayTotal(DayOfWeek.FRIDAY, 5000L, "Fri", 0),
            HomeDayTotal(DayOfWeek.SATURDAY, 6000L, "Sat", 0),
            HomeDayTotal(DayOfWeek.SUNDAY, 7000L, "Sun", 0)
        )
        val weekTotal = 400
        val homeBalance = 14600

        val message = HomeWeeklyMessageBuilder.buildHomeWeeklyMessage(
            weekRange = weekRange,
            dayTotals = dayTotals,
            weekTotal = weekTotal,
            homeBalance = homeBalance
        )

        // Verify home values are present
        assertTrue(message.contains("Mon – ₹250"))
        assertTrue(message.contains("Tue – ₹150"))
        assertTrue(message.contains("Week total: ₹400"))
        assertTrue(message.contains("Balance left: ₹14,600"))

        // Verify private data is absent
        assertFalse("Must not leak real amount 950", message.contains("950"))
        assertFalse("Must not leak real balance", message.contains("12000"))
        assertFalse("Must not leak personal notes", message.contains("private"))
        assertFalse("Must not leak savings calculation explicitly", message.contains("savings"))
    }
}
