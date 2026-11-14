package com.daykhata.app.ui.savings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daykhata.app.domain.model.GoalPeriod
import com.daykhata.app.domain.model.SavingsGoalProgress
import com.daykhata.app.domain.model.SavingsSummary
import com.daykhata.app.ui.theme.AmberWarning
import com.daykhata.app.ui.theme.AmberWarningBg
import com.daykhata.app.ui.theme.GreenPositive
import com.daykhata.app.ui.theme.PurplePersonal
import com.daykhata.app.ui.theme.PurplePersonalLight
import com.daykhata.app.ui.theme.RedNegative
import com.daykhata.app.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    viewModel: SavingsViewModel,
    onExitToDashboard: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Safety constraint: If mode switches to HOME while on Savings screen, exit immediately
    LaunchedEffect(uiState.isPersonalMode) {
        if (!uiState.isPersonalMode) {
            onExitToDashboard()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Personal",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Private Savings",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                // Safety: NO share action allowed on Savings screen!
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PurplePersonal)
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
            val summary = uiState.summary

            // Negative Savings Note (if any expense has home < real)
            if (summary?.hasNegativeSavingsExpenses == true) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AmberWarningBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Negative Savings Logged",
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${summary.negativeSavingsCount} expense(s) had actual spending higher than family amount (-${CurrencyUtils.formatIndianRupees(summary.totalNegativeSavingsAmount)}). This is factored into all net figures.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Top Total Savings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PurplePersonalLight),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "TOTAL SAVINGS (CURRENT PERIOD)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePersonal,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatIndianRupees(summary?.currentPeriodSavings ?: 0),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = if ((summary?.currentPeriodSavings ?: 0) >= 0) GreenPositive else RedNegative
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("All-Time Total", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = CurrencyUtils.formatIndianRupees(summary?.allTimeSavings ?: 0),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("This Month's Savings", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = CurrencyUtils.formatIndianRupees(summary?.thisMonthSavings ?: 0),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPositive
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("This Week's Savings", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = CurrencyUtils.formatIndianRupees(summary?.thisWeekSavings ?: 0),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Avg / Profitable Expense", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${CurrencyUtils.formatIndianRupees(summary?.averageSavingsPerProfitableExpense ?: 0)} (${summary?.profitableExpensesCount ?: 0} entries)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Savings Goal Card
            SavingsGoalCard(
                progress = summary?.goalProgress,
                onNavigateToSettings = onNavigateToSettings
            )

            // Savings Trend Chart
            SavingsTrendChartCard(
                points = uiState.trendPoints,
                selectedRange = uiState.selectedRange,
                onRangeSelected = { viewModel.onRangeSelected(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SavingsGoalCard(
    progress: SavingsGoalProgress?,
    onNavigateToSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = PurplePersonal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Savings Target",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (progress != null) {
                    Text(
                        text = when (progress.goal.period) {
                            GoalPeriod.WEEKLY -> "Weekly Goal"
                            GoalPeriod.MONTHLY -> "Monthly Goal"
                            GoalPeriod.NONE -> ""
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (progress == null || !progress.goal.isSet) {
                // Goal not configured
                Text(
                    text = "You haven't set a personal savings target yet. Set a weekly or monthly target to stay motivated!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onNavigateToSettings,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Set Target in Settings")
                }
            } else {
                // Goal is active
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Target: ${CurrencyUtils.formatIndianRupees(progress.goal.amount)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "Saved: ${CurrencyUtils.formatIndianRupees(progress.savedAmount)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (progress.isGoalReached) GreenPositive else PurplePersonal
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val percentage = (progress.progressRatio * 100f).toInt()
                        Text("$percentage%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (progress.isGoalReached) GreenPositive else PurplePersonal)
                        if (progress.isGoalReached) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenPositive, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Goal reached!", fontSize = 12.sp, color = GreenPositive, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Text("${CurrencyUtils.formatIndianRupees(progress.remainingAmount)} remaining", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar clamped to 1.0f visually
                val progressClamped = progress.progressRatio.coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progressClamped },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (progress.isGoalReached) GreenPositive else PurplePersonal,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                if (progress.extraSaved > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🎉 Awesome! You exceeded your target by +${CurrencyUtils.formatIndianRupees(progress.extraSaved)}!",
                        fontSize = 12.sp,
                        color = GreenPositive,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
