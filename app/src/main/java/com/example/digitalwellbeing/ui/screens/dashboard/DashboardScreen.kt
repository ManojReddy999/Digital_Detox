package com.example.digitalwellbeing.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalwellbeing.ui.MainViewModel
import com.example.digitalwellbeing.ui.components.*
import com.example.digitalwellbeing.ui.theme.TextPrimary
import com.example.digitalwellbeing.ui.theme.TextSecondary
import com.example.digitalwellbeing.util.DateUtils

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.dashboardState.collectAsState()
    var showTimerDialog by remember { mutableStateOf(false) }
    var selectedAppForTimer by remember { mutableStateOf<com.example.digitalwellbeing.data.model.AppUsageInfo?>(null) }
    var showAllApps by remember { mutableStateOf(false) }

    // Debug logging
    android.util.Log.d("DashboardScreen", "UI State - hasTimersSet: ${uiState.hasTimersSet}, accessibilityEnabled: ${uiState.accessibilityEnabled}, should show card: ${uiState.hasTimersSet && !uiState.accessibilityEnabled}")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp)
        ) {
            Text(
                text = "Focus",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.8).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = DateUtils.formatDate(System.currentTimeMillis()),
                fontSize = 15.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.1).sp
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 100.dp)
        ) {
            // Debug logging
            android.util.Log.d("Dashboard", "State: hasTimersSet=${uiState.hasTimersSet}, accessibilityEnabled=${uiState.accessibilityEnabled}, appLimitsCount=${uiState.appLimits.size}")

            if (!uiState.usagePermissionGranted) {
                UsagePermissionCard(
                    onGrantClick = { viewModel.requestUsagePermission() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Show accessibility permission card if accessibility is not enabled
            if (!uiState.accessibilityEnabled) {
                AccessibilityPermissionCard(
                    onGrantClick = { viewModel.requestAccessibilityPermission() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.usagePermissionGranted) {
                // Screen Time Card
                WellbeingCard {
                    val totalUsage = uiState.todayStats?.totalUsageFormatted ?: "0h 0m"
                    val progress = uiState.todayStats?.let {
                        (it.totalUsageTimeMillis.toFloat() / (6 * 60 * 60 * 1000)).coerceAtMost(1f)
                    } ?: 0f

                    StatCircle(
                        time = totalUsage,
                        label = "Screen time",
                        progress = progress,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    if (uiState.percentageChange > 0) {
                        Text(
                            text = "${uiState.percentageChange}% less than yesterday",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(
                            text = uiState.percentageChange.toString(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Most Used Apps Card
                WellbeingCard {
                    CardHeader(
                        title = "Most Used",
                        actionText = "See all",
                        onActionClick = onNavigateToStats
                    )

                    // Separate apps with usage from apps without usage
                    val appsWithUsage = uiState.topApps.filter { it.usageTimeMillis > 0 }
                    val appsWithoutUsage = uiState.topApps.filter { it.usageTimeMillis == 0L }

                    val maxUsage = uiState.topApps.maxOfOrNull { it.usageTimeMillis } ?: 1L

                    // Show apps with usage
                    appsWithUsage.forEachIndexed { index, app ->
                        AppUsageItem(
                            appInfo = app,
                            maxUsage = maxUsage,
                            timerLimitMillis = uiState.appLimits[app.packageName],
                            onSetTimerClick = {
                                selectedAppForTimer = app
                                showTimerDialog = true
                            }
                        )
                        if (index < appsWithUsage.size - 1 || appsWithoutUsage.isNotEmpty()) {
                            Divider(color = com.example.digitalwellbeing.ui.theme.Border)
                        }
                    }

                    // Show "Show all apps" button if there are apps without usage
                    if (appsWithoutUsage.isNotEmpty()) {
                        AnimatedVisibility(
                            visible = !showAllApps,
                            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        onClick = { showAllApps = true },
                                        indication = rememberRipple(bounded = true),
                                        interactionSource = remember { MutableInteractionSource() }
                                    )
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Show all apps (${appsWithoutUsage.size})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Show apps without usage when expanded
                        AnimatedVisibility(
                            visible = showAllApps,
                            enter = expandVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                            exit = shrinkVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                        ) {
                            Column {
                                appsWithoutUsage.forEachIndexed { index, app ->
                                    AppUsageItem(
                                        appInfo = app,
                                        maxUsage = maxUsage,
                                        timerLimitMillis = uiState.appLimits[app.packageName],
                                        onSetTimerClick = {
                                            selectedAppForTimer = app
                                            showTimerDialog = true
                                        }
                                    )
                                    if (index < appsWithoutUsage.size - 1) {
                                        Divider(color = com.example.digitalwellbeing.ui.theme.Border)
                                    }
                                }

                                // Show "Hide apps" button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            onClick = { showAllApps = false },
                                            indication = rememberRipple(bounded = true),
                                            interactionSource = remember { MutableInteractionSource() }
                                        )
                                        .padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Hide apps",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.topApps.isEmpty()) {
                        Text(
                            text = "No usage data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Challenges Completed Card
            WellbeingCard {
                CardHeader(title = "Challenges Completed")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = uiState.challengesCompleted.toString(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = "Today",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        StatBadge(
                            value = "+45%",
                            label = "vs last week"
                        )
                    }
                }
            }
        }
    }

    // App Timer Dialog
    if (showTimerDialog && selectedAppForTimer != null) {
        AppTimerDialog(
            appName = selectedAppForTimer!!.appName,
            currentLimitMillis = uiState.appLimits[selectedAppForTimer!!.packageName],
            onDismiss = {
                showTimerDialog = false
                selectedAppForTimer = null
            },
            onSetTimer = { minutes ->
                selectedAppForTimer?.let { app ->
                    viewModel.setAppTimer(app.packageName, app.appName, minutes)
                }
                showTimerDialog = false
                selectedAppForTimer = null
            },
            onDeleteTimer = {
                selectedAppForTimer?.let { app ->
                    viewModel.removeAppTimer(app.packageName)
                }
            }
        )
    }
}

@Composable
fun UsagePermissionCard(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WellbeingCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "⚠️",
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Usage Permission Required",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    letterSpacing = (-0.3).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Grant usage stats permission to track your screen time and app usage.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onGrantClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.digitalwellbeing.ui.theme.Black
                    )
                ) {
                    Text(
                        text = "Grant Permission",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun AccessibilityPermissionCard(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WellbeingCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "🔒",
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable App Blocking",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    letterSpacing = (-0.3).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enable accessibility service to block apps when timers run out. You've set timers but blocking is not enabled yet.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onGrantClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.digitalwellbeing.ui.theme.Black
                    )
                ) {
                    Text(
                        text = "Enable App Blocking",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
