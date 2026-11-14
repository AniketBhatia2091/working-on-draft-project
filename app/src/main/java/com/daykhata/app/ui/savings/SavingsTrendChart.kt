package com.daykhata.app.ui.savings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daykhata.app.domain.model.SavingsTrendPoint
import com.daykhata.app.domain.model.TrendRange
import com.daykhata.app.ui.theme.GreenPositive
import com.daykhata.app.ui.theme.PurplePersonal
import com.daykhata.app.ui.theme.RedNegative
import com.daykhata.app.util.CurrencyUtils
import kotlin.math.abs
import kotlin.math.max

@Composable
fun SavingsTrendChartCard(
    points: List<SavingsTrendPoint>,
    selectedRange: TrendRange,
    onRangeSelected: (TrendRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header & Range Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Savings Trend",
                    fontSize = 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = PurplePersonal
                )

                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(GreenPositive, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Savings", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(PurplePersonal, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cumulative", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Range Segmented Switcher
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TrendRange.entries.forEach { range ->
                        val isSelected = range == selectedRange
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PurplePersonal else Color.Transparent)
                                .clickable { onRangeSelected(range) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (range) {
                                    TrendRange.LAST_7_DAYS -> "7 Days"
                                    TrendRange.LAST_4_WEEKS -> "4 Weeks"
                                    TrendRange.LAST_6_MONTHS -> "6 Months"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Check if all zero
            val hasActivity = points.any { it.savings != 0 }

            if (!hasActivity) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No savings activity in this range",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                SavingsCanvasChart(points = points)
            }
        }
    }
}

@Composable
fun SavingsCanvasChart(
    points: List<SavingsTrendPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val bottomLabelSpace = 32f
        val topPadding = 24f
        val chartHeight = canvasHeight - bottomLabelSpace - topPadding

        val minSavings = points.minOfOrNull { it.savings } ?: 0
        val maxSavings = points.maxOfOrNull { it.savings } ?: 0

        val maxVal = max(0, maxSavings).toFloat()
        val minVal = minSavings.coerceAtMost(0).toFloat()
        val totalSpan = (maxVal - minVal).coerceAtLeast(1f)

        // Baseline Y position
        val baselineY = topPadding + (maxVal / totalSpan) * chartHeight

        val slotCount = points.size
        val slotWidth = canvasWidth / slotCount.toFloat()
        val barWidth = slotWidth * 0.45f

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 26f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        // 1. Draw Zero Baseline Line
        drawLine(
            color = Color.LightGray.copy(alpha = 0.8f),
            start = Offset(0f, baselineY),
            end = Offset(canvasWidth, baselineY),
            strokeWidth = 2f
        )

        // 2. Draw Bars (Positive and Negative)
        points.forEachIndexed { index, point ->
            val slotLeft = index * slotWidth
            val barLeft = slotLeft + (slotWidth - barWidth) / 2f

            if (point.savings > 0) {
                val barHeight = (point.savings.toFloat() / totalSpan) * chartHeight
                val barTop = baselineY - barHeight
                drawRoundRect(
                    color = GreenPositive,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            } else if (point.savings < 0) {
                val barHeight = (abs(point.savings).toFloat() / totalSpan) * chartHeight
                val barTop = baselineY
                drawRoundRect(
                    color = RedNegative,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }

            // Day / Period label at bottom
            drawContext.canvas.nativeCanvas.drawText(
                point.label,
                barLeft + barWidth / 2f,
                canvasHeight - 6f,
                textPaint
            )
        }

        // 3. Draw Cumulative Savings Line (connecting points)
        if (points.size > 1) {
            val minCumulative = points.minOfOrNull { it.cumulativeSavings } ?: 0
            val maxCumulative = points.maxOfOrNull { it.cumulativeSavings } ?: 0
            val cumMax = max(1, max(abs(minCumulative), abs(maxCumulative))).toFloat()

            val linePath = Path()
            points.forEachIndexed { index, point ->
                val slotLeft = index * slotWidth
                val x = slotLeft + slotWidth / 2f
                // Map cumulative savings cleanly into chart height
                val cumRatio = (point.cumulativeSavings.toFloat() / (cumMax * 2f)).coerceIn(-0.5f, 0.5f)
                val y = baselineY - (cumRatio * chartHeight)

                if (index == 0) {
                    linePath.moveTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                }

                // Point dot
                drawCircle(
                    color = PurplePersonal,
                    radius = 4f,
                    center = Offset(x, y)
                )
            }

            drawPath(
                path = linePath,
                color = PurplePersonal,
                style = Stroke(width = 3f)
            )
        }
    }
}
