package com.daykhata.app.ui.daily_list

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daykhata.app.data.local.Expense
import com.daykhata.app.ui.theme.GreenPositive
import com.daykhata.app.ui.theme.PrimaryBlue
import com.daykhata.app.ui.theme.PurplePersonal
import com.daykhata.app.ui.theme.PurplePersonalLight
import com.daykhata.app.ui.theme.RedNegative
import com.daykhata.app.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyListScreen(
    viewModel: DailyListViewModel,
    onNavigateToEditExpense: (Long) -> Unit,
    onNavigateToAddExpense: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    // Delete confirmation dialog
    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete '${expense.homeLabel}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(expense)
                        expenseToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Daily Expenses", fontWeight = FontWeight.Bold, color = Color.White)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Date Navigation Bar
            val calendar = Calendar.getInstance().apply { timeInMillis = uiState.selectedDate }
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selected = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    }
                    viewModel.onDateSelected(selected.timeInMillis)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.onPreviousDayClicked() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            val isToday = DateUtils.isToday(uiState.selectedDate)
                            Text(
                                text = if (isToday) "Today, ${DateUtils.formatDate(uiState.selectedDate)}" else DateUtils.formatDate(uiState.selectedDate),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick date", modifier = Modifier.size(20.dp))
                            }
                        }

                        IconButton(onClick = { viewModel.onNextDayClicked() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                        }
                    }

                    // Personal mode toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 8.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.isPersonalMode) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (uiState.isPersonalMode) PurplePersonal else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Personal Mode",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (uiState.isPersonalMode) PurplePersonal else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = uiState.isPersonalMode,
                            onCheckedChange = { viewModel.togglePersonalMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PurplePersonal,
                                checkedTrackColor = PurplePersonal.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Day Total summary
                    if (!uiState.isPersonalMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Home Spent:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${uiState.dayHomeTotal}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Home: ₹${uiState.dayHomeTotal}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Real: ₹${uiState.dayRealTotal}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Day Savings:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "₹${uiState.daySavings}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.daySavings >= 0) GreenPositive else RedNegative
                                )
                            }
                        }
                    }
                }
            }

            // Expenses List
            if (uiState.expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No expenses logged for this day",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onNavigateToAddExpense) {
                            Text("Add Expense")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = uiState.expenses, key = { it.id }) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            isPersonalMode = uiState.isPersonalMode,
                            onEdit = { onNavigateToEditExpense(expense.id) },
                            onDelete = { expenseToDelete = expense }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    isPersonalMode: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPersonalMode) PurplePersonalLight.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Home label (always shown)
                Text(
                    text = expense.homeLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Personal label & breakdown (ONLY in Personal mode)
                if (isPersonalMode) {
                    if (expense.personalLabel.isNotBlank() && expense.personalLabel != expense.homeLabel) {
                        Text(
                            text = "Note: ${expense.personalLabel}",
                            fontSize = 12.sp,
                            color = PurplePersonal,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real: ₹${expense.realAmount} | Home: ₹${expense.homeAmount}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val savings = expense.homeAmount - expense.realAmount
                    Text(
                        text = "Difference: ${if (savings >= 0) "+₹$savings" else "-₹${-savings}"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (savings >= 0) GreenPositive else RedNegative
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = DateUtils.formatTime(expense.date),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amount Display
            Column(horizontalAlignment = Alignment.End) {
                // In family mode: show strictly homeAmount
                // In personal mode: show homeAmount as primary with real indicator
                Text(
                    text = "₹${expense.homeAmount}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = PrimaryBlue
                )

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = RedNegative,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
