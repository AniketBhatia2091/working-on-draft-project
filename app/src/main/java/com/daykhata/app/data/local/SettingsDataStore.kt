package com.daykhata.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.daykhata.app.domain.model.PeriodType
import com.daykhata.app.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "daykhata_settings")

class SettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        val KEY_BUDGET = intPreferencesKey("budget")
        val KEY_CUTOFF_LIMIT = intPreferencesKey("cutoff_limit")
        val KEY_PERIOD_TYPE = stringPreferencesKey("period_type")
        val KEY_PERIOD_START_DATE = longPreferencesKey("period_start_date")
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

        Settings(
            budget = budget,
            cutOffLimit = cutOffLimit,
            periodType = periodType,
            periodStartDate = periodStartDate
        )
    }

    suspend fun saveSettings(settings: Settings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_BUDGET] = settings.budget
            preferences[PreferencesKeys.KEY_CUTOFF_LIMIT] = settings.cutOffLimit
            preferences[PreferencesKeys.KEY_PERIOD_TYPE] = settings.periodType.name
            preferences[PreferencesKeys.KEY_PERIOD_START_DATE] = settings.periodStartDate
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
}
