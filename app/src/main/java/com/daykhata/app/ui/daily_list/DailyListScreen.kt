package com.daykhata.app.ui.daily_list

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daykhata.app.data.local.Expense
import com.daykhata.app.domain.model.FamilyContact
import com.daykhata.app.domain.model.ViewMode
import com.daykhata.app.ui.theme.GreenPositive
import com.daykhata.app.ui.theme.PrimaryBlue
import com.daykhata.app.ui.theme.PurplePersonal
import com.daykhata.app.ui.theme.PurplePersonalLight
import com.daykhata.app.ui.theme.RedNegative
import com.daykhata.app.util.CurrencyUtils
import com.daykhata.app.util.DateUtils
import com.daykhata.app.util.ShareUtils
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
    val snackbarHostState = remember { SnackbarHostState() }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DailyListEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

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

    // Share Preview Bottom Sheet
    if (uiState.isShareSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissShareSheet() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SharePreviewSheetContent(
                message = uiState.shareMessage,
                familyContacts = uiState.familyContacts,
                onShareWhatsApp = {
                    ShareUtils.shareToWhatsApp(context, uiState.shareMessage)
                    viewModel.dismissShareSheet()
                },
                onSendToContact = { contact ->
                    ShareUtils.sendToWhatsAppNumber(context, contact.phone, uiState.shareMessage)
                    viewModel.dismissShareSheet()
                },
                onCopy = {
                    ShareUtils.copyToClipboard(context, uiState.shareMessage)
                    viewModel.onMessageCopied()
                    viewModel.dismissShareSheet()
                }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Daily Expenses", fontWeight = FontWeight.Bold, color = Color.White)
                },
                actions = {
                    // Share Action (Disabled in Personal Mode)
                    IconButton(
                        onClick = { viewModel.openShareSheet() },
                        enabled = !uiState.isPersonalMode
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share daily expenses",
                            tint = if (uiState.isPersonalMode) Color.White.copy(alpha = 0.38f) else Color.White
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
        ) {
            // Segmented Mode Toggle (Home | Personal)
            SegmentedModeToggle(
                currentMode = uiState.viewMode,
                onModeSelected = { viewModel.setViewMode(it) }
            )

            // Personal Mode Share Disabled Notice
            AnimatedVisibility(visible = uiState.isPersonalMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = PurplePersonalLight),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "🔒 Personal Mode active. Sharing is disabled to protect private data.",
                        fontSize = 12.sp,
                        color = PurplePersonal,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Date Navigation Card
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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

                    Spacer(modifier = Modifier.height(4.dp))

                    // Summary Section
                    if (!uiState.isPersonalMode) {
                        // HOME Mode Summary: Day Total (Home) and Home Balance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Day Total (Home)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatIndianRupees(uiState.dayHomeTotal),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Remaining Home Balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatIndianRupees(uiState.homeBalance),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.homeBalance < 0) RedNegative else GreenPositive
                                )
                            }
                        }
                    } else {
                        // PERSONAL Mode Summary: Day Total (Real) and Day Savings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Day Total (Real)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatIndianRupees(uiState.dayRealTotal),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePersonal
                                )
                                Text("Home total: ${CurrencyUtils.formatIndianRupees(uiState.dayHomeTotal)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Day's Savings (Home - Real)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatIndianRupees(uiState.daySavings),
                                    fontSize = 17.sp,
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
                        DailyExpenseItemCard(
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
fun SegmentedModeToggle(
    currentMode: ViewMode,
    onModeSelected: (ViewMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Home Mode Tab
            val isHome = currentMode == ViewMode.HOME
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isHome) PrimaryBlue else Color.Transparent)
                    .clickable { onModeSelected(ViewMode.HOME) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏠 Home Mode",
                    fontWeight = if (isHome) FontWeight.Bold else FontWeight.Normal,
                    color = if (isHome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            // Personal Mode Tab
            val isPersonal = currentMode == ViewMode.PERSONAL
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isPersonal) PurplePersonal else Color.Transparent)
                    .clickable { onModeSelected(ViewMode.PERSONAL) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👤 Personal Mode",
                    fontWeight = if (isPersonal) FontWeight.Bold else FontWeight.Normal,
                    color = if (isPersonal) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun DailyExpenseItemCard(
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
                if (!isPersonalMode) {
                    // HOME MODE: strictly homeLabel and homeAmount
                    Text(
                        text = expense.homeLabel,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = DateUtils.formatTime(expense.date),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // PERSONAL MODE: realAmount, personalLabel, difference
                    Text(
                        text = if (expense.personalLabel.isNotBlank()) expense.personalLabel else expense.homeLabel,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (expense.personalLabel.isNotBlank() && expense.personalLabel != expense.homeLabel) {
                        Text(
                            text = "Home label: ${expense.homeLabel}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val savings = expense.homeAmount - expense.realAmount
                    Text(
                        text = "Real: ${CurrencyUtils.formatIndianRupees(expense.realAmount)} | Home: ${CurrencyUtils.formatIndianRupees(expense.homeAmount)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Diff: ${if (savings >= 0) "+" else ""}${CurrencyUtils.formatIndianRupees(savings)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (savings >= 0) GreenPositive else RedNegative
                    )
                }
            }

            // Primary Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (!isPersonalMode) {
                        CurrencyUtils.formatIndianRupees(expense.homeAmount)
                    } else {
                        CurrencyUtils.formatIndianRupees(expense.realAmount)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (!isPersonalMode) PrimaryBlue else PurplePersonal
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

@Composable
fun SharePreviewSheetContent(
    message: String,
    familyContacts: List<FamilyContact>,
    onShareWhatsApp: () -> Unit,
    onSendToContact: (FamilyContact) -> Unit,
    onCopy: () -> Unit
) {
    var isContactsDropdownOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Share Home Expenses",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        // Read-only Message Preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "PREVIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Button 1: WhatsApp
            Button(
                onClick = onShareWhatsApp,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("WhatsApp", fontSize = 13.sp)
            }

            // Button 2: Saved person
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = {
                        if (familyContacts.size == 1) {
                            onSendToContact(familyContacts.first())
                        } else if (familyContacts.size > 1) {
                            isContactsDropdownOpen = true
                        }
                    },
                    enabled = familyContacts.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saved person", fontSize = 13.sp)
                }

                if (familyContacts.size > 1) {
                    DropdownMenu(
                        expanded = isContactsDropdownOpen,
                        onDismissRequest = { isContactsDropdownOpen = false }
                    ) {
                        familyContacts.forEach { contact ->
                            DropdownMenuItem(
                                text = { Text("${contact.name} (${contact.phone})") },
                                onClick = {
                                    isContactsDropdownOpen = false
                                    onSendToContact(contact)
                                }
                            )
                        }
                    }
                }
            }

            // Button 3: Copy
            OutlinedButton(
                onClick = onCopy,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy", fontSize = 13.sp)
            }
        }

        // Hint if no saved contacts
        if (familyContacts.isEmpty()) {
            Text(
                text = "💡 Tip: Add family contacts in Settings to send directly with one tap.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
