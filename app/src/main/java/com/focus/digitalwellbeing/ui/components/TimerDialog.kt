package com.focus.digitalwellbeing.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.focus.digitalwellbeing.data.model.AppLimit

@Composable
fun TimerDialog(
    appName: String,
    currentUsageMillis: Long,
    currentLimit: AppLimit?,
    currentBalance: Int,
    onDismiss: () -> Unit,
    onSetTimer: (limitMillis: Long) -> Unit,
    onDeleteTimer: () -> Unit
) {
    var selectedLimitMillis by remember { mutableStateOf<Long?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    val presetOptions = listOf(
        "15m" to 15 * 60 * 1000L,
        "30m" to 30 * 60 * 1000L,
        "1h" to 60 * 60 * 1000L,
        "2h" to 2 * 60 * 60 * 1000L
    )
    
    val cost = calculateTimerCost(currentLimit?.limitMillis, selectedLimitMillis)
    val hasBalance = currentBalance >= cost
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        WellbeingCard(modifier = Modifier
            .fillMaxWidth(0.9f)
            .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Set Timer for $appName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Current usage
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Usage Today:")
                        Text(
                            formatDuration(currentUsageMillis),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Set Daily Limit:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetOptions.forEach { (label, millis) ->
                        val isSelected = selectedLimitMillis == millis
                        OutlinedButton(
                            onClick = { selectedLimitMillis = millis },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                
                if (selectedLimitMillis != null && cost > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Cost warning
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasBalance) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                               else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (hasBalance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (currentLimit == null) "Setting new timer" else "Adjusting timer",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasBalance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = if (currentLimit == null) {
                                    "Cost: $cost coins"
                                } else {
                                    val diff = (selectedLimitMillis!! - currentLimit.limitMillis) / (60 * 1000)
                                    "Adding ${diff}m costs $cost coins"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Balance display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Your Balance:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "$currentBalance",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Delete button (if timer exists)
                    if (currentLimit != null) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                    
                    // Set button
                    Button(
                        onClick = {
                            selectedLimitMillis?.let { onSetTimer(it) }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedLimitMillis != null && hasBalance
                    ) {
                        Text(if (currentLimit == null) "Set Timer" else "Update")
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        DeleteTimerConfirmation(
            appName = appName,
            currentBalance = currentBalance,
            onConfirm = {
                showDeleteConfirmation = false
                onDeleteTimer()
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }
}

@Composable
fun DeleteTimerConfirmation(
    appName: String,
    currentBalance: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val deleteCost = 100
    val hasBalance = currentBalance >= deleteCost
    
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .windowInsetsPadding(WindowInsets.systemBars),
        title = { Text("Remove Timer?") },
        text = {
            Column {
                Text("Are you sure you want to remove the timer for $appName?")
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasBalance) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                           else MaterialTheme.colorScheme.errorContainer
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "This will cost $deleteCost coins",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Your Balance:")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                )
                                Text("$currentBalance", fontWeight = FontWeight.Bold)
                            }
                        }
                        if (!hasBalance) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Insufficient balance!",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = hasBalance,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Remove ($deleteCost")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onError
                    )
                    Text(")")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun calculateTimerCost(currentLimitMillis: Long?, newLimitMillis: Long?): Int {
    if (newLimitMillis == null) return 0
    
    if (currentLimitMillis == null) {
        // Setting new timer - cost per minute
        return (newLimitMillis / (60 * 1000)).toInt()
    }
    
    val diff = newLimitMillis - currentLimitMillis
    if (diff <= 0) {
        // Reducing timer is free
        return 0
    }
    
    // Adding time costs 1 coin per minute
    return (diff / (60 * 1000)).toInt()
}

private fun formatDuration(millis: Long): String {
    val hours = millis / (1000 * 60 * 60)
    val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        millis > 0 -> "<1m"
        else -> "0m"
    }
}

