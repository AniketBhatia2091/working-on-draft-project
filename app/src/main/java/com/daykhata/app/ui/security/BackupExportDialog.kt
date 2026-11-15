package com.daykhata.app.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
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
import com.daykhata.app.domain.backup.BackupCrypto
import com.daykhata.app.ui.theme.AmberWarning
import com.daykhata.app.ui.theme.PrimaryBlue
import com.daykhata.app.ui.theme.RedNegative

@Composable
fun BackupExportDialog(
    onConfirmExport: (passphrase: CharArray) -> Unit,
    onDismiss: () -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Encrypted Backup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Create a strong passphrase to encrypt your backup (AES-256-GCM).",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = {
                        passphrase = it
                        errorMessage = null
                    },
                    label = { Text("Backup Passphrase (min 8 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassphrase,
                    onValueChange = {
                        confirmPassphrase = it
                        errorMessage = null
                    },
                    label = { Text("Confirm Passphrase") },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = AmberWarning,
                        modifier = Modifier.weight(0.1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "If you forget this passphrase, the backup cannot be recovered. DayKhata does not store your passphrase.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(0.9f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passphrase.length < BackupCrypto.MIN_PASSPHRASE_LENGTH) {
                        errorMessage = "Passphrase must be at least ${BackupCrypto.MIN_PASSPHRASE_LENGTH} characters."
                    } else if (passphrase != confirmPassphrase) {
                        errorMessage = "Passphrases do not match."
                    } else {
                        val passArray = passphrase.toCharArray()
                        passphrase = ""
                        confirmPassphrase = ""
                        onConfirmExport(passArray)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Export Backup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
