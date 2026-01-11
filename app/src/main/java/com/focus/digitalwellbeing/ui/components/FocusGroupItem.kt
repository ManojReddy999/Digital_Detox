package com.focus.digitalwellbeing.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focus.digitalwellbeing.data.model.FocusGroup
import com.focus.digitalwellbeing.data.model.FocusType
import com.focus.digitalwellbeing.data.model.FocusSession

@Composable
fun FocusGroupItem(
    group: FocusGroup,
    activeSession: FocusSession?,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isTemplate: Boolean = false
) {
    val isActive = activeSession?.focusGroupId == group.id
    
    // Card Container
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Row 1: Name + App Icons + (Template Add Icon)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Name
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                Spacer(modifier = Modifier.width(12.dp))

                // App Icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val maxIconsToShow = 5
                    val appsToShow = group.appPackages.take(maxIconsToShow)
                    val remainingCount = (group.appPackages.size - maxIconsToShow).coerceAtLeast(0)

                    appsToShow.forEach { packageName ->
                        AppIcon(
                            packageName = packageName,
                            appName = "",
                            size = 24.dp,
                            modifier = Modifier
                        )
                    }

                    if (remainingCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$remainingCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    
                    // Template Add Icon
                    if (isTemplate) {
                         Spacer(modifier = Modifier.width(12.dp))
                         Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Template",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Row 2: Metadata + Duration/Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Metadata
                Text(
                    text = buildString {
                        append(if (group.type == FocusType.ALLOWLIST) "Allowlist" else "Blocklist")
                        if (group.scheduledStartTime != null) {
                            append(" \u2022 ${group.scheduledStartTime}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Time/Duration Display
                val timeText = if (isActive) {
                    activeSession?.endTime?.let { endTime ->
                        val remaining = endTime - System.currentTimeMillis()
                        if (remaining > 0) {
                            val hours = (remaining / (60 * 60 * 1000)).toInt()
                            val minutes = ((remaining % (60 * 60 * 1000)) / (60 * 1000)).toInt()
                            if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                        } else "0m"
                    } ?: ""
                } else {
                    if (group.scheduledDurationMinutes != null) {
                        val hours = group.scheduledDurationMinutes / 60
                        val mins = group.scheduledDurationMinutes % 60
                        if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                    } else {
                        "Manual"
                    }
                }
                
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Actions Row (Only show if NOT a template)
            if (!isTemplate) {
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start/Stop Button
                    val isScheduled = group.scheduledStartTime != null
                    
                    Button(
                        onClick = if (isActive) onStopSession else onStartSession,
                        enabled = isActive || !isScheduled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            contentColor = if (isActive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.Delete else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isActive -> "STOP SESSION"
                                isScheduled -> "SCHEDULED"
                                else -> "START FOCUS"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Edit Button
                    OutlinedButton(
                        onClick = onEdit,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = Color.Transparent
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp),
                        enabled = !isActive
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    // Delete Button
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                            containerColor = Color.Transparent
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp),
                        enabled = !isActive
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

