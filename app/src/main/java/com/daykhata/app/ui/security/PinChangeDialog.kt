package com.daykhata.app.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
fun PinChangeDialog(
    onVerifyCurrentPin: suspend (String) -> Boolean,
    onSaveNewPin: suspend (String) -> Unit,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Verify current, 2: New PIN, 3: Confirm
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingCurrent by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = PurplePersonal
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (step) {
                        1 -> "Verify Current PIN"
                        2 -> "Enter New PIN"
                        else -> "Confirm New PIN"
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
                            text = "Please enter your current PIN to authorize this change.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = currentPin,
                            onValueChange = { input ->
                                if (input.length <= 6 && input.all { it.isDigit() }) {
                                    currentPin = input
                                    errorMessage = null
                                }
                            },
                            label = { Text("Current PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            isError = errorMessage != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    2 -> {
                        Text(
                            text = "Enter a new 4 to 6 digit security PIN.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { input ->
                                if (input.length <= 6 && input.all { it.isDigit() }) {
                                    newPin = input
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
                    3 -> {
                        Text(
                            text = "Re-enter the new PIN to confirm.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = confirmNewPin,
                            onValueChange = { input ->
                                if (input.length <= 6 && input.all { it.isDigit() }) {
                                    confirmNewPin = input
                                    errorMessage = null
                                }
                            },
                            label = { Text("Confirm New PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            isError = errorMessage != null,
                            modifier = Modifier.fillMaxWidth()
                        )
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
                            if (currentPin.length in 4..6 && !isCheckingCurrent) {
                                isCheckingCurrent = true
                            }
                        }
                        2 -> {
                            if (newPin.length < 4) {
                                errorMessage = "PIN must be at least 4 digits."
                            } else {
                                step = 3
                            }
                        }
                        3 -> {
                            if (confirmNewPin != newPin) {
                                errorMessage = "PINs do not match."
                            } else {
                                isSaving = true
                            }
                        }
                    }
                },
                enabled = !isCheckingCurrent && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = PurplePersonal)
            ) {
                Text(
                    when (step) {
                        1 -> "Verify"
                        2 -> "Next"
                        else -> "Save New PIN"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Current PIN verification
    androidx.compose.runtime.LaunchedEffect(isCheckingCurrent) {
        if (isCheckingCurrent) {
            val valid = onVerifyCurrentPin(currentPin)
            if (valid) {
                step = 2
                errorMessage = null
            } else {
                errorMessage = "Incorrect current PIN."
            }
            isCheckingCurrent = false
        }
    }

    // New PIN saving
    androidx.compose.runtime.LaunchedEffect(isSaving) {
        if (isSaving) {
            onSaveNewPin(newPin)
            onSuccess()
        }
    }
}
