package com.daykhata.app.domain.backup

import androidx.room.withTransaction
import com.daykhata.app.data.local.AppDatabase
import com.daykhata.app.data.local.Expense
import com.daykhata.app.data.repository.SettingsRepository
import com.daykhata.app.domain.model.FamilyContact
import com.daykhata.app.domain.model.GoalPeriod
import com.daykhata.app.domain.model.PeriodType
import com.daykhata.app.domain.model.Settings
import kotlinx.coroutines.flow.first

class BackupManager(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) {

    suspend fun createBackupPayload(): BackupPayload {
        val expenses = database.expenseDao().getAllExpenses().first()
        val settings = settingsRepository.settings.first()
        val contacts = settingsRepository.familyContacts.first()

        val expenseDtos = expenses.map { exp ->
            BackupExpenseDto(
                date = exp.date,
                realAmount = exp.realAmount,
                homeAmount = exp.homeAmount,
                homeLabel = exp.homeLabel,
                personalLabel = exp.personalLabel
            )
        }

        val settingsDto = BackupSettingsDto(
            budget = settings.budget,
            cutOffLimit = settings.cutOffLimit,
            periodType = settings.periodType.name,
            periodStartDate = settings.periodStartDate
        )

        val contactDtos = contacts.map { contact ->
            BackupContactDto(name = contact.name, phone = contact.phone)
        }

        val goalDto = BackupGoalDto(
            savingsGoal = settings.savingsGoal,
            goalPeriod = settings.goalPeriod.name
        )

        return BackupPayload(
            version = BackupCrypto.CURRENT_VERSION,
            exportTimestamp = System.currentTimeMillis(),
            expenses = expenseDtos,
            settings = settingsDto,
            familyContacts = contactDtos,
            savingsGoal = goalDto
        )
    }

    suspend fun exportEncryptedBackup(passphrase: CharArray): ByteArray {
        val payload = createBackupPayload()
        val json = BackupCrypto.serializePayload(payload)
        return BackupCrypto.encryptBackup(json, passphrase)
    }

    suspend fun restoreEncryptedBackup(
        encryptedBytes: ByteArray,
        passphrase: CharArray,
        mode: RestoreMode
    ): RestoreResult {
        val json = try {
            BackupCrypto.decryptBackup(encryptedBytes, passphrase)
        } catch (e: Exception) {
            return RestoreResult.Error("Could not open this backup. Incorrect passphrase or corrupted file.")
        }

        val payload = try {
            BackupCrypto.deserializePayload(json)
        } catch (e: Exception) {
            return RestoreResult.Error("Invalid backup data format: ${e.message}")
        }

        return try {
            database.withTransaction {
                when (mode) {
                    RestoreMode.REPLACE -> {
                        // Purge existing expenses and insert all backup expenses
                        database.expenseDao().deleteAllExpenses()
                        val newExpenses = payload.expenses.map { dto ->
                            Expense(
                                id = 0,
                                date = dto.date,
                                realAmount = dto.realAmount,
                                homeAmount = dto.homeAmount,
                                homeLabel = dto.homeLabel,
                                personalLabel = dto.personalLabel
                            )
                        }
                        database.expenseDao().insertAll(newExpenses)

                        // Update settings
                        val periodType = try {
                            PeriodType.valueOf(payload.settings.periodType)
                        } catch (_: Exception) {
                            PeriodType.MONTHLY
                        }
                        val goalPeriod = try {
                            GoalPeriod.valueOf(payload.savingsGoal.goalPeriod)
                        } catch (_: Exception) {
                            GoalPeriod.NONE
                        }

                        settingsRepository.saveSettings(
                            Settings(
                                budget = payload.settings.budget,
                                cutOffLimit = payload.settings.cutOffLimit,
                                periodType = periodType,
                                periodStartDate = payload.settings.periodStartDate,
                                savingsGoal = payload.savingsGoal.savingsGoal,
                                goalPeriod = goalPeriod
                            )
                        )

                        // Replace contacts
                        val newContacts = payload.familyContacts.map { dto ->
                            FamilyContact(name = dto.name, phone = dto.phone)
                        }
                        settingsRepository.saveFamilyContacts(newContacts)

                        RestoreResult.Success(
                            expensesRestored = newExpenses.size,
                            contactsRestored = newContacts.size,
                            mode = RestoreMode.REPLACE
                        )
                    }

                    RestoreMode.MERGE -> {
                        val currentExpenses = database.expenseDao().getAllExpenses().first()
                        val toInsert = mutableListOf<Expense>()

                        payload.expenses.forEach { dto ->
                            val exists = currentExpenses.any { curr ->
                                curr.date == dto.date &&
                                    curr.realAmount == dto.realAmount &&
                                    curr.homeAmount == dto.homeAmount &&
                                    curr.homeLabel == dto.homeLabel &&
                                    curr.personalLabel == dto.personalLabel
                            }
                            if (!exists) {
                                toInsert.add(
                                    Expense(
                                        id = 0,
                                        date = dto.date,
                                        realAmount = dto.realAmount,
                                        homeAmount = dto.homeAmount,
                                        homeLabel = dto.homeLabel,
                                        personalLabel = dto.personalLabel
                                    )
                                )
                            }
                        }
                        if (toInsert.isNotEmpty()) {
                            database.expenseDao().insertAll(toInsert)
                        }

                        // Merge contacts
                        val currentContacts = settingsRepository.familyContacts.first()
                        val mergedContacts = currentContacts.toMutableList()
                        var newContactsCount = 0

                        payload.familyContacts.forEach { dto ->
                            val exists = currentContacts.any { it.phone == dto.phone }
                            if (!exists) {
                                mergedContacts.add(FamilyContact(name = dto.name, phone = dto.phone))
                                newContactsCount++
                            }
                        }
                        if (newContactsCount > 0) {
                            settingsRepository.saveFamilyContacts(mergedContacts)
                        }

                        RestoreResult.Success(
                            expensesRestored = toInsert.size,
                            contactsRestored = newContactsCount,
                            mode = RestoreMode.MERGE
                        )
                    }
                }
            }
        } catch (e: Exception) {
            RestoreResult.Error("Database transaction failed during restore: ${e.message}")
        }
    }
}
