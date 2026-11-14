package com.daykhata.app.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daykhata.app.data.repository.ExpenseRepository
import com.daykhata.app.data.repository.SettingsRepository
import com.daykhata.app.domain.AppModeState
import com.daykhata.app.domain.SavingsCalculator
import com.daykhata.app.domain.model.SavingsSummary
import com.daykhata.app.domain.model.SavingsTrendPoint
import com.daykhata.app.domain.model.TrendRange
import com.daykhata.app.domain.model.ViewMode
import com.daykhata.app.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class SavingsUiState(
    val summary: SavingsSummary? = null,
    val trendPoints: List<SavingsTrendPoint> = emptyList(),
    val selectedRange: TrendRange = TrendRange.LAST_7_DAYS,
    val isPersonalMode: Boolean = false,
    val isLoading: Boolean = true
)

sealed interface SavingsEvent {
    data object ExitToDashboard : SavingsEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class SavingsViewModel(
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val savingsCalculator: SavingsCalculator,
    private val appModeState: AppModeState
) : ViewModel() {

    private val selectedRangeFlow = MutableStateFlow(TrendRange.LAST_7_DAYS)

    private val _events = MutableSharedFlow<SavingsEvent>()
    val events: SharedFlow<SavingsEvent> = _events.asSharedFlow()

    val uiState: StateFlow<SavingsUiState> = combine(
        selectedRangeFlow,
        appModeState.mode,
        settingsRepository.settings
    ) { range, mode, settings ->
        SavingsParams(range, mode, settings)
    }.flatMapLatest { params ->
        val (periodStart, periodEnd) = DateUtils.calculatePeriodRange(params.settings.periodType)

        combine(
            expenseRepository.getAllExpenses(),
            expenseRepository.getExpensesByPeriod(periodStart, periodEnd)
        ) { allExpenses, periodExpenses ->
            val summary = savingsCalculator.calculateSavingsSummary(
                allExpenses = allExpenses,
                periodExpenses = periodExpenses,
                settings = params.settings
            )

            val trendPoints = savingsCalculator.calculateTrendSeries(
                allExpenses = allExpenses,
                range = params.range
            )

            SavingsUiState(
                summary = summary,
                trendPoints = trendPoints,
                selectedRange = params.range,
                isPersonalMode = params.mode == ViewMode.PERSONAL,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SavingsUiState()
    )

    fun onRangeSelected(range: TrendRange) {
        selectedRangeFlow.value = range
    }

    private data class SavingsParams(
        val range: TrendRange,
        val mode: ViewMode,
        val settings: com.daykhata.app.domain.model.Settings
    )

    class Factory(
        private val expenseRepository: ExpenseRepository,
        private val settingsRepository: SettingsRepository,
        private val savingsCalculator: SavingsCalculator,
        private val appModeState: AppModeState
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SavingsViewModel(
                expenseRepository,
                settingsRepository,
                savingsCalculator,
                appModeState
            ) as T
        }
    }
}
