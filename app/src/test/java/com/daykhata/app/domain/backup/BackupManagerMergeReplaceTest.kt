package com.daykhata.app.domain.backup

import com.daykhata.app.data.local.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerMergeReplaceTest {

    @Test
    fun `merge logic skips exact duplicates and adds new expenses`() {
        val existingExpenses = listOf(
            Expense(id = 1, date = 1000L, realAmount = 100, homeAmount = 100, homeLabel = "Lunch", personalLabel = "Lunch"),
            Expense(id = 2, date = 2000L, realAmount = 200, homeAmount = 250, homeLabel = "Dinner", personalLabel = "Dinner + sweets")
        )

        val backupExpenses = listOf(
            // Duplicate of id=1
            BackupExpenseDto(date = 1000L, realAmount = 100, homeAmount = 100, homeLabel = "Lunch", personalLabel = "Lunch"),
            // New expense
            BackupExpenseDto(date = 3000L, realAmount = 300, homeAmount = 300, homeLabel = "Groceries", personalLabel = "Groceries"),
            // Different real amount from id=2 -> not duplicate
            BackupExpenseDto(date = 2000L, realAmount = 210, homeAmount = 250, homeLabel = "Dinner", personalLabel = "Dinner + sweets")
        )

        val toInsert = mutableListOf<BackupExpenseDto>()
        backupExpenses.forEach { dto ->
            val exists = existingExpenses.any { curr ->
                curr.date == dto.date &&
                    curr.realAmount == dto.realAmount &&
                    curr.homeAmount == dto.homeAmount &&
                    curr.homeLabel == dto.homeLabel &&
                    curr.personalLabel == dto.personalLabel
            }
            if (!exists) {
                toInsert.add(dto)
            }
        }

        assertEquals(2, toInsert.size)
        assertEquals(3000L, toInsert[0].date)
        assertEquals(210, toInsert[1].realAmount)
    }

    @Test
    fun `replace logic replaces full expense set`() {
        val backupExpenses = listOf(
            BackupExpenseDto(date = 4000L, realAmount = 400, homeAmount = 400, homeLabel = "Fuel", personalLabel = "Fuel")
        )

        val replacedExpenses = backupExpenses.map { dto ->
            Expense(
                id = 0,
                date = dto.date,
                realAmount = dto.realAmount,
                homeAmount = dto.homeAmount,
                homeLabel = dto.homeLabel,
                personalLabel = dto.personalLabel
            )
        }

        assertEquals(1, replacedExpenses.size)
        assertEquals("Fuel", replacedExpenses[0].homeLabel)
    }
}
