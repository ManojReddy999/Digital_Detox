package com.focus.digitalwellbeing.data.database

import androidx.room.*
import com.focus.digitalwellbeing.data.model.FocusGroup
import com.focus.digitalwellbeing.data.model.FocusSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_groups ORDER BY createdAt DESC")
    fun getAllFocusGroups(): Flow<List<FocusGroup>>

    @Query("SELECT * FROM focus_groups WHERE id = :id")
    suspend fun getFocusGroupById(id: Long): FocusGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusGroup(focusGroup: FocusGroup): Long
    
    @Update
    suspend fun updateFocusGroup(focusGroup: FocusGroup)

    @Delete
    suspend fun deleteFocusGroup(focusGroup: FocusGroup)
    
    @Query("DELETE FROM focus_groups WHERE id = :id")
    suspend fun deleteFocusGroupById(id: Long)

    // Session Management
    @Query("SELECT * FROM focus_sessions WHERE isActive = 1 LIMIT 1")
    fun getActiveSession(): Flow<FocusSession?>

    @Query("SELECT * FROM focus_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSessionSync(): FocusSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession)

    @Update
    suspend fun updateSession(session: FocusSession)

    @Query("SELECT * FROM focus_sessions WHERE isActive = 0 ORDER BY startTime DESC LIMIT 1")
    fun getMostRecentSession(): Flow<FocusSession?>
}

