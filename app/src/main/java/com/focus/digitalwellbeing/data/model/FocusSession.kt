package com.focus.digitalwellbeing.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val isActive: Boolean = false,
    val startTime: Long = 0,
    val endTime: Long? = null, // Optional end time if timed session
    val focusGroupId: Long? = null,
    val focusGroupName: String? = null
)

