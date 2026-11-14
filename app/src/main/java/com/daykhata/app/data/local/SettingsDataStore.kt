package com.daykhata.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.daykhata.app.domain.model.FamilyContact
import com.daykhata.app.domain.model.PeriodType
import com.daykhata.app.domain.model.Settings
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
}
