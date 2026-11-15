package com.daykhata.app.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daykhata.app.domain.backup.RestoreMode
import com.daykhata.app.ui.theme.PrimaryBlue
import com.daykhata.app.ui.theme.RedNegative

@Composable
fun BackupRestoreDialog(
    fileName: String,
    onConfirmRestore: (passphrase: CharArray, mode: RestoreMode) -> Unit,
    onDismiss: () -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(RestoreMode.MERGE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FileOpen, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore Backup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Selected file: $fileName",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = {
                        passphrase = it
                        errorMessage = null
                    },
                    label = { Text("Backup Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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

                Text(
                    text = "Restore Mode",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Merge option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedMode == RestoreMode.MERGE),
                                onClick = { selectedMode = RestoreMode.MERGE },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedMode == RestoreMode.MERGE),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Merge (Recommended)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(
                                text = "Adds new expenses from backup; preserves existing entries.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Replace option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedMode == RestoreMode.REPLACE),
                                onClick = { selectedMode = RestoreMode.REPLACE },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedMode == RestoreMode.REPLACE),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Replace All Data", fontWeight = FontWeight.Bold, color = RedNegative, fontSize = 14.sp)
                            Text(
                                text = "Wipes existing expenses and overwrites with backup content.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (selectedMode == RestoreMode.REPLACE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RedNegative, modifier = Modifier.weight(0.1f))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Replace mode is irreversible. Current records will be deleted before restoring.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RedNegative,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passphrase.length < 8) {
                        errorMessage = "Passphrase must be at least 8 characters."
                    } else {
                        val passArray = passphrase.toCharArray()
                        passphrase = ""
                        onConfirmRestore(passArray, selectedMode)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedMode == RestoreMode.REPLACE) RedNegative else PrimaryBlue
                )
            ) {
                Text(if (selectedMode == RestoreMode.REPLACE) "Replace & Restore" else "Merge & Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
