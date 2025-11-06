package com.example.digitalwellbeing.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    /**
     * Get start of today (midnight) in device's local timezone
     * This returns the timestamp for 00:00:00.000 of the current day
     */
    fun getStartOfToday(): Long {
        val calendar = Calendar.getInstance()
        // Clear time fields to get midnight
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Get start of yesterday (midnight) in device's local timezone
     * This returns the timestamp for 00:00:00.000 of yesterday
     */
    fun getStartOfYesterday(): Long {
        val calendar = Calendar.getInstance()
        // First go to midnight today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        // Then subtract one day
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return calendar.timeInMillis
    }

    /**
     * Get end of yesterday
     */
    fun getEndOfYesterday(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * Format date as "Thursday, October 30"
     */
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Normalize any timestamp to start of its day (midnight) in local timezone
     * Useful for ensuring consistent day boundaries
     */
    fun normalizeToStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Get end of day (23:59:59.999) for a given timestamp in local timezone
     */
    fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * Get start of current week
     */
    fun getStartOfWeek(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Get start of last week
     */
    fun getStartOfLastWeek(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
