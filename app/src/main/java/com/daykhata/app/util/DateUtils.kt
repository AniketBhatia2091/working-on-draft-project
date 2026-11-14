package com.daykhata.app.util

import com.daykhata.app.domain.model.PeriodType
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale

object DateUtils {

    fun getStartOfDay(epochMillis: Long): Long {
        val localDate = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getEndOfDay(epochMillis: Long): Long {
        val localDate = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1_000_000).toInstant().toEpochMilli()
    }

    /**
     * Calculates the [startEpoch, endEpoch] range for the current period based on period type.
     */
    fun calculatePeriodRange(periodType: PeriodType, customStartDate: Long? = null): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)

        return when (periodType) {
            PeriodType.WEEKLY -> {
                val startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                val startEpoch = startOfWeek.atStartOfDay(zone).toInstant().toEpochMilli()
                val endEpoch = endOfWeek.plusDays(1).atStartOfDay(zone).minusNanos(1_000_000).toInstant().toEpochMilli()
                Pair(startEpoch, endEpoch)
            }
            PeriodType.MONTHLY -> {
                val startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth())
                val endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth())
                val startEpoch = startOfMonth.atStartOfDay(zone).toInstant().toEpochMilli()
                val endEpoch = endOfMonth.plusDays(1).atStartOfDay(zone).minusNanos(1_000_000).toInstant().toEpochMilli()
                Pair(startEpoch, endEpoch)
            }
        }
    }

    fun formatDate(epochMillis: Long): String {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return formatter.format(Date(epochMillis))
    }

    fun formatTime(epochMillis: Long): String {
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return formatter.format(Date(epochMillis))
    }

    fun isToday(epochMillis: Long): Boolean {
        val zone = ZoneId.systemDefault()
        val target = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        return target.isEqual(today)
    }
}
