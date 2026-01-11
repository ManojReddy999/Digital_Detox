package com.focus.digitalwellbeing.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focus.digitalwellbeing.ui.MainViewModel
import com.focus.digitalwellbeing.ui.components.AppHeader
import com.focus.digitalwellbeing.ui.theme.BackgroundBlack
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import com.focus.digitalwellbeing.ui.theme.CardBackground
import com.focus.digitalwellbeing.ui.theme.CardBorder
import com.focus.digitalwellbeing.ui.theme.DividerAlpha
import com.focus.digitalwellbeing.ui.theme.NeonGreen
import com.focus.digitalwellbeing.ui.theme.TextSecondaryAlpha
import com.focus.digitalwellbeing.util.DateUtils



@Composable
fun ProfileScreen(
    viewModel: MainViewModel
) {
    val streakCount by viewModel.streakCount.collectAsState()
    val coinBalance by viewModel.coinBalance.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val isDarkTheme = currentTheme == com.focus.digitalwellbeing.data.repository.AppTheme.DARK

    val displayName = userName ?: "User"
    val userInitial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        AppHeader(
            title = displayName,
            action = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitial,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp), // Spacing handled by components
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // --- Hero Stat (Balance) ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "BALANCE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$coinBalance",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 72.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-3).sp,
                                lineHeight = 72.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "coins",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                    }
                }
            }

            // --- Streaks Section ---
            item {
                Text(
                    text = "Streaks",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        DataRowItem(
                            label = "Timer Compliance", 
                            value = "$streakCount days",
                            showDivider = false
                        )
                        DataRowItem(
                            label = "Focus Sessions", 
                            value = "0 days",
                            showDivider = false
                        )
                        DataRowItem(
                            label = "Screen Time Goal", 
                            value = "0 days",
                            showDivider = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // --- Settings Section ---
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        SettingsRowItem(
                            label = "Dark Mode",
                            showDivider = false,
                            trailing = {
                                Switch(
                                    checked = isDarkTheme,
                                    onCheckedChange = { viewModel.toggleTheme() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        )
                        SettingsRowItem(
                            label = "Notifications",
                            showDivider = false,
                            trailing = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        SettingsRowItem(
                            label = "Privacy",
                            showDivider = false,
                            trailing = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // User Info
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Version 1.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun DataRowItem(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.primary,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showDivider) Modifier.drawBehindBottomBorder(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else Modifier)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = valueColor
        )
    }
}

@Composable
fun SettingsRowItem(
    label: String,
    trailing: @Composable () -> Unit,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .then(if (showDivider) Modifier.drawBehindBottomBorder(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), startX = 20.dp) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        trailing()
    }
}

@Composable
fun Modifier.drawBehindBottomBorder(
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    strokeWidth: Dp = 1.dp,
    startX: Dp = 0.dp
): Modifier = this.drawBehind {
    val strokePx = strokeWidth.toPx()
    val startXPx = startX.toPx()
    drawLine(
        color = color,
        start = Offset(startXPx, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = strokePx
    )
}

