package com.daykhata.app.domain.model

enum class GoalPeriod {
    WEEKLY,
    MONTHLY,
    NONE
}

data class SavingsGoal(
    val amount: Int = 0,
    val period: GoalPeriod = GoalPeriod.NONE
) {
    val isSet: Boolean
        get() = amount > 0 && period != GoalPeriod.NONE
}

data class SavingsGoalProgress(
    val goal: SavingsGoal,
    val savedAmount: Int,
    val remainingAmount: Int,
    val progressRatio: Float, // 0.0f to 1.0f+
    val isGoalReached: Boolean,
    val extraSaved: Int
)

data class SavingsTrendPoint(
    val label: String,
    val startMillis: Long,
    val endMillis: Long,
    val savings: Int,
    val cumulativeSavings: Int
)

enum class TrendRange {
    LAST_7_DAYS,
    LAST_4_WEEKS,
    LAST_6_MONTHS
}

data class SavingsSummary(
    val currentPeriodSavings: Int,
    val allTimeSavings: Int,
    val thisWeekSavings: Int,
    val thisMonthSavings: Int,
    val averageSavingsPerProfitableExpense: Int,
    val profitableExpensesCount: Int,
    val hasNegativeSavingsExpenses: Boolean,
    val negativeSavingsCount: Int,
    val totalNegativeSavingsAmount: Int,
    val goalProgress: SavingsGoalProgress?
)
