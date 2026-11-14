package com.daykhata.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daykhata.app.data.repository.SettingsRepository
import com.daykhata.app.domain.model.PeriodType
import com.daykhata.app.domain.model.Settings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val budget: String = "15000",
    val cutOffLimit: String = "2000",
    val periodType: PeriodType = PeriodType.MONTHLY,
    val budgetError: String? = null,
    val cutOffError: String? = null,
    val isSavedSuccess: Boolean = false
)

sealed interface SettingsEvent {
    data class ShowToast(val message: String) : SettingsEvent
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _uiState.update {
                it.copy(
                    budget = settings.budget.toString(),
                    cutOffLimit = settings.cutOffLimit.toString(),
                    periodType = settings.periodType
                )
            }
        }
    }

    fun onBudgetChanged(newBudget: String) {
        _uiState.update { it.copy(budget = newBudget, budgetError = null, isSavedSuccess = false) }
    }

    fun onCutOffLimitChanged(newLimit: String) {
        _uiState.update { it.copy(cutOffLimit = newLimit, cutOffError = null, isSavedSuccess = false) }
    }

    fun onPeriodTypeChanged(newType: PeriodType) {
        _uiState.update { it.copy(periodType = newType, isSavedSuccess = false) }
    }

    fun saveSettings() {
        val state = _uiState.value

        val budgetInt = state.budget.trim().toIntOrNull()
        if (budgetInt == null || budgetInt <= 0) {
            _uiState.update { it.copy(budgetError = "Budget must be a positive number") }
            return
        }

        val cutOffInt = state.cutOffLimit.trim().toIntOrNull()
        if (cutOffInt == null || cutOffInt < 0) {
            _uiState.update { it.copy(cutOffError = "Cut-off limit must be 0 or higher") }
            return
        }

        if (cutOffInt > budgetInt) {
            _uiState.update { it.copy(cutOffError = "Cut-off limit cannot exceed total budget") }
            return
        }

        viewModelScope.launch {
            settingsRepository.saveSettings(
                Settings(
                    budget = budgetInt,
                    cutOffLimit = cutOffInt,
                    periodType = state.periodType
                )
            )
            _uiState.update { it.copy(isSavedSuccess = true) }
            _events.emit(SettingsEvent.ShowToast("Settings saved successfully"))
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository) as T
        }
    }
}
