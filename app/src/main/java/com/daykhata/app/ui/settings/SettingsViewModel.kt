package com.daykhata.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daykhata.app.data.repository.SettingsRepository
import com.daykhata.app.domain.AppModeState
import com.daykhata.app.domain.backup.BackupManager
import com.daykhata.app.domain.backup.RestoreMode
import com.daykhata.app.domain.backup.RestoreResult
import com.daykhata.app.domain.model.AutoLockTimeout
import com.daykhata.app.domain.model.FamilyContact
import com.daykhata.app.domain.model.GoalPeriod
import com.daykhata.app.domain.model.PeriodType
import com.daykhata.app.domain.model.SecuritySettings
import com.daykhata.app.domain.model.Settings
import com.daykhata.app.domain.model.ViewMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    // Security Settings
    val securitySettings: SecuritySettings = SecuritySettings(),
    val isPersonalMode: Boolean = false,
    val showPinChangeDialog: Boolean = false,
    val showResetLockDialog: Boolean = false,

    // Backup & Restore state
    val showExportDialog: Boolean = false,
    val showRestoreDialog: Boolean = false,
    val pendingRestoreFileName: String = "",
    val isBackingUpOrRestoring: Boolean = false,

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
    data object ResetSuccessful : SettingsEvent
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val appModeState: AppModeState
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
                settingsRepository.familyContacts,
                settingsRepository.securitySettings,
                appModeState.mode
            ) { settings, contacts, security, mode ->
                _uiState.update { current ->
                    current.copy(
                        budget = settings.budget.toString(),
                        cutOffLimit = settings.cutOffLimit.toString(),
                        periodType = settings.periodType,
                        savingsGoal = if (settings.savingsGoal > 0) settings.savingsGoal.toString() else "",
                        goalPeriod = settings.goalPeriod,
                        familyContacts = contacts,
                        securitySettings = security,
                        isPersonalMode = mode == ViewMode.PERSONAL
                    )
                }
            }.collect {}
        }
    }

    fun requestUnlockPersonal() {
        appModeState.requestPersonalMode()
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

    // Security Settings actions
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
            _events.emit(SettingsEvent.ShowToast(if (enabled) "Biometric unlock enabled" else "Biometric unlock disabled"))
        }
    }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        viewModelScope.launch {
            settingsRepository.setAutoLockTimeout(timeout)
            _events.emit(SettingsEvent.ShowToast("Auto-lock set to ${timeout.label}"))
        }
    }

    fun setAlwaysBlockScreenshots(always: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAlwaysBlockScreenshots(always)
            _events.emit(SettingsEvent.ShowToast(if (always) "Screenshots blocked on all screens" else "Screenshots allowed in Home mode"))
        }
    }

    fun openChangePinDialog() {
        _uiState.update { it.copy(showPinChangeDialog = true) }
    }

    fun dismissChangePinDialog() {
        _uiState.update { it.copy(showPinChangeDialog = false) }
    }

    fun openResetLockDialog() {
        _uiState.update { it.copy(showResetLockDialog = true) }
    }

    fun dismissResetLockDialog() {
        _uiState.update { it.copy(showResetLockDialog = false) }
    }

    suspend fun verifyCurrentPin(pin: String): Boolean {
        return settingsRepository.verifyPin(pin)
    }

    suspend fun saveNewPin(pin: String) {
        settingsRepository.setPin(pin)
        _events.emit(SettingsEvent.ShowToast("PIN updated successfully"))
    }

    suspend fun resetAppLockAndData() {
        settingsRepository.resetAllData()
        _events.emit(SettingsEvent.ResetSuccessful)
    }

    // Backup & Restore
    fun openExportDialog() {
        _uiState.update { it.copy(showExportDialog = true) }
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(showExportDialog = false) }
    }

    fun openRestoreDialog(fileName: String) {
        _uiState.update { it.copy(showRestoreDialog = true, pendingRestoreFileName = fileName) }
    }

    fun dismissRestoreDialog() {
        _uiState.update { it.copy(showRestoreDialog = false, pendingRestoreFileName = "") }
    }

    suspend fun generateEncryptedBackup(passphrase: CharArray): ByteArray? {
        return try {
            backupManager.exportEncryptedBackup(passphrase)
        } catch (e: Exception) {
            _events.emit(SettingsEvent.ShowToast("Export failed: ${e.message}"))
            null
        }
    }

    fun restoreBackup(encryptedBytes: ByteArray, passphrase: CharArray, mode: RestoreMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUpOrRestoring = true) }
            val result = backupManager.restoreEncryptedBackup(encryptedBytes, passphrase, mode)
            _uiState.update { it.copy(isBackingUpOrRestoring = false, showRestoreDialog = false) }
            when (result) {
                is RestoreResult.Success -> {
                    val modeLabel = if (result.mode == RestoreMode.MERGE) "Merged" else "Restored (Replaced)"
                    _events.emit(SettingsEvent.ShowToast("$modeLabel ${result.expensesRestored} expenses, ${result.contactsRestored} contacts."))
                }
                is RestoreResult.Error -> {
                    _events.emit(SettingsEvent.ShowToast(result.message))
                }
            }
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
        private val settingsRepository: SettingsRepository,
        private val backupManager: BackupManager,
        private val appModeState: AppModeState
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository, backupManager, appModeState) as T
        }
    }
}
