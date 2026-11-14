package com.daykhata.app.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daykhata.app.ui.theme.RedNegative

@Composable
fun ResetAppLockDialog(
    onConfirmReset: suspend () -> Unit,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var isResetting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = RedNegative
            )
        },
        title = {
            Text(
                text = "Reset App Lock & Purge Data",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = RedNegative
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "To prevent unauthorized bypass of your private financial data, resetting the app lock will permanently wipe ALL expense entries, savings history, and personal settings.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "⚠️ This action cannot be undone. The app will return to its initial setup state.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedNegative
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { isResetting = true },
                enabled = !isResetting,
                colors = ButtonDefaults.buttonColors(containerColor = RedNegative)
            ) {
                Text(if (isResetting) "Purging..." else "Delete All & Reset")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isResetting
            ) {
                Text("Cancel")
            }
        }
    )

    androidx.compose.runtime.LaunchedEffect(isResetting) {
        if (isResetting) {
            onConfirmReset()
            onSuccess()
        }
    }
}
