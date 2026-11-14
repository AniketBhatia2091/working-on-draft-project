package com.daykhata.app.data.repository

import com.daykhata.app.data.local.SettingsDataStore
import com.daykhata.app.domain.model.FamilyContact
import com.daykhata.app.domain.model.PeriodType
import com.daykhata.app.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface SettingsRepository {
    val settings: Flow<Settings>
    val familyContacts: Flow<List<FamilyContact>>
    suspend fun saveSettings(settings: Settings)
    suspend fun updateBudget(budget: Int)
    suspend fun updateCutOffLimit(cutOffLimit: Int)
    suspend fun updatePeriodType(periodType: PeriodType)
    suspend fun saveFamilyContacts(contacts: List<FamilyContact>)
    suspend fun addFamilyContact(contact: FamilyContact)
    suspend fun updateFamilyContact(contact: FamilyContact)
    suspend fun deleteFamilyContact(contactId: String)
}

class SettingsRepositoryImpl(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override val settings: Flow<Settings> = settingsDataStore.settingsFlow
    override val familyContacts: Flow<List<FamilyContact>> = settingsDataStore.familyContactsFlow

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

    override suspend fun saveFamilyContacts(contacts: List<FamilyContact>) {
        settingsDataStore.saveFamilyContacts(contacts)
    }

    override suspend fun addFamilyContact(contact: FamilyContact) {
        val current = familyContacts.first()
        saveFamilyContacts(current + contact)
    }

    override suspend fun updateFamilyContact(contact: FamilyContact) {
        val current = familyContacts.first()
        val updated = current.map { if (it.id == contact.id) contact else it }
        saveFamilyContacts(updated)
    }

    override suspend fun deleteFamilyContact(contactId: String) {
        val current = familyContacts.first()
        val updated = current.filter { it.id != contactId }
        saveFamilyContacts(updated)
    }
}
