package com.daykhata.app.domain.backup

import com.daykhata.app.data.local.Expense
import com.daykhata.app.domain.model.FamilyContact
import com.daykhata.app.domain.model.GoalPeriod
import com.daykhata.app.domain.model.PeriodType
import com.daykhata.app.domain.model.Settings

data class BackupExpenseDto(
    val date: Long,
    val realAmount: Int,
    val homeAmount: Int,
    val homeLabel: String,
    val personalLabel: String
)

data class BackupSettingsDto(
    val budget: Int,
    val cutOffLimit: Int,
    val periodType: String,
    val periodStartDate: Long
)

data class BackupContactDto(
    val name: String,
    val phone: String
)

data class BackupGoalDto(
    val savingsGoal: Int,
    val goalPeriod: String
)

data class BackupPayload(
    val version: Int = 1,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val expenses: List<BackupExpenseDto>,
    val settings: BackupSettingsDto,
    val familyContacts: List<BackupContactDto>,
    val savingsGoal: BackupGoalDto
)

enum class RestoreMode {
    REPLACE,
    MERGE
}

sealed interface RestoreResult {
    data class Success(val expensesRestored: Int, val contactsRestored: Int, val mode: RestoreMode) : RestoreResult
    data class Error(val message: String) : RestoreResult
}
