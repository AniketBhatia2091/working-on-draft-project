package com.daykhata.app.domain

import com.daykhata.app.data.local.Expense
import com.daykhata.app.domain.model.HomeExpenseItem
import com.daykhata.app.domain.model.toHomeExpenseItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HomeMessageBuilderTest {

    @Test
    fun `normal day builds correctly formatted message with Indian currency grouping`() {
        // Date: 14 Nov 2026
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 14)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }
        val dateMillis = calendar.timeInMillis

        val items = listOf(
            HomeExpenseItem(homeLabel = "Lunch", homeAmount = 150),
            HomeExpenseItem(homeLabel = "Auto", homeAmount = 80)
        )
        val homeBalance = 14770

        val message = HomeMessageBuilder.buildHomeMessage(dateMillis, items, homeBalance)

        val expected = """
            Daily Expenses – 14 Nov
            Lunch – ₹150
            Auto – ₹80
            Total today: ₹230
            Balance left: ₹14,770
        """.trimIndent()

        assertEquals(expected, message)
    }

    @Test
    fun `empty day builds message indicating no expenses logged today`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 14)
        }
        val dateMillis = calendar.timeInMillis

        val items = emptyList<HomeExpenseItem>()
        val homeBalance = 15000

        val message = HomeMessageBuilder.buildHomeMessage(dateMillis, items, homeBalance)

        val expected = """
            Daily Expenses – 14 Nov
            No expenses logged today.
            Balance left: ₹15,000
        """.trimIndent()

        assertEquals(expected, message)
    }

    @Test
    fun `large amounts are formatted with proper Indian number grouping`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 14)
        }
        val dateMillis = calendar.timeInMillis

        val items = listOf(
            HomeExpenseItem(homeLabel = "House Renovation", homeAmount = 105000),
            HomeExpenseItem(homeLabel = "Land Registry", homeAmount = 10000000)
        )
        val homeBalance = 2500000

        val message = HomeMessageBuilder.buildHomeMessage(dateMillis, items, homeBalance)

        assertTrue(message.contains("House Renovation – ₹1,05,000"))
        assertTrue(message.contains("Land Registry – ₹1,00,00,000"))
        assertTrue(message.contains("Total today: ₹1,01,05,000"))
        assertTrue(message.contains("Balance left: ₹25,00,000"))
    }

    @Test
    fun `output strictly contains no real amount or personal label`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 14)
        }
        val dateMillis = calendar.timeInMillis

        // Expenses where realAmount and personalLabel differ substantially from home amounts
        val expenses = listOf(
            Expense(
                id = 1,
                date = dateMillis,
                realAmount = 850,
                homeAmount = 300,
                homeLabel = "Groceries",
                personalLabel = "Groceries plus private cigarettes and beer"
            ),
            Expense(
                id = 2,
                date = dateMillis,
                realAmount = 1200,
                homeAmount = 400,
                homeLabel = "Fuel",
                personalLabel = "Personal long drive with secret friends"
            )
        )

        // Map to safe home items
        val homeItems = expenses.map { it.toHomeExpenseItem() }
        val homeBalance = 8000

        val message = HomeMessageBuilder.buildHomeMessage(dateMillis, homeItems, homeBalance)

        // Home data must be present
        assertTrue(message.contains("Groceries – ₹300"))
        assertTrue(message.contains("Fuel – ₹400"))
        assertTrue(message.contains("Total today: ₹700"))

        // Real amounts must NOT be present
        assertFalse("Message must not leak realAmount 850", message.contains("850"))
        assertFalse("Message must not leak realAmount 1200", message.contains("1200"))
        assertFalse("Message must not leak real total 2050", message.contains("2050"))

        // Personal labels must NOT be present
        assertFalse("Message must not leak cigarettes", message.contains("cigarettes"))
        assertFalse("Message must not leak beer", message.contains("beer"))
        assertFalse("Message must not leak secret friends", message.contains("secret friends"))
        assertFalse("Message must not leak long drive", message.contains("long drive"))
    }
}
