package com.daykhata.app.ui.daily_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daykhata.app.data.local.Expense
import com.daykhata.app.data.repository.ExpenseRepository
import com.daykhata.app.data.repository.SettingsRepository
import com.daykhata.app.domain.AppModeState
import com.daykhata.app.domain.ExpenseCalculator
import com.daykhata.app.domain.HomeMessageBuilder
import com.daykhata.app.domain.model.FamilyContact
import com.daykhata.app.domain.model.HomeExpenseItem
import com.daykhata.app.domain.model.ViewMode
import com.daykhata.app.domain.model.toHomeExpenseItem
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DailyListUiState(
    val selectedDate: Long = System.currentTimeMillis(),
    val expenses: List<Expense> = emptyList(),
    val homeItems: List<HomeExpenseItem> = emptyList(),
    val dayHomeTotal: Int = 0,
    val dayRealTotal: Int = 0,
    val daySavings: Int = 0,
    val homeBalance: Int = 0,
    val viewMode: ViewMode = ViewMode.HOME,
    val familyContacts: List<FamilyContact> = emptyList(),
    val shareMessage: String = "",
    val isShareSheetVisible: Boolean = false,
    val isLoading: Boolean = true
) {
    val isPersonalMode: Boolean
        get() = viewMode == ViewMode.PERSONAL
}

sealed interface DailyListEvent {
    data class ShowSnackbar(val message: String) : DailyListEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class DailyListViewModel(
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val calculator: ExpenseCalculator,
    private val appModeState: AppModeState
) : ViewModel() {

    private val selectedDateFlow = MutableStateFlow(System.currentTimeMillis())
    private val isShareSheetVisibleFlow = MutableStateFlow(false)

    private val _events = MutableSharedFlow<DailyListEvent>()
    val events: SharedFlow<DailyListEvent> = _events.asSharedFlow()

    val uiState: StateFlow<DailyListUiState> = combine(
        selectedDateFlow,
        appModeState.mode,
        settingsRepository.settings,
        settingsRepository.familyContacts,
        isShareSheetVisibleFlow
    ) { date, mode, settings, contacts, isShareSheetVisible ->
        DailyStateParams(date, mode, settings, contacts, isShareSheetVisible)
    }.flatMapLatest { params ->
        val startOfDay = DateUtils.getStartOfDay(params.date)
        val endOfDay = DateUtils.getEndOfDay(params.date)
        val (periodStart, periodEnd) = DateUtils.calculatePeriodRange(params.settings.periodType)

        combine(
            expenseRepository.getExpensesByDate(startOfDay, endOfDay),
            expenseRepository.getExpensesByPeriod(periodStart, periodEnd)
        ) { dayExpenses, periodExpenses ->
            val periodSummary = calculator.calculate(periodExpenses, params.settings)
            val homeItems = dayExpenses.map { it.toHomeExpenseItem() }
            val homeTotal = calculator.calculateTodayHomeTotal(dayExpenses)
            val realTotal = dayExpenses.sumOf { it.realAmount }
            val savings = homeTotal - realTotal

            // Message is built strictly from HomeExpenseItem and home balance
            val shareMessage = HomeMessageBuilder.buildHomeMessage(
                dateMillis = params.date,
                items = homeItems,
                homeBalance = periodSummary.homeBalance
            )

            DailyListUiState(
                selectedDate = params.date,
                expenses = dayExpenses,
                homeItems = homeItems,
                dayHomeTotal = homeTotal,
                dayRealTotal = realTotal,
                daySavings = savings,
                homeBalance = periodSummary.homeBalance,
                viewMode = params.mode,
                familyContacts = params.contacts,
                shareMessage = shareMessage,
                isShareSheetVisible = params.isShareSheetVisible,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyListUiState()
    )

    fun setViewMode(mode: ViewMode) {
        appModeState.setMode(mode)
    }

    fun onDateSelected(epochMillis: Long) {
        selectedDateFlow.value = epochMillis
    }

    fun onPreviousDayClicked() {
        selectedDateFlow.value -= 24 * 60 * 60 * 1000L
    }

    fun onNextDayClicked() {
        selectedDateFlow.value += 24 * 60 * 60 * 1000L
    }

    fun onTodayClicked() {
        selectedDateFlow.value = System.currentTimeMillis()
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
        }
    }

    fun openShareSheet() {
        if (appModeState.isPersonalMode) {
            viewModelScope.launch {
                _events.emit(DailyListEvent.ShowSnackbar("Switch to Home mode to share expenses."))
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
            _events.emit(DailyListEvent.ShowSnackbar("Message copied to clipboard."))
        }
    }

    private data class DailyStateParams(
        val date: Long,
        val mode: ViewMode,
        val settings: com.daykhata.app.domain.model.Settings,
        val contacts: List<FamilyContact>,
        val isShareSheetVisible: Boolean
    )

    class Factory(
        private val expenseRepository: ExpenseRepository,
        private val settingsRepository: SettingsRepository,
        private val calculator: ExpenseCalculator,
        private val appModeState: AppModeState
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DailyListViewModel(
                expenseRepository,
                settingsRepository,
                calculator,
                appModeState
            ) as T
        }
    }
}
