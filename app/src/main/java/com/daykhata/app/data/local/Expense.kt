package com.daykhata.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an expense entry.
 *
 * Each expense stores two amounts and two labels:
 * - realAmount & personalLabel: Actual amount spent and private notes.
 * - homeAmount & homeLabel: Amount and label visible to family.
 */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val realAmount: Int,
    val homeAmount: Int,
    val homeLabel: String,
    val personalLabel: String = ""
) {
    companion object {
        /**
         * Creates an Expense ensuring that if homeAmount is null or blank,
         * it defaults to realAmount.
         */
        fun create(
            id: Long = 0,
            date: Long,
            realAmount: Int,
            homeAmount: Int? = null,
            homeLabel: String,
            personalLabel: String = ""
        ): Expense {
            val resolvedHomeAmount = homeAmount ?: realAmount
            val resolvedPersonalLabel = if (personalLabel.isBlank()) homeLabel else personalLabel
            return Expense(
                id = id,
                date = date,
                realAmount = realAmount,
                homeAmount = resolvedHomeAmount,
                homeLabel = homeLabel.trim(),
                personalLabel = resolvedPersonalLabel.trim()
            )
        }
    }
}
