package com.daykhata.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.daykhata.app.domain.model.AutoLockTimeout
import com.daykhata.app.domain.model.FamilyContact
import com.daykhata.app.domain.model.PeriodType
import com.daykhata.app.domain.model.SecuritySettings
import com.daykhata.app.domain.model.Settings
import com.daykhata.app.domain.security.PinSecurityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "daykhata_settings")

class SettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        val KEY_BUDGET = intPreferencesKey("budget")
        val KEY_CUTOFF_LIMIT = intPreferencesKey("cutoff_limit")
        val KEY_PERIOD_TYPE = stringPreferencesKey("period_type")
        val KEY_PERIOD_START_DATE = longPreferencesKey("period_start_date")
        val KEY_FAMILY_CONTACTS = stringPreferencesKey("family_contacts_json")
        val KEY_SAVINGS_GOAL = intPreferencesKey("savings_goal")
        val KEY_GOAL_PERIOD = stringPreferencesKey("goal_period")

        // Security / Phase 5 keys
        val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_AUTO_LOCK_TIMEOUT = stringPreferencesKey("auto_lock_timeout")
        val KEY_ALWAYS_BLOCK_SCREENSHOTS = booleanPreferencesKey("always_block_screenshots")
        val KEY_FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val KEY_LOCKOUT_UNTIL = longPreferencesKey("lockout_until_epoch")
    }

    val settingsFlow: Flow<Settings> = context.dataStore.data.map { preferences ->
        val budget = preferences[PreferencesKeys.KEY_BUDGET] ?: 15000
        val cutOffLimit = preferences[PreferencesKeys.KEY_CUTOFF_LIMIT] ?: 2000
        val periodTypeStr = preferences[PreferencesKeys.KEY_PERIOD_TYPE] ?: PeriodType.MONTHLY.name
        val periodType = try {
            PeriodType.valueOf(periodTypeStr)
        } catch (_: Exception) {
            PeriodType.MONTHLY
        }
        val periodStartDate = preferences[PreferencesKeys.KEY_PERIOD_START_DATE] ?: System.currentTimeMillis()
        val savingsGoal = preferences[PreferencesKeys.KEY_SAVINGS_GOAL] ?: 0
        val goalPeriodStr = preferences[PreferencesKeys.KEY_GOAL_PERIOD] ?: com.daykhata.app.domain.model.GoalPeriod.NONE.name
        val goalPeriod = try {
            com.daykhata.app.domain.model.GoalPeriod.valueOf(goalPeriodStr)
        } catch (_: Exception) {
            com.daykhata.app.domain.model.GoalPeriod.NONE
        }

        Settings(
            budget = budget,
            cutOffLimit = cutOffLimit,
            periodType = periodType,
            periodStartDate = periodStartDate,
            savingsGoal = savingsGoal,
            goalPeriod = goalPeriod
        )
    }

    val securitySettingsFlow: Flow<SecuritySettings> = context.dataStore.data.map { preferences ->
        val pinHash = preferences[PreferencesKeys.KEY_PIN_HASH]
        val isPinSet = !pinHash.isNullOrBlank()
        val isBiometricEnabled = preferences[PreferencesKeys.KEY_BIOMETRIC_ENABLED] ?: false
        val timeoutStr = preferences[PreferencesKeys.KEY_AUTO_LOCK_TIMEOUT] ?: AutoLockTimeout.SECONDS_30.name
        val timeout = try {
            AutoLockTimeout.valueOf(timeoutStr)
        } catch (_: Exception) {
            AutoLockTimeout.SECONDS_30
        }
        val alwaysBlockScreenshots = preferences[PreferencesKeys.KEY_ALWAYS_BLOCK_SCREENSHOTS] ?: false
        val failedAttempts = preferences[PreferencesKeys.KEY_FAILED_ATTEMPTS] ?: 0
        val lockoutUntil = preferences[PreferencesKeys.KEY_LOCKOUT_UNTIL] ?: 0L

        SecuritySettings(
            isPinSet = isPinSet,
            isBiometricEnabled = isBiometricEnabled,
            autoLockTimeout = timeout,
            alwaysBlockScreenshots = alwaysBlockScreenshots,
            failedAttempts = failedAttempts,
            lockoutUntilEpoch = lockoutUntil
        )
    }

    val familyContactsFlow: Flow<List<FamilyContact>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[PreferencesKeys.KEY_FAMILY_CONTACTS] ?: "[]"
        parseContactsJson(jsonStr)
    }

    suspend fun saveFamilyContacts(contacts: List<FamilyContact>) {
        val jsonArray = JSONArray()
        contacts.forEach { contact ->
            val obj = JSONObject().apply {
                put("id", contact.id)
                put("name", contact.name)
                put("phone", contact.phone)
            }
            jsonArray.put(obj)
        }
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_FAMILY_CONTACTS] = jsonArray.toString()
        }
    }

    private fun parseContactsJson(jsonStr: String): List<FamilyContact> {
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<FamilyContact>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FamilyContact(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.getString("name"),
                        phone = obj.getString("phone")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveSettings(settings: Settings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_BUDGET] = settings.budget
            preferences[PreferencesKeys.KEY_CUTOFF_LIMIT] = settings.cutOffLimit
            preferences[PreferencesKeys.KEY_PERIOD_TYPE] = settings.periodType.name
            preferences[PreferencesKeys.KEY_PERIOD_START_DATE] = settings.periodStartDate
            preferences[PreferencesKeys.KEY_SAVINGS_GOAL] = settings.savingsGoal
            preferences[PreferencesKeys.KEY_GOAL_PERIOD] = settings.goalPeriod.name
        }
    }

    suspend fun updateBudget(budget: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_BUDGET] = budget
        }
    }

    suspend fun updateCutOffLimit(cutOffLimit: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_CUTOFF_LIMIT] = cutOffLimit
        }
    }

    suspend fun updatePeriodType(periodType: PeriodType) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_PERIOD_TYPE] = periodType.name
        }
    }

    suspend fun updatePeriodStartDate(startDate: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_PERIOD_START_DATE] = startDate
        }
    }

    suspend fun savePin(pin: String) {
        val salt = PinSecurityManager.generateSalt()
        val hash = PinSecurityManager.hashPin(pin, salt)
        val saltHex = PinSecurityManager.toHexString(salt)
        val hashHex = PinSecurityManager.toHexString(hash)

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_PIN_SALT] = saltHex
            preferences[PreferencesKeys.KEY_PIN_HASH] = hashHex
            preferences[PreferencesKeys.KEY_FAILED_ATTEMPTS] = 0
            preferences[PreferencesKeys.KEY_LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun verifyEnteredPin(pin: String): Boolean {
        var isValid = false
        context.dataStore.edit { preferences ->
            val lockoutUntil = preferences[PreferencesKeys.KEY_LOCKOUT_UNTIL] ?: 0L
            if (System.currentTimeMillis() < lockoutUntil) {
                isValid = false
                return@edit
            }

            val saltHex = preferences[PreferencesKeys.KEY_PIN_SALT] ?: ""
            val hashHex = preferences[PreferencesKeys.KEY_PIN_HASH] ?: ""
            if (saltHex.isBlank() || hashHex.isBlank()) {
                isValid = false
                return@edit
            }

            if (PinSecurityManager.verifyPin(pin, saltHex, hashHex)) {
                preferences[PreferencesKeys.KEY_FAILED_ATTEMPTS] = 0
                preferences[PreferencesKeys.KEY_LOCKOUT_UNTIL] = 0L
                isValid = true
            } else {
                val currentFailed = preferences[PreferencesKeys.KEY_FAILED_ATTEMPTS] ?: 0
                val newFailed = currentFailed + 1
                preferences[PreferencesKeys.KEY_FAILED_ATTEMPTS] = newFailed
                val lockoutDuration = PinSecurityManager.getLockoutDurationSeconds(newFailed)
                if (lockoutDuration > 0) {
                    preferences[PreferencesKeys.KEY_LOCKOUT_UNTIL] = System.currentTimeMillis() + (lockoutDuration * 1000L)
                }
                isValid = false
            }
        }
        return isValid
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_AUTO_LOCK_TIMEOUT] = timeout.name
        }
    }

    suspend fun setAlwaysBlockScreenshots(always: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_ALWAYS_BLOCK_SCREENSHOTS] = always
        }
    }

    suspend fun clearSecuritySettings() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.KEY_PIN_HASH)
            preferences.remove(PreferencesKeys.KEY_PIN_SALT)
            preferences.remove(PreferencesKeys.KEY_BIOMETRIC_ENABLED)
            preferences.remove(PreferencesKeys.KEY_FAILED_ATTEMPTS)
            preferences.remove(PreferencesKeys.KEY_LOCKOUT_UNTIL)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
