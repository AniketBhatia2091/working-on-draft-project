package com.daykhata.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daykhata.app.data.repository.SettingsRepository
import com.daykhata.app.domain.model.FamilyContact
import com.daykhata.app.domain.model.GoalPeriod
import com.daykhata.app.domain.model.PeriodType
import com.daykhata.app.domain.model.Settings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val budget: String = "15000",
    val cutOffLimit: String = "2000",
    val periodType: PeriodType = PeriodType.MONTHLY,
    val savingsGoal: String = "",
    val goalPeriod: GoalPeriod = GoalPeriod.NONE,
    val familyContacts: List<FamilyContact> = emptyList(),
    val budgetError: String? = null,
    val cutOffError: String? = null,
    val savingsGoalError: String? = null,
    val isSavedSuccess: Boolean = false,
    // Contact dialog state
    val showContactDialog: Boolean = false,
    val contactBeingEdited: FamilyContact? = null,
    val contactNameInput: String = "",
    val contactPhoneInput: String = "",
    val contactNameError: String? = null,
    val contactPhoneError: String? = null
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
        loadSettingsAndContacts()
    }

    private fun loadSettingsAndContacts() {
        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                settingsRepository.familyContacts
            ) { settings, contacts ->
                Pair(settings, contacts)
            }.collect { (settings, contacts) ->
                _uiState.update { current ->
                    current.copy(
                        budget = settings.budget.toString(),
                        cutOffLimit = settings.cutOffLimit.toString(),
                        periodType = settings.periodType,
                        savingsGoal = if (settings.savingsGoal > 0) settings.savingsGoal.toString() else "",
                        goalPeriod = settings.goalPeriod,
                        familyContacts = contacts
                    )
                }
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

    fun onSavingsGoalChanged(goal: String) {
        _uiState.update { it.copy(savingsGoal = goal, savingsGoalError = null, isSavedSuccess = false) }
    }

    fun onGoalPeriodChanged(period: GoalPeriod) {
        _uiState.update { it.copy(goalPeriod = period, isSavedSuccess = false) }
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

        val goalInt = if (state.savingsGoal.trim().isNotBlank()) {
            val parsed = state.savingsGoal.trim().toIntOrNull()
            if (parsed == null || parsed < 0) {
                _uiState.update { it.copy(savingsGoalError = "Savings goal must be a non-negative number") }
                return
            }
            parsed
        } else {
            0
        }

        viewModelScope.launch {
            settingsRepository.saveSettings(
                Settings(
                    budget = budgetInt,
                    cutOffLimit = cutOffInt,
                    periodType = state.periodType,
                    savingsGoal = goalInt,
                    goalPeriod = state.goalPeriod
                )
            )
            _uiState.update { it.copy(isSavedSuccess = true) }
            _events.emit(SettingsEvent.ShowToast("Settings saved successfully"))
        }
    }

    // Family contacts management
    fun openAddContactDialog() {
        _uiState.update {
            it.copy(
                showContactDialog = true,
                contactBeingEdited = null,
                contactNameInput = "",
                contactPhoneInput = "",
                contactNameError = null,
                contactPhoneError = null
            )
        }
    }

    fun openEditContactDialog(contact: FamilyContact) {
        _uiState.update {
            it.copy(
                showContactDialog = true,
                contactBeingEdited = contact,
                contactNameInput = contact.name,
                contactPhoneInput = contact.phone,
                contactNameError = null,
                contactPhoneError = null
            )
        }
    }

    fun dismissContactDialog() {
        _uiState.update { it.copy(showContactDialog = false) }
    }

    fun onContactNameChanged(name: String) {
        _uiState.update { it.copy(contactNameInput = name, contactNameError = null) }
    }

    fun onContactPhoneChanged(phone: String) {
        _uiState.update { it.copy(contactPhoneInput = phone, contactPhoneError = null) }
    }

    fun saveContact() {
        val state = _uiState.value
        val name = state.contactNameInput.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(contactNameError = "Contact name cannot be empty") }
            return
        }

        val sanitizedPhone = FamilyContact.sanitizePhone(state.contactPhoneInput, defaultCountryCode = "91")
        if (sanitizedPhone == null) {
            _uiState.update { it.copy(contactPhoneError = "Enter 10 to 15 digits (default country code 91)") }
            return
        }

        viewModelScope.launch {
            val contactToSave = if (state.contactBeingEdited != null) {
                state.contactBeingEdited.copy(name = name, phone = sanitizedPhone)
            } else {
                FamilyContact(name = name, phone = sanitizedPhone)
            }

            if (state.contactBeingEdited != null) {
                settingsRepository.updateFamilyContact(contactToSave)
            } else {
                settingsRepository.addFamilyContact(contactToSave)
            }

            _uiState.update { it.copy(showContactDialog = false) }
            _events.emit(SettingsEvent.ShowToast("Contact '${contactToSave.name}' saved"))
        }
    }

    fun deleteContact(contact: FamilyContact) {
        viewModelScope.launch {
            settingsRepository.deleteFamilyContact(contact.id)
            _events.emit(SettingsEvent.ShowToast("Contact '${contact.name}' deleted"))
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
