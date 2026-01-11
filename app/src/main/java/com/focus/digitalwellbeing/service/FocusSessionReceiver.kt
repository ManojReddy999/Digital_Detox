package com.focus.digitalwellbeing.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.focus.digitalwellbeing.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FocusSessionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val focusGroupId = intent.getLongExtra("FOCUS_GROUP_ID", -1)
        val durationMinutes = intent.getIntExtra("DURATION_MINUTES", -1)

        if (focusGroupId == -1L) return

        Log.d("FocusSessionReceiver", "Alarm triggered for group $focusGroupId")

        // We need to start the session. Since we can't inject Repository easily here without Hilt/Dagger,
        // we'll instantiate it manually for now. Ideally use Dependency Injection.
        val database = AppDatabase.getDatabase(context)
        val focusDao = database.focusDao()
        
        // We need a scope to run coroutines
        val scope = CoroutineScope(Dispatchers.IO)
        
        scope.launch {
            try {
                val focusGroup = focusDao.getFocusGroupById(focusGroupId)
                if (focusGroup != null) {
                    // Start the session
                    val durationMillis = if (durationMinutes > 0) durationMinutes * 60 * 1000L else null
                    val endTime = durationMillis?.let { System.currentTimeMillis() + it }
                    
                    val session = com.focus.digitalwellbeing.data.model.FocusSession(
                        focusGroupId = focusGroup.id,
                        focusGroupName = focusGroup.name,
                        startTime = System.currentTimeMillis(),
                        endTime = endTime,
                        isActive = true
                    )
                    
                    focusDao.insertSession(session)
                    Log.d("FocusSessionReceiver", "Session started via alarm")


                    
                    // Reschedule for next day
                    val scheduler = FocusScheduler(context)
                    scheduler.scheduleSession(focusGroup)
                }
            } catch (e: Exception) {
                Log.e("FocusSessionReceiver", "Error starting session", e)
            }
        }
    }
}

