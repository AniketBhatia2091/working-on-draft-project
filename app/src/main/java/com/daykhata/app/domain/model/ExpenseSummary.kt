package com.daykhata.app.domain.model

data class ExpenseSummary(
    val homeSpent: Int,
    val realSpent: Int,
    val homeBalance: Int,
    val realBalance: Int,
    val savings: Int,
    val cutOffReached: Boolean,
    val budget: Int,
    val cutOffLimit: Int
) {
    /**
     * Progress ratio of budget spent based on HOME expenses (0.0 to 1.0+).
     */
    val homeSpentRatio: Float
        get() = if (budget > 0) (homeSpent.toFloat() / budget.toFloat()).coerceAtLeast(0f) else 0f
}
