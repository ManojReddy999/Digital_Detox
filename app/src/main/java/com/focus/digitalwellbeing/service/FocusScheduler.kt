package com.focus.digitalwellbeing.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.focus.digitalwellbeing.data.model.FocusGroup
import java.util.Calendar

class FocusScheduler(private val context: Context) {

    fun scheduleSession(focusGroup: FocusGroup) {
        if (focusGroup.scheduledStartTime == null) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FocusSessionReceiver::class.java).apply {
            putExtra("FOCUS_GROUP_ID", focusGroup.id)
            putExtra("DURATION_MINUTES", focusGroup.scheduledDurationMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            focusGroup.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Parse start time
        val parts = focusGroup.scheduledStartTime.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If time has passed for today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact if permission missing (shouldn't happen if we request it)
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("FocusScheduler", "Scheduled session for group ${focusGroup.name} at ${calendar.time}")
        } catch (e: SecurityException) {
            Log.e("FocusScheduler", "Failed to schedule alarm: permission denied", e)
        }
    }

    fun cancelSession(focusGroup: FocusGroup) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FocusSessionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            focusGroup.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d("FocusScheduler", "Cancelled session for group ${focusGroup.name}")
    }
}

