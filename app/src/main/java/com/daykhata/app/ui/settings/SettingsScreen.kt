package com.daykhata.app.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daykhata.app.domain.model.AutoLockTimeout
import com.daykhata.app.domain.model.FamilyContact
import com.daykhata.app.domain.model.GoalPeriod
import com.daykhata.app.domain.model.PeriodType
import com.daykhata.app.domain.security.BiometricHelper
import com.daykhata.app.ui.security.PinChangeDialog
import com.daykhata.app.ui.security.ResetAppLockDialog
import com.daykhata.app.ui.theme.GreenPositive
import com.daykhata.app.ui.theme.PrimaryBlue
import com.daykhata.app.ui.theme.PurplePersonal
import com.daykhata.app.ui.theme.RedNegative
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onResetCompleted: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var contactToDelete by remember { mutableStateOf<FamilyContact?>(null) }

    val isBiometricAvailable = remember(context) {
        BiometricHelper.isBiometricAvailable(context)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.ResetSuccessful -> {
                    Toast.makeText(context, "App lock and data purged successfully.", Toast.LENGTH_LONG).show()
                    onResetCompleted()
                }
            }
        }
    }

    // Delete contact dialog
    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Delete Family Contact") },
            text = { Text("Are you sure you want to remove ${contact.name} (${contact.phone})?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteContact(contact)
                        contactToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add / Edit contact dialog
    if (uiState.showContactDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissContactDialog() },
            title = {
                Text(if (uiState.contactBeingEdited != null) "Edit Contact" else "Add Family Contact")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.contactNameInput,
                        onValueChange = { viewModel.onContactNameChanged(it) },
                        label = { Text("Name") },
                        placeholder = { Text("e.g. Papa, Home, Sister") },
                        isError = uiState.contactNameError != null,
                        supportingText = uiState.contactNameError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.contactPhoneInput,
                        onValueChange = { viewModel.onContactPhoneChanged(it) },
                        label = { Text("WhatsApp Phone Number") },
                        placeholder = { Text("e.g. 9876543210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = uiState.contactPhoneError != null,
                        supportingText = uiState.contactPhoneError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.saveContact() }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissContactDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PIN Change Dialog
    if (uiState.showPinChangeDialog) {
        PinChangeDialog(
            onVerifyCurrentPin = { pin -> viewModel.verifyCurrentPin(pin) },
            onSaveNewPin = { newPin -> viewModel.saveNewPin(newPin) },
            onSuccess = { viewModel.dismissChangePinDialog() },
            onDismiss = { viewModel.dismissChangePinDialog() }
        )
    }

    // Reset App Lock Dialog
    if (uiState.showResetLockDialog) {
        ResetAppLockDialog(
            onConfirmReset = { viewModel.resetAppLockAndData() },
            onSuccess = { viewModel.dismissResetLockDialog() },
            onDismiss = { viewModel.dismissResetLockDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Success Banner
            AnimatedVisibility(visible = uiState.isSavedSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GreenPositive.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenPositive)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Settings saved successfully",
                            color = GreenPositive,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ==========================================
            // 1. Budget Settings
            // ==========================================
            SectionHeader(title = "1. Budget Settings", icon = Icons.Default.AccountBalanceWallet)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.budget,
                        onValueChange = { viewModel.onBudgetChanged(it) },
                        label = { Text("Period Budget (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.budgetError != null,
                        supportingText = uiState.budgetError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.cutOffLimit,
                        onValueChange = { viewModel.onCutOffLimitChanged(it) },
                        label = { Text("Cut-off Threshold (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.cutOffError != null,
                        supportingText = uiState.cutOffError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Budget Period Type",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PeriodType.entries.forEach { type ->
                            Row(
                                modifier = Modifier
                                    .selectable(
                                        selected = (uiState.periodType == type),
                                        onClick = { viewModel.onPeriodTypeChanged(type) },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (uiState.periodType == type),
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (type == PeriodType.MONTHLY) "Monthly" else "Weekly",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.saveSettings() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Save Budget Settings")
                    }
                }
            }

            // ==========================================
            // 2. Savings Target
            // ==========================================
            SectionHeader(title = "2. Savings Target", icon = Icons.Default.Savings)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.savingsGoal,
                        onValueChange = { viewModel.onSavingsGoalChanged(it) },
                        label = { Text("Savings Goal Amount (₹)") },
                        placeholder = { Text("e.g. 5000 (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.savingsGoalError != null,
                        supportingText = uiState.savingsGoalError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Goal Target Period",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        GoalPeriod.entries.forEach { period ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = (uiState.goalPeriod == period),
                                        onClick = { viewModel.onGoalPeriodChanged(period) },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (uiState.goalPeriod == period),
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (period) {
                                        GoalPeriod.NONE -> "No specific period"
                                        GoalPeriod.WEEKLY -> "Weekly Target"
                                        GoalPeriod.MONTHLY -> "Monthly Target"
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.saveSettings() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Save Savings Target")
                    }
                }
            }

            // ==========================================
            // 3. Family Contacts
            // ==========================================
            SectionHeader(title = "3. Family Contacts", icon = Icons.Default.Person)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Quick WhatsApp recipients for sharing family-safe expense summaries.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (uiState.familyContacts.isEmpty()) {
                        Text(
                            text = "No family contacts added yet.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        uiState.familyContacts.forEach { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = contact.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(text = contact.phone, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row {
                                    IconButton(onClick = { viewModel.openEditContactDialog(contact) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryBlue)
                                    }
                                    IconButton(onClick = { contactToDelete = contact }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedNegative)
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.openAddContactDialog() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Family Contact")
                    }
                }
            }

            // ==========================================
            // 4. Privacy & Lock
            // ==========================================
            SectionHeader(title = "4. Privacy & Lock", icon = Icons.Default.Security)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // PIN Status & Change PIN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "App Lock PIN", fontWeight = FontWeight.Bold)
                            Text(
                                text = if (uiState.securitySettings.isPinSet) "PIN is active (4-6 digits)" else "No PIN set (Setup required for Personal mode)",
                                fontSize = 12.sp,
                                color = if (uiState.securitySettings.isPinSet) GreenPositive else RedNegative
                            )
                        }
                        if (uiState.securitySettings.isPinSet) {
                            OutlinedButton(onClick = { viewModel.openChangePinDialog() }) {
                                Text("Change PIN")
                            }
                        }
                    }

                    HorizontalDivider()

                    // Biometric Unlock Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = PurplePersonal, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Biometric Unlock", fontWeight = FontWeight.Medium)
                            }
                            Text(
                                text = if (isBiometricAvailable) "Unlock Personal Mode with fingerprint or face" else "Biometrics not available on this device",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.securitySettings.isBiometricEnabled && isBiometricAvailable,
                            onCheckedChange = { viewModel.setBiometricEnabled(it) },
                            enabled = isBiometricAvailable && uiState.securitySettings.isPinSet,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PurplePersonal,
                                checkedTrackColor = PurplePersonal.copy(alpha = 0.5f)
                            )
                        )
                    }

                    HorizontalDivider()

                    // Auto-lock inactivity timeout
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Auto-Lock Inactivity Timeout", fontWeight = FontWeight.Medium)
                        Text(
                            text = "Auto-relocks to Home mode when inactive in Personal mode",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AutoLockTimeout.entries.forEach { timeout ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = (uiState.securitySettings.autoLockTimeout == timeout),
                                        onClick = { viewModel.setAutoLockTimeout(timeout) },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (uiState.securitySettings.autoLockTimeout == timeout),
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = timeout.label, fontSize = 14.sp)
                            }
                        }
                    }

                    HorizontalDivider()

                    // Always Block Screenshots Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Always Block Screenshots", fontWeight = FontWeight.Medium)
                            }
                            Text(
                                text = "Keep screen protected (FLAG_SECURE) even in Home mode",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.securitySettings.alwaysBlockScreenshots,
                            onCheckedChange = { viewModel.setAlwaysBlockScreenshots(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryBlue,
                                checkedTrackColor = PrimaryBlue.copy(alpha = 0.5f)
                            )
                        )
                    }

                    HorizontalDivider()

                    // Reset App Lock Action
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Reset App Lock & Purge Data", fontWeight = FontWeight.Bold, color = RedNegative)
                        Text(
                            text = "If you forgot your PIN or want to clear all data, resetting the lock permanently wipes all entries and settings to prevent unauthorized access.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.openResetLockDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = RedNegative),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset App Lock")
                        }
                    }
                }
            }

            // ==========================================
            // 5. About
            // ==========================================
            SectionHeader(title = "5. About", icon = Icons.Default.Info)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "DayKhata", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(text = "Version 1.0 (Production Release)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "How it works:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "DayKhata is a dual-accounting daily expense tracker designed to help you balance shared family household budgets with your personal spending. Keep daily logs clear, calculate savings effortlessly, and share family-ready summaries directly via WhatsApp.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Home mode displays family-safe expenses, budgets, and WhatsApp summaries. Personal mode reveals real amounts, private logs, and accumulated savings behind biometric and PIN encryption.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
