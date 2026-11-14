package com.daykhata.app.ui.daily_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daykhata.app.data.local.Expense
import com.daykhata.app.data.repository.ExpenseRepository
import com.daykhata.app.domain.ExpenseCalculator
import com.daykhata.app.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DailyListUiState(
    val selectedDate: Long = System.currentTimeMillis(),
    val expenses: List<Expense> = emptyList(),
    val dayHomeTotal: Int = 0,
    val dayRealTotal: Int = 0,
    val daySavings: Int = 0,
    val isPersonalMode: Boolean = false,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class DailyListViewModel(
    private val expenseRepository: ExpenseRepository,
    private val calculator: ExpenseCalculator
) : ViewModel() {

    private val selectedDateFlow = MutableStateFlow(System.currentTimeMillis())
    private val isPersonalModeFlow = MutableStateFlow(false)

    val uiState: StateFlow<DailyListUiState> = combine(
        selectedDateFlow,
        isPersonalModeFlow
    ) { date, isPersonalMode ->
        Pair(date, isPersonalMode)
    }.flatMapLatest { (date, isPersonalMode) ->
        val startOfDay = DateUtils.getStartOfDay(date)
        val endOfDay = DateUtils.getEndOfDay(date)

        expenseRepository.getExpensesByDate(startOfDay, endOfDay).combine(
            MutableStateFlow(isPersonalMode)
        ) { expenses, mode ->
            val homeTotal = calculator.calculateTodayHomeTotal(expenses)
            val realTotal = expenses.sumOf { it.realAmount }
            val savings = homeTotal - realTotal

            DailyListUiState(
                selectedDate = date,
                expenses = expenses,
                dayHomeTotal = homeTotal,
                dayRealTotal = realTotal,
                daySavings = savings,
                isPersonalMode = mode,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyListUiState()
    )

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

    fun togglePersonalMode() {
        isPersonalModeFlow.value = !isPersonalModeFlow.value
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
        }
    }

    class Factory(
        private val expenseRepository: ExpenseRepository,
        private val calculator: ExpenseCalculator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DailyListViewModel(expenseRepository, calculator) as T
        }
    }
}
