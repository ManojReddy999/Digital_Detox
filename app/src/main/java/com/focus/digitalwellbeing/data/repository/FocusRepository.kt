package com.focus.digitalwellbeing.data.repository

import com.focus.digitalwellbeing.data.database.FocusDao
import com.focus.digitalwellbeing.data.model.FocusGroup
import com.focus.digitalwellbeing.data.model.FocusSession
import kotlinx.coroutines.flow.Flow

import com.focus.digitalwellbeing.service.FocusScheduler
import android.content.Context

class FocusRepository(
    private val focusDao: FocusDao,
    private val scheduler: FocusScheduler,
    private val context: Context
) {

    // Active session from DB
    val activeSession: Flow<FocusSession?> = focusDao.getActiveSession()

    // Most recent completed session
    val mostRecentSession: Flow<FocusSession?> = focusDao.getMostRecentSession()

    fun getAllFocusGroups(): Flow<List<FocusGroup>> = focusDao.getAllFocusGroups()

    suspend fun createFocusGroup(focusGroup: FocusGroup) {
        val id = focusDao.insertFocusGroup(focusGroup)
        // Schedule if needed
        scheduler.scheduleSession(focusGroup.copy(id = id))
    }
    
    suspend fun updateFocusGroup(focusGroup: FocusGroup) {
        focusDao.updateFocusGroup(focusGroup)
        // Reschedule (cancel old, schedule new)
        scheduler.cancelSession(focusGroup)
        scheduler.scheduleSession(focusGroup)
    }

    suspend fun deleteFocusGroup(focusGroup: FocusGroup) {
        focusDao.deleteFocusGroup(focusGroup)
        scheduler.cancelSession(focusGroup)
    }

    suspend fun startSession(focusGroup: FocusGroup, durationMillis: Long? = null) {
        // First, end any existing session
        val currentSession = focusDao.getActiveSessionSync()
        if (currentSession != null) {
            focusDao.updateSession(currentSession.copy(isActive = false, endTime = System.currentTimeMillis()))
        }

        val endTime = if (durationMillis != null) System.currentTimeMillis() + durationMillis else null
        val newSession = FocusSession(
            isActive = true,
            startTime = System.currentTimeMillis(),
            endTime = endTime,
            focusGroupId = focusGroup.id,
            focusGroupName = focusGroup.name
        )
        focusDao.insertSession(newSession)
        

    }

    suspend fun stopSession() {
        val currentSession = focusDao.getActiveSessionSync()
        if (currentSession != null) {
            focusDao.updateSession(currentSession.copy(isActive = false, endTime = System.currentTimeMillis()))
        }
        

    }

    suspend fun getFocusGroupById(id: Long): FocusGroup? {
        return focusDao.getFocusGroupById(id)
    }
}

