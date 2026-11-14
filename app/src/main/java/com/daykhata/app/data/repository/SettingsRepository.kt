package com.daykhata.app.data.repository

import com.daykhata.app.data.local.SettingsDataStore
import com.daykhata.app.domain.model.PeriodType
import com.daykhata.app.domain.model.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<Settings>
    suspend fun saveSettings(settings: Settings)
    suspend fun updateBudget(budget: Int)
    suspend fun updateCutOffLimit(cutOffLimit: Int)
    suspend fun updatePeriodType(periodType: PeriodType)
}

class SettingsRepositoryImpl(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override val settings: Flow<Settings> = settingsDataStore.settingsFlow

    override suspend fun saveSettings(settings: Settings) {
        settingsDataStore.saveSettings(settings)
    }

    override suspend fun updateBudget(budget: Int) {
        settingsDataStore.updateBudget(budget)
    }

    override suspend fun updateCutOffLimit(cutOffLimit: Int) {
        settingsDataStore.updateCutOffLimit(cutOffLimit)
    }

    override suspend fun updatePeriodType(periodType: PeriodType) {
        settingsDataStore.updatePeriodType(periodType)
    }
}
