package com.daykhata.app.ui.add_expense

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daykhata.app.ui.theme.AmberWarning
import com.daykhata.app.ui.theme.AmberWarningBg
import com.daykhata.app.ui.theme.PrimaryBlue
import com.daykhata.app.ui.theme.PurplePersonal
import com.daykhata.app.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: AddExpenseViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AddExpenseEvent.ExpenseSaved -> onNavigateBack()
                is AddExpenseEvent.ShowToast -> {}
            }
        }
    }

    // Cut-off Warning Confirmation Dialog
    if (uiState.showCutOffConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCutOffDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = AmberWarning
                )
            },
            title = {
                Text(text = "Cut-off Limit Reached", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Your current home balance (₹${uiState.currentHomeBalance}) has reached or is below the cut-off limit of ₹${uiState.cutOffLimit}.\n\nAre you sure you want to add this expense?"
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onConfirmSaveDespiteCutOff() },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                ) {
                    Text("Confirm & Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCutOffDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditing) "Edit Expense" else "Add Expense",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cut-off status badge
            if (uiState.isCutOffReached) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AmberWarningBg),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AmberWarning
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cut-off limit reached (Remaining: ₹${uiState.currentHomeBalance})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = AmberWarning
                        )
                    }
                }
            }

            // Real Amount Input (What I actually spent)
            OutlinedTextField(
                value = uiState.realAmount,
                onValueChange = { viewModel.onRealAmountChange(it) },
                label = { Text("Real Amount Spent (₹) *") },
                placeholder = { Text("e.g. 500") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.realAmountError != null,
                supportingText = {
                    uiState.realAmountError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    } ?: Text("What was actually spent (private)")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Home Amount Input (What is shown to family)
            OutlinedTextField(
                value = uiState.homeAmount,
                onValueChange = { viewModel.onHomeAmountChange(it) },
                label = { Text("Home Amount (₹) (Optional)") },
                placeholder = { Text("Leave blank to match real amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.homeAmountError != null,
                supportingText = {
                    uiState.homeAmountError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    } ?: Text("Amount shown to family (defaults to real amount if blank)")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Home Label Input (Shown to family)
            OutlinedTextField(
                value = uiState.homeLabel,
                onValueChange = { viewModel.onHomeLabelChange(it) },
                label = { Text("Home Label *") },
                placeholder = { Text("e.g. Groceries, Vegetables, Fuel") },
                isError = uiState.homeLabelError != null,
                supportingText = {
                    uiState.homeLabelError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    } ?: Text("Label visible to family")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Personal Label Input (Private notes)
            OutlinedTextField(
                value = uiState.personalLabel,
                onValueChange = { viewModel.onPersonalLabelChange(it) },
                label = { Text("Personal Label (Optional)") },
                placeholder = { Text("Private note (e.g. Groceries + chocolate)") },
                supportingText = {
                    Text("Private note for yourself (only shown in Personal mode)", color = PurplePersonal)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date Picker Field
            val calendar = Calendar.getInstance().apply { timeInMillis = uiState.date }
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selected = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    }
                    viewModel.onDateChange(selected.timeInMillis)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            OutlinedButton(
                onClick = { datePickerDialog.show() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Date: ${DateUtils.formatDate(uiState.date)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Expense Button
            Button(
                onClick = { viewModel.onSaveClicked() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (uiState.isEditing) "Update Expense" else "Save Expense",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
