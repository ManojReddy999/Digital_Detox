package com.focus.digitalwellbeing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import com.focus.digitalwellbeing.data.model.FocusSession
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun EarlyTerminationConfirmationDialog(
    session: FocusSession,
    coinBalance: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onGetCoins: () -> Unit
) {
    val currentTime = System.currentTimeMillis()
    val isIndefinite = session.endTime == null
    
    val remainingMinutes = session.endTime?.let {
        ((it - currentTime) / (60 * 1000)).toInt().coerceAtLeast(0)
    } ?: 0

    val coinCharge = if (isIndefinite) 200 else remainingMinutes
    val canAfford = coinBalance >= coinCharge

    // Format time as "Xh Ym" or "Ym"
    fun formatTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false // Allow custom width/positioning
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        "Stop Focus Session Early?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // Info Rows
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Duration Left Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Duration Left",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (isIndefinite) "Until turned off" else formatTime(remainingMinutes),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Charge Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Early Termination Charge",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$coinCharge coins",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (canAfford) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (!canAfford) {
                        Text(
                            "Insufficient coins! You need $coinCharge coins.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action Buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Continue Session (Secondary Action)
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(
                                "Continue Session",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Stop & Pay (Primary Action)
                        Button(
                            onClick = {
                                if (canAfford) {
                                    onConfirm()
                                } else {
                                    onGetCoins()
                                }
                            },
                            enabled = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                                disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                if (canAfford) "Stop & Pay $coinCharge Coins" else "Get Coins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FocusSessionManager(
    activeSession: FocusSession?,
    coinBalance: Int,
    onStopSession: () -> Unit,
    onStopSessionWithCharge: () -> Unit,
    onGetCoins: () -> Unit,
    content: @Composable (onStopClick: () -> Unit) -> Unit
) {
    var showEarlyTerminationDialog by remember { mutableStateOf(false) }

    val onStopClick = {
        // Show dialog if indefinite (endTime == null) OR if there is time remaining
        val isIndefinite = activeSession?.endTime == null
        val hasTimeRemaining = activeSession?.endTime?.let { it > System.currentTimeMillis() } ?: false
        
        if (isIndefinite || hasTimeRemaining) {
            showEarlyTerminationDialog = true
        } else {
            onStopSession()
        }
    }

    content(onStopClick)

    if (showEarlyTerminationDialog && activeSession != null) {
        EarlyTerminationConfirmationDialog(
            session = activeSession,
            coinBalance = coinBalance,
            onDismiss = { showEarlyTerminationDialog = false },
            onConfirm = {
                onStopSessionWithCharge()
                showEarlyTerminationDialog = false
            },
            onGetCoins = onGetCoins
        )
    }
}


object FocusIcons {
    fun getIcon(name: String): ImageVector {
        return when (name) {
            "Study" -> Icons.AutoMirrored.Filled.MenuBook
            "Work" -> Icons.Default.Work
            "Wake Up" -> Icons.Default.WbSunny
            "Sleep" -> Icons.Default.Bedtime
            "Exercise" -> Icons.Default.FitnessCenter
            "Social Media" -> Icons.AutoMirrored.Filled.Chat
            "Entertainment" -> Icons.Default.Movie
            "Games" -> Icons.Default.Gamepad
            "Productivity" -> Icons.AutoMirrored.Filled.TrendingUp
            "Communication" -> Icons.Default.Call
            "Shopping" -> Icons.Default.ShoppingCart
            "News & Reading" -> Icons.Default.Newspaper
            "Utilities" -> Icons.Default.Build
            "Other" -> Icons.Default.Smartphone
            "Focus" -> Icons.Default.CenterFocusStrong
            else -> Icons.Default.Lens // Default fallback
        }
    }
}
