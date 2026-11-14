package com.daykhata.app.ui.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daykhata.app.data.repository.ExpenseRepository
import com.daykhata.app.data.repository.SettingsRepository
import com.daykhata.app.domain.AppModeState
import com.daykhata.app.domain.ExpenseCalculator
import com.daykhata.app.domain.HomeWeeklyMessageBuilder
import com.daykhata.app.domain.WeeklySummaryCalculator
import com.daykhata.app.domain.model.FamilyContact
import com.daykhata.app.domain.model.HomeWeekChartData
import com.daykhata.app.domain.model.HomeWeekSummary
import com.daykhata.app.domain.model.PersonalWeekChartData
import com.daykhata.app.domain.model.PersonalWeekSummary
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
import kotlinx.coroutines.launch

data class WeeklyUiState(
    val weekRangeLabel: String = "",
    val startOfWeekMillis: Long = 0L,
    val endOfWeekMillis: Long = 0L,
    val isCurrentWeek: Boolean = true,
    val viewMode: ViewMode = ViewMode.HOME,
    val homeSummary: HomeWeekSummary? = null,
    val personalSummary: PersonalWeekSummary? = null,
    val homeChartData: HomeWeekChartData? = null,
    val personalChartData: PersonalWeekChartData? = null,
    val familyContacts: List<FamilyContact> = emptyList(),
    val shareMessage: String = "",
    val isShareSheetVisible: Boolean = false,
    val isLoading: Boolean = true
) {
    val isPersonalMode: Boolean
        get() = viewMode == ViewMode.PERSONAL
}

sealed interface WeeklyEvent {
    data class ShowSnackbar(val message: String) : WeeklyEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyViewModel(
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val expenseCalculator: ExpenseCalculator,
    private val weeklyCalculator: WeeklySummaryCalculator,
    private val appModeState: AppModeState
) : ViewModel() {

    private val currentWeekRange = DateUtils.getWeekRangeForDate(System.currentTimeMillis())
    private val selectedWeekStartFlow = MutableStateFlow(currentWeekRange.first)
    private val isShareSheetVisibleFlow = MutableStateFlow(false)

    private val _events = MutableSharedFlow<WeeklyEvent>()
    val events: SharedFlow<WeeklyEvent> = _events.asSharedFlow()

    val uiState: StateFlow<WeeklyUiState> = combine(
        selectedWeekStartFlow,
        appModeState.mode,
        settingsRepository.settings,
        settingsRepository.familyContacts,
        isShareSheetVisibleFlow
    ) { startMillis, mode, settings, contacts, isShareSheetVisible ->
        val (_, endMillis) = DateUtils.getWeekRangeForDate(startMillis)
        WeeklyParams(startMillis, endMillis, mode, settings, contacts, isShareSheetVisible)
    }.flatMapLatest { params ->
        val (periodStart, periodEnd) = DateUtils.calculatePeriodRange(params.settings.periodType)

        combine(
            expenseRepository.getExpensesByPeriod(params.startMillis, params.endMillis),
            expenseRepository.getExpensesByPeriod(periodStart, periodEnd)
        ) { weekExpenses, periodExpenses ->
            val periodSummary = expenseCalculator.calculate(periodExpenses, params.settings)

            val homeSummary = weeklyCalculator.calculateHomeSummary(
                expenses = weekExpenses,
                startOfWeekMillis = params.startMillis,
                endOfWeekMillis = params.endMillis,
                homeBalance = periodSummary.homeBalance
            )

            val personalSummary = weeklyCalculator.calculatePersonalSummary(
                expenses = weekExpenses,
                startOfWeekMillis = params.startMillis,
                endOfWeekMillis = params.endMillis,
                homeBalance = periodSummary.homeBalance,
                realBalance = periodSummary.realBalance
            )

            val homeChartData = weeklyCalculator.buildHomeChartData(homeSummary)
            val personalChartData = weeklyCalculator.buildPersonalChartData(personalSummary)

            val weekRangeWithTo = DateUtils.formatWeekRangeWithTo(params.startMillis, params.endMillis)
            val shareMessage = HomeWeeklyMessageBuilder.buildHomeWeeklyMessage(
                weekRange = weekRangeWithTo,
                dayTotals = homeSummary.dayTotals,
                weekTotal = homeSummary.weekHomeTotal,
                homeBalance = periodSummary.homeBalance
            )

            val isCurrentWeek = DateUtils.isCurrentWeek(params.startMillis, params.endMillis)

            WeeklyUiState(
                weekRangeLabel = homeSummary.weekRangeLabel,
                startOfWeekMillis = params.startMillis,
                endOfWeekMillis = params.endMillis,
                isCurrentWeek = isCurrentWeek,
                viewMode = params.mode,
                homeSummary = homeSummary,
                personalSummary = personalSummary,
                homeChartData = homeChartData,
                personalChartData = personalChartData,
                familyContacts = params.contacts,
                shareMessage = shareMessage,
                isShareSheetVisible = params.isShareSheetVisible,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeeklyUiState()
    )

    fun setViewMode(mode: ViewMode) {
        appModeState.setMode(mode)
    }

    fun onPreviousWeekClicked() {
        val currentStart = selectedWeekStartFlow.value
        val (newStart, _) = DateUtils.getWeekRangeForDate(currentStart - 7 * 24 * 60 * 60 * 1000L)
        selectedWeekStartFlow.value = newStart
    }

    fun onNextWeekClicked() {
        val currentStart = selectedWeekStartFlow.value
        val (_, currentEnd) = DateUtils.getWeekRangeForDate(currentStart)
        if (DateUtils.isCurrentWeek(currentStart, currentEnd)) {
            return // Do not allow navigating past current week
        }
        val (newStart, _) = DateUtils.getWeekRangeForDate(currentStart + 7 * 24 * 60 * 60 * 1000L)
        val now = System.currentTimeMillis()
        if (newStart > now) {
            return
        }
        selectedWeekStartFlow.value = newStart
    }

    fun onThisWeekClicked() {
        val (currentStart, _) = DateUtils.getWeekRangeForDate(System.currentTimeMillis())
        selectedWeekStartFlow.value = currentStart
    }

    fun openShareSheet() {
        if (appModeState.isPersonalMode) {
            viewModelScope.launch {
                _events.emit(WeeklyEvent.ShowSnackbar("Switch to Home mode to share weekly expenses."))
            }
            return
        }
        isShareSheetVisibleFlow.value = true
    }

    fun dismissShareSheet() {
        isShareSheetVisibleFlow.value = false
    }

    fun onMessageCopied() {
        viewModelScope.launch {
            _events.emit(WeeklyEvent.ShowSnackbar("Weekly summary copied to clipboard."))
        }
    }

    private data class WeeklyParams(
        val startMillis: Long,
        val endMillis: Long,
        val mode: ViewMode,
        val settings: com.daykhata.app.domain.model.Settings,
        val contacts: List<FamilyContact>,
        val isShareSheetVisible: Boolean
    )

    class Factory(
        private val expenseRepository: ExpenseRepository,
        private val settingsRepository: SettingsRepository,
        private val expenseCalculator: ExpenseCalculator,
        private val weeklyCalculator: WeeklySummaryCalculator,
        private val appModeState: AppModeState
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WeeklyViewModel(
                expenseRepository,
                settingsRepository,
                expenseCalculator,
                weeklyCalculator,
                appModeState
            ) as T
        }
    }
}
