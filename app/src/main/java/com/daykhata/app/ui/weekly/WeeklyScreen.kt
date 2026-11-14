package com.daykhata.app.ui.weekly

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daykhata.app.domain.model.HomeDayTotal
import com.daykhata.app.domain.model.PersonalDayTotal
import com.daykhata.app.ui.daily_list.SegmentedModeToggle
import com.daykhata.app.ui.daily_list.SharePreviewSheetContent
import com.daykhata.app.ui.theme.GreenPositive
import com.daykhata.app.ui.theme.PrimaryBlue
import com.daykhata.app.ui.theme.PurplePersonal
import com.daykhata.app.ui.theme.PurplePersonalLight
import com.daykhata.app.ui.theme.RedNegative
import com.daykhata.app.util.CurrencyUtils
import com.daykhata.app.util.DateUtils
import com.daykhata.app.util.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScreen(
    viewModel: WeeklyViewModel,
    onNavigateToDailyDate: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is WeeklyEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // Share Preview Bottom Sheet (HOME data only)
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
                    Text("Weekly Log", fontWeight = FontWeight.Bold, color = Color.White)
                },
                actions = {
                    // Share weekly summary (Disabled in Personal Mode)
                    IconButton(
                        onClick = { viewModel.openShareSheet() },
                        enabled = !uiState.isPersonalMode
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share weekly expenses",
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
                .verticalScroll(rememberScrollState())
        ) {
            // Segmented Mode Toggle (Shared App State)
            SegmentedModeToggle(
                currentMode = uiState.viewMode,
                onModeSelected = { viewModel.setViewMode(it) }
            )

            // Personal Mode Warning Banner
            AnimatedVisibility(visible = uiState.isPersonalMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = PurplePersonalLight),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "🔒 Personal Mode active. Weekly sharing is disabled to protect private numbers.",
                        fontSize = 12.sp,
                        color = PurplePersonal,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Week Selector Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.onPreviousWeekClicked() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Week")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.weekRangeLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (!uiState.isCurrentWeek) {
                            TextButton(
                                onClick = { viewModel.onThisWeekClicked() },
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("This week", fontSize = 12.sp)
                            }
                        } else {
                            Text(
                                text = "Current Week",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Next Week is disabled if already on current week
                    IconButton(
                        onClick = { viewModel.onNextWeekClicked() },
                        enabled = !uiState.isCurrentWeek
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Week",
                            tint = if (uiState.isCurrentWeek) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Weekly Totals Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isPersonalMode) PurplePersonalLight.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (!uiState.isPersonalMode) "WEEKLY TOTALS (HOME)" else "WEEKLY TOTALS (PERSONAL)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (!uiState.isPersonalMode) MaterialTheme.colorScheme.onSurfaceVariant else PurplePersonal
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (!uiState.isPersonalMode) {
                        val summary = uiState.homeSummary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Week Home Total", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatIndianRupees(summary?.weekHomeTotal ?: 0),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Remaining Home Balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatIndianRupees(summary?.homeBalance ?: 0),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if ((summary?.homeBalance ?: 0) < 0) RedNegative else GreenPositive
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Average / Day", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${CurrencyUtils.formatIndianRupees(summary?.averagePerDay ?: 0)}/day",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Highest Spending Day", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val highestText = summary?.highestDay?.let {
                                    "${it.dayLabel} (${CurrencyUtils.formatIndianRupees(it.homeTotal)})"
                                } ?: "–"
                                Text(
                                    text = highestText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        val summary = uiState.personalSummary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Week Real Total", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatIndianRupees(summary?.weekRealTotal ?: 0),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePersonal
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Week's Savings (Home - Real)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val savings = summary?.weekSavings ?: 0
                                Text(
                                    text = CurrencyUtils.formatIndianRupees(savings),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (savings >= 0) GreenPositive else RedNegative
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Average / Day (Real)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${CurrencyUtils.formatIndianRupees(summary?.averagePerDay ?: 0)}/day",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Highest Real Day", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val highestText = summary?.highestDay?.let {
                                    "${it.dayLabel} (${CurrencyUtils.formatIndianRupees(it.realTotal)})"
                                } ?: "–"
                                Text(
                                    text = highestText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Chart Section
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (!uiState.isPersonalMode) {
                    uiState.homeChartData?.let {
                        HomeWeekBarChart(chartData = it)
                    }
                } else {
                    uiState.personalChartData?.let {
                        PersonalWeekBarChart(chartData = it)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day-by-Day List Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Day-by-Day Breakdown",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (!uiState.isPersonalMode) {
                        uiState.homeSummary?.dayTotals?.forEach { day ->
                            HomeDayRow(
                                dayTotal = day,
                                onClick = { onNavigateToDailyDate(day.dateMillis) }
                            )
                        }
                    } else {
                        uiState.personalSummary?.dayTotals?.forEach { day ->
                            PersonalDayRow(
                                dayTotal = day,
                                onClick = { onNavigateToDailyDate(day.dateMillis) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HomeDayRow(
    dayTotal: HomeDayTotal,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = dayTotal.dayLabel,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = DateUtils.formatDate(dayTotal.dateMillis),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (dayTotal.homeTotal > 0) CurrencyUtils.formatIndianRupees(dayTotal.homeTotal) else "–",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (dayTotal.homeTotal > 0) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View Day",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PersonalDayRow(
    dayTotal: PersonalDayTotal,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = dayTotal.dayLabel,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = DateUtils.formatDate(dayTotal.dateMillis),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (dayTotal.realTotal > 0) "Real: ${CurrencyUtils.formatIndianRupees(dayTotal.realTotal)}" else "–",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (dayTotal.realTotal > 0) PurplePersonal else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (dayTotal.homeTotal > 0 || dayTotal.realTotal > 0) {
                    val savings = dayTotal.savings
                    Text(
                        text = "Savings: ${if (savings >= 0) "+" else ""}${CurrencyUtils.formatIndianRupees(savings)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (savings >= 0) GreenPositive else RedNegative
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View Day",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
