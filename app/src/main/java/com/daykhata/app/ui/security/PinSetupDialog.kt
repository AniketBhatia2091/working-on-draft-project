package com.daykhata.app.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daykhata.app.ui.theme.PurplePersonal
import com.daykhata.app.ui.theme.RedNegative

@Composable
fun PinSetupDialog(
    isBiometricAvailable: Boolean,
    onPinCreated: suspend (pin: String, enableBiometric: Boolean) -> Unit,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    // Steps: 1 = Enter PIN, 2 = Confirm PIN, 3 = Biometric Opt-in (if available)
    var step by remember { mutableIntStateOf(1) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = PurplePersonal
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (step) {
                        1 -> "Set Up Personal PIN"
                        2 -> "Confirm Your PIN"
                        else -> "Biometric Unlock"
                    },
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
                when (step) {
                    1 -> {
                        Text(
                            text = "To protect your real amounts and private savings, create a 4 to 6 digit security PIN.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { input ->
                                if (input.length <= 6 && input.all { it.isDigit() }) {
                                    pin = input
                                    errorMessage = null
                                }
                            },
                            label = { Text("New PIN (4-6 digits)") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            isError = errorMessage != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    2 -> {
                        Text(
                            text = "Please re-enter your PIN to confirm.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { input ->
                                if (input.length <= 6 && input.all { it.isDigit() }) {
                                    confirmPin = input
                                    errorMessage = null
                                }
                            },
                            label = { Text("Confirm PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            isError = errorMessage != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    3 -> {
                        Text(
                            text = "Enable biometric authentication to quickly unlock Personal Mode with your fingerprint or face.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = PurplePersonal
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enable Biometric Unlock",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Switch(
                                checked = enableBiometric,
                                onCheckedChange = { enableBiometric = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PurplePersonal,
                                    checkedTrackColor = PurplePersonal.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = RedNegative,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (step) {
                        1 -> {
                            if (pin.length < 4) {
                                errorMessage = "PIN must be at least 4 digits."
                            } else {
                                step = 2
                            }
                        }
                        2 -> {
                            if (confirmPin != pin) {
                                errorMessage = "PINs do not match. Please try again."
                            } else if (isBiometricAvailable) {
                                step = 3
                            } else {
                                isSaving = true
                            }
                        }
                        3 -> {
                            isSaving = true
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = PurplePersonal)
            ) {
                Text(
                    when {
                        step == 1 -> "Next"
                        step == 2 && isBiometricAvailable -> "Next"
                        else -> "Finish Setup"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (step > 1) {
                        step--
                        errorMessage = null
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(if (step > 1) "Back" else "Cancel")
            }
        }
    )

    // Trigger save
    androidx.compose.runtime.LaunchedEffect(isSaving) {
        if (isSaving) {
            onPinCreated(pin, enableBiometric)
            onSuccess()
        }
    }
}
