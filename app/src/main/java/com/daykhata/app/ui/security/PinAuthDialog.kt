package com.daykhata.app.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daykhata.app.domain.model.SecuritySettings
import com.daykhata.app.ui.theme.PurplePersonal
import com.daykhata.app.ui.theme.RedNegative
import kotlinx.coroutines.delay

@Composable
fun PinAuthDialog(
    securitySettings: SecuritySettings,
    isBiometricAvailable: Boolean,
    onVerifyPin: suspend (String) -> Boolean,
    onTriggerBiometric: () -> Unit,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    // Lockout countdown timer
    var remainingSeconds by remember(securitySettings.lockoutUntilEpoch) {
        mutableLongStateOf(securitySettings.remainingLockoutSeconds)
    }

    LaunchedEffect(securitySettings.lockoutUntilEpoch, remainingSeconds) {
        if (remainingSeconds > 0) {
            delay(1000L)
            val currentRemaining = (securitySettings.lockoutUntilEpoch - System.currentTimeMillis()) / 1000L
            remainingSeconds = if (currentRemaining > 0) currentRemaining else 0L
        }
    }

    val isLockedOut = remainingSeconds > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PurplePersonal
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unlock Personal Mode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter your 4-6 digit PIN to view real spending amounts and private savings.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isLockedOut) {
                    Text(
                        text = "⚠️ Too many failed attempts. Try again in $remainingSeconds seconds.",
                        color = RedNegative,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                } else {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { input ->
                            if (input.length <= 6 && input.all { it.isDigit() }) {
                                pin = input
                                errorMessage = null
                            }
                        },
                        label = { Text("App PIN") },
                        placeholder = { Text("4 to 6 digits") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (pin.length in 4..6 && !isVerifying) {
                                    isVerifying = true
                                    // Coroutine launched in caller or handled below
                                }
                            }
                        ),
                        singleLine = true,
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = RedNegative,
                            fontSize = 12.sp
                        )
                    }
                }

                if (securitySettings.isBiometricEnabled && isBiometricAvailable && !isLockedOut) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onTriggerBiometric,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint",
                            tint = PurplePersonal
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Fingerprint / Face", color = PurplePersonal)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length in 4..6 && !isLockedOut && !isVerifying) {
                        isVerifying = true
                    }
                },
                enabled = pin.length in 4..6 && !isLockedOut && !isVerifying,
                colors = ButtonDefaults.buttonColors(containerColor = PurplePersonal)
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Handle asynchronous PIN verification
    LaunchedEffect(isVerifying) {
        if (isVerifying) {
            val success = onVerifyPin(pin)
            if (success) {
                onSuccess()
            } else {
                pin = ""
                errorMessage = "Incorrect PIN. Attempts remaining before lockout: ${5 - ((securitySettings.failedAttempts + 1) % 5)}"
                isVerifying = false
            }
        }
    }
}
