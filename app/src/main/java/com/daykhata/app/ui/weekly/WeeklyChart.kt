package com.daykhata.app.ui.weekly

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daykhata.app.domain.model.HomeWeekChartData
import com.daykhata.app.domain.model.PersonalWeekChartData
import com.daykhata.app.ui.theme.PrimaryBlue
import com.daykhata.app.ui.theme.PurplePersonal
import com.daykhata.app.ui.theme.PurplePersonalLight
import com.daykhata.app.ui.theme.SecondaryTeal
import com.daykhata.app.util.CurrencyUtils

@Composable
fun HomeWeekBarChart(
    chartData: HomeWeekChartData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly Home Spending",
                fontSize = 15.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (chartData.maxAmount == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses this week",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val bottomLabelSpace = 40f
                    val topPadding = 30f
                    val chartHeight = canvasHeight - bottomLabelSpace - topPadding

                    val slotWidth = canvasWidth / 7f
                    val barWidth = slotWidth * 0.45f
                    val maxVal = chartData.maxAmount.toFloat().coerceAtLeast(1f)

                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    val valuePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    chartData.bars.forEachIndexed { index, bar ->
                        val slotLeft = index * slotWidth
                        val barLeft = slotLeft + (slotWidth - barWidth) / 2f
                        val ratio = (bar.homeAmount.toFloat() / maxVal).coerceIn(0f, 1f)
                        val barHeight = chartHeight * ratio

                        val barTop = topPadding + (chartHeight - barHeight)

                        if (bar.homeAmount > 0) {
                            drawRoundRect(
                                color = PrimaryBlue,
                                topLeft = Offset(barLeft, barTop),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(8f, 8f)
                            )

                            // Value above bar
                            drawContext.canvas.nativeCanvas.drawText(
                                "₹${CurrencyUtils.formatIndianNumber(bar.homeAmount.toLong())}",
                                barLeft + barWidth / 2f,
                                barTop - 8f,
                                valuePaint
                            )
                        }

                        // Day label below bar
                        drawContext.canvas.nativeCanvas.drawText(
                            bar.dayLabel,
                            barLeft + barWidth / 2f,
                            canvasHeight - 10f,
                            textPaint
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalWeekBarChart(
    chartData: PersonalWeekChartData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Real vs Home (Savings Gap)",
                    fontSize = 15.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = PurplePersonal
                )

                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(PurplePersonal, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Real", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(SecondaryTeal, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Home", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (chartData.maxAmount == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses this week",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val bottomLabelSpace = 40f
                    val topPadding = 30f
                    val chartHeight = canvasHeight - bottomLabelSpace - topPadding

                    val slotWidth = canvasWidth / 7f
                    val individualBarWidth = slotWidth * 0.28f
                    val barGap = 4f
                    val totalBarsWidth = individualBarWidth * 2f + barGap
                    val maxVal = chartData.maxAmount.toFloat().coerceAtLeast(1f)

                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    chartData.bars.forEachIndexed { index, bar ->
                        val slotLeft = index * slotWidth
                        val groupLeft = slotLeft + (slotWidth - totalBarsWidth) / 2f

                        // Real bar (Left)
                        val realLeft = groupLeft
                        val realRatio = (bar.realAmount.toFloat() / maxVal).coerceIn(0f, 1f)
                        val realHeight = chartHeight * realRatio
                        val realTop = topPadding + (chartHeight - realHeight)

                        if (bar.realAmount > 0) {
                            drawRoundRect(
                                color = PurplePersonal,
                                topLeft = Offset(realLeft, realTop),
                                size = Size(individualBarWidth, realHeight),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }

                        // Home bar (Right, showing the contrast/gap)
                        val homeLeft = realLeft + individualBarWidth + barGap
                        val homeRatio = (bar.homeAmount.toFloat() / maxVal).coerceIn(0f, 1f)
                        val homeHeight = chartHeight * homeRatio
                        val homeTop = topPadding + (chartHeight - homeHeight)

                        if (bar.homeAmount > 0) {
                            drawRoundRect(
                                color = SecondaryTeal,
                                topLeft = Offset(homeLeft, homeTop),
                                size = Size(individualBarWidth, homeHeight),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }

                        // Day label below group
                        drawContext.canvas.nativeCanvas.drawText(
                            bar.dayLabel,
                            groupLeft + totalBarsWidth / 2f,
                            canvasHeight - 10f,
                            textPaint
                        )
                    }
                }
            }
        }
    }
}
