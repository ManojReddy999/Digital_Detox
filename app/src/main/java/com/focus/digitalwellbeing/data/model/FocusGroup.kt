package com.focus.digitalwellbeing.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FocusType {
    ALLOWLIST,
    BLOCKLIST
}

@Entity(tableName = "focus_groups")
data class FocusGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: FocusType,
    val appPackages: List<String>, // List of package names
    val icon: String = "Focus", // Default icon key
    val scheduledStartTime: String? = null, // Format "HH:mm"
    val scheduledDurationMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

