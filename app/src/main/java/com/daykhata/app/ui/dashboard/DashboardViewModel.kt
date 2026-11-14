package com.daykhata.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daykhata.app.data.local.Expense
import com.daykhata.app.data.repository.ExpenseRepository
import com.daykhata.app.data.repository.SettingsRepository
import com.daykhata.app.domain.AppModeState
import com.daykhata.app.domain.ExpenseCalculator
import com.daykhata.app.domain.model.ExpenseSummary
import com.daykhata.app.domain.model.Settings
import com.daykhata.app.domain.model.ViewMode
import com.daykhata.app.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val homeBalance: Int = 0,
    val homeSpent: Int = 0,
    val budget: Int = 15000,
    val cutOffLimit: Int = 2000,
    val cutOffReached: Boolean = false,
    val homeSpentRatio: Float = 0f,
    val todayHomeTotal: Int = 0,
    val realBalance: Int = 0,
    val realSpent: Int = 0,
    val savings: Int = 0,
    val isPersonalMode: Boolean = false,
    val recentExpenses: List<Expense> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val calculator: ExpenseCalculator,
    private val appModeState: AppModeState
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        settingsRepository.settings,
        appModeState.mode
    ) { settings, mode ->
        Pair(settings, mode == ViewMode.PERSONAL)
    }.flatMapLatest { (settings, isPersonalMode) ->
        val (periodStart, periodEnd) = DateUtils.calculatePeriodRange(settings.periodType)
        val todayStart = DateUtils.getStartOfDay(System.currentTimeMillis())
        val todayEnd = DateUtils.getEndOfDay(System.currentTimeMillis())

        combine(
            expenseRepository.getExpensesByPeriod(periodStart, periodEnd),
            expenseRepository.getExpensesByDate(todayStart, todayEnd)
        ) { periodExpenses, todayExpenses ->
            val summary = calculator.calculate(periodExpenses, settings)
            val todayHomeTotal = calculator.calculateTodayHomeTotal(todayExpenses)

            DashboardUiState(
                homeBalance = summary.homeBalance,
                homeSpent = summary.homeSpent,
                budget = summary.budget,
                cutOffLimit = summary.cutOffLimit,
                cutOffReached = summary.cutOffReached,
                homeSpentRatio = summary.homeSpentRatio,
                todayHomeTotal = todayHomeTotal,
                realBalance = summary.realBalance,
                realSpent = summary.realSpent,
                savings = summary.savings,
                isPersonalMode = isPersonalMode,
                recentExpenses = periodExpenses.take(5),
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun togglePersonalMode() {
        appModeState.toggleMode()
    }

    class Factory(
        private val expenseRepository: ExpenseRepository,
        private val settingsRepository: SettingsRepository,
        private val calculator: ExpenseCalculator,
        private val appModeState: AppModeState
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(expenseRepository, settingsRepository, calculator, appModeState) as T
        }
    }
}
