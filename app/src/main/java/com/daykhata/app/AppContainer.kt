package com.daykhata.app

import android.content.Context
import com.daykhata.app.data.local.AppDatabase
import com.daykhata.app.data.local.SettingsDataStore
import com.daykhata.app.data.repository.ExpenseRepository
import com.daykhata.app.data.repository.ExpenseRepositoryImpl
import com.daykhata.app.data.repository.SettingsRepository
import com.daykhata.app.data.repository.SettingsRepositoryImpl
import com.daykhata.app.domain.AppModeState
import com.daykhata.app.domain.ExpenseCalculator
import com.daykhata.app.domain.WeeklySummaryCalculator

interface AppContainer {
    val expenseRepository: ExpenseRepository
    val settingsRepository: SettingsRepository
    val expenseCalculator: ExpenseCalculator
    val weeklySummaryCalculator: WeeklySummaryCalculator
    val appModeState: AppModeState
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    private val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(context)
    }

    override val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepositoryImpl(database.expenseDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(settingsDataStore)
    }

    override val expenseCalculator: ExpenseCalculator by lazy {
        ExpenseCalculator()
    }

    override val weeklySummaryCalculator: WeeklySummaryCalculator by lazy {
        WeeklySummaryCalculator()
    }

    override val appModeState: AppModeState by lazy {
        AppModeState()
    }
}
