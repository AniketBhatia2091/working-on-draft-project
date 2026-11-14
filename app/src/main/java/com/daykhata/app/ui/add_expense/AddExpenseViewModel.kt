package com.daykhata.app.ui.add_expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daykhata.app.data.local.Expense
import com.daykhata.app.data.repository.ExpenseRepository
import com.daykhata.app.data.repository.SettingsRepository
import com.daykhata.app.domain.ExpenseCalculator
import com.daykhata.app.util.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddExpenseUiState(
    val realAmount: String = "",
    val homeAmount: String = "",
    val homeLabel: String = "",
    val personalLabel: String = "",
    val date: Long = System.currentTimeMillis(),
    val realAmountError: String? = null,
    val homeAmountError: String? = null,
    val homeLabelError: String? = null,
    val isCutOffReached: Boolean = false,
    val cutOffLimit: Int = 2000,
    val currentHomeBalance: Int = 0,
    val showCutOffConfirmDialog: Boolean = false,
    val isEditing: Boolean = false,
    val editingExpenseId: Long? = null,
    val isSaved: Boolean = false
)

sealed interface AddExpenseEvent {
    data object ExpenseSaved : AddExpenseEvent
    data class ShowToast(val message: String) : AddExpenseEvent
}

class AddExpenseViewModel(
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val calculator: ExpenseCalculator,
    private val expenseIdToEdit: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddExpenseEvent>()
    val events: SharedFlow<AddExpenseEvent> = _events.asSharedFlow()

    init {
        checkCurrentCutOffStatus()
        if (expenseIdToEdit != null && expenseIdToEdit > 0) {
            loadExpenseToEdit(expenseIdToEdit)
        }
    }

    private fun checkCurrentCutOffStatus() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val (periodStart, periodEnd) = DateUtils.calculatePeriodRange(settings.periodType)
            val periodExpenses = expenseRepository.getExpensesByPeriod(periodStart, periodEnd).first()
            val summary = calculator.calculate(periodExpenses, settings)

            _uiState.update {
                it.copy(
                    isCutOffReached = summary.cutOffReached,
                    cutOffLimit = summary.cutOffLimit,
                    currentHomeBalance = summary.homeBalance
                )
            }
        }
    }

    private fun loadExpenseToEdit(id: Long) {
        viewModelScope.launch {
            expenseRepository.getExpenseById(id).first()?.let { expense ->
                _uiState.update {
                    it.copy(
                        isEditing = true,
                        editingExpenseId = expense.id,
                        realAmount = expense.realAmount.toString(),
                        homeAmount = if (expense.homeAmount != expense.realAmount) expense.homeAmount.toString() else "",
                        homeLabel = expense.homeLabel,
                        personalLabel = if (expense.personalLabel != expense.homeLabel) expense.personalLabel else "",
                        date = expense.date
                    )
                }
            }
        }
    }

    fun onRealAmountChange(amount: String) {
        _uiState.update { it.copy(realAmount = amount, realAmountError = null) }
    }

    fun onHomeAmountChange(amount: String) {
        _uiState.update { it.copy(homeAmount = amount, homeAmountError = null) }
    }

    fun onHomeLabelChange(label: String) {
        _uiState.update { it.copy(homeLabel = label, homeLabelError = null) }
    }

    fun onPersonalLabelChange(label: String) {
        _uiState.update { it.copy(personalLabel = label) }
    }

    fun onDateChange(dateMillis: Long) {
        _uiState.update { it.copy(date = dateMillis) }
    }

    fun dismissCutOffDialog() {
        _uiState.update { it.copy(showCutOffConfirmDialog = false) }
    }

    fun onSaveClicked() {
        val state = _uiState.value

        // Validate real amount
        val realAmountInt = state.realAmount.trim().toIntOrNull()
        if (realAmountInt == null || realAmountInt <= 0) {
            _uiState.update { it.copy(realAmountError = "Please enter a valid amount greater than 0") }
            return
        }

        // Validate home amount if entered
        if (state.homeAmount.isNotBlank()) {
            val homeAmountInt = state.homeAmount.trim().toIntOrNull()
            if (homeAmountInt == null || homeAmountInt <= 0) {
                _uiState.update { it.copy(homeAmountError = "Home amount must be a positive number") }
                return
            }
        }

        // Validate home label
        if (state.homeLabel.trim().isBlank()) {
            _uiState.update { it.copy(homeLabelError = "Home label cannot be empty") }
            return
        }

        // Check cut-off condition: if cut-off reached and adding a new expense, show confirmation dialog
        if (state.isCutOffReached && !state.isEditing) {
            _uiState.update { it.copy(showCutOffConfirmDialog = true) }
            return
        }

        saveExpenseInternal()
    }

    fun onConfirmSaveDespiteCutOff() {
        _uiState.update { it.copy(showCutOffConfirmDialog = false) }
        saveExpenseInternal()
    }

    private fun saveExpenseInternal() {
        val state = _uiState.value
        val realAmount = state.realAmount.trim().toInt()
        val resolvedHomeAmount = calculator.resolveHomeAmount(realAmount, state.homeAmount)
        val homeLabel = state.homeLabel.trim()
        val personalLabel = if (state.personalLabel.trim().isBlank()) homeLabel else state.personalLabel.trim()

        viewModelScope.launch {
            if (state.isEditing && state.editingExpenseId != null) {
                val updatedExpense = Expense(
                    id = state.editingExpenseId,
                    date = state.date,
                    realAmount = realAmount,
                    homeAmount = resolvedHomeAmount,
                    homeLabel = homeLabel,
                    personalLabel = personalLabel
                )
                expenseRepository.updateExpense(updatedExpense)
            } else {
                val newExpense = Expense.create(
                    date = state.date,
                    realAmount = realAmount,
                    homeAmount = resolvedHomeAmount,
                    homeLabel = homeLabel,
                    personalLabel = personalLabel
                )
                expenseRepository.insertExpense(newExpense)
            }

            _uiState.update { it.copy(isSaved = true) }
            _events.emit(AddExpenseEvent.ExpenseSaved)
        }
    }

    class Factory(
        private val expenseRepository: ExpenseRepository,
        private val settingsRepository: SettingsRepository,
        private val calculator: ExpenseCalculator,
        private val expenseIdToEdit: Long? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddExpenseViewModel(
                expenseRepository,
                settingsRepository,
                calculator,
                expenseIdToEdit
            ) as T
        }
    }
}
