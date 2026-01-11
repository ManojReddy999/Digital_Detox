package com.focus.digitalwellbeing.ui.screens.stats

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focus.digitalwellbeing.data.model.AppUsageInfo
import com.focus.digitalwellbeing.ui.MainViewModel
import com.focus.digitalwellbeing.ui.components.AndroidTimerPicker
import com.focus.digitalwellbeing.ui.components.AppHeader
import com.focus.digitalwellbeing.ui.components.AppIcon
import com.focus.digitalwellbeing.ui.theme.BackgroundBlack
import com.focus.digitalwellbeing.ui.theme.CardBorder
import com.focus.digitalwellbeing.ui.theme.DividerAlpha
import com.focus.digitalwellbeing.ui.theme.ErrorRed
import com.focus.digitalwellbeing.ui.theme.NeonGreen
import com.focus.digitalwellbeing.ui.theme.NeonGreenLowAlpha
import com.focus.digitalwellbeing.ui.theme.TextSecondaryAlpha
import com.focus.digitalwellbeing.ui.theme.WarningOrange
import com.focus.digitalwellbeing.util.DateUtils
import kotlinx.coroutines.launch

@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    onOpenShop: () -> Unit = {}
) {
    val uiState by viewModel.statsState.collectAsState()
    val dashboardState by viewModel.dashboardState.collectAsState()
    val appLimitsMap = dashboardState.appLimits
    val accessibilityEnabled = dashboardState.accessibilityEnabled

    var showTimerDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<AppUsageInfo?>(null) }
    var selectedSegmentIndex by remember { mutableStateOf(1) } // 0=Day, 1=Week, 2=Month
    var showAllApps by remember { mutableStateOf(false) } // For expandable section

    val scope = rememberCoroutineScope()

    // Main Container
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        val coinBalance by viewModel.coinBalance.collectAsState()
        AppHeader(
            title = "Activity",
            action = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClick = onOpenShop)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Wallet",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = coinBalance.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // --- Time Display (Large) ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = uiState.totalUsage,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-3).sp,
                            lineHeight = 80.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // --- Chart Section (No Card) ---
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Chart with grid lines and labels
                    // Fixed scale to match the labels (8h)
                    val chartMaxMillis = 8 * 60 * 60 * 1000L
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Chart area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(170.dp)
                                .drawBehind {
                                    // Draw horizontal grid lines at 0h, 2h, 4h, 6h, 8h
                                    val gridLinesCount = 5 // 0h, 2h, 4h, 6h, 8h
                                    val lineColor = Color.Gray.copy(alpha = 0.2f) // Using generic color for canvas drawing inside composable
                                    val height = size.height
                                    
                                    for (i in 0 until gridLinesCount) {
                                        val y = height - (height / (gridLinesCount - 1)) * i
                                        drawLine(
                                            color = lineColor,
                                            start = Offset(0f, y),
                                            end = Offset(size.width, y),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                if (selectedSegmentIndex == 1) {
                                    // Week View - Interactive Bars
                                    uiState.weeklyStats.forEachIndexed { index, (_, usage) ->
                                        val isSelected = index == uiState.selectedDayIndex
                                        val heightRatio = (usage.toFloat() / chartMaxMillis).coerceIn(0f, 1f)
                                        
                                        Box(
                                            modifier = Modifier
                                                .width(36.dp)
                                                .fillMaxHeight()
                                                .clickable { viewModel.selectDay(index) },
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight(heightRatio.coerceAtLeast(0.02f))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            )
                                        }
                                    }
                                } else {
                                    // Dummy Visuals for Day/Month (Placeholder)
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Chart data available for Week view",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Grid line labels (0h, 2h, 4h, 6h, 8h) - positioned to match grid lines exactly
                        Column(
                            modifier = Modifier
                                .height(170.dp)
                                .width(32.dp) // Fixed width to align with bottom labels
                                .padding(start = 8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("8h", "6h", "4h", "2h", "0h").forEach { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Day labels below the chart
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            uiState.weeklyStats.forEach { (day, _) ->
                                Text(
                                    text = day.take(3),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                        // Empty space for grid labels column
                        Spacer(modifier = Modifier.width(32.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }

            // --- Most Used Section ---
            val appsToShow = if (uiState.selectedDayApps.isNotEmpty()) uiState.selectedDayApps else uiState.allApps
            
            // Split apps: >= 1 minute and < 1 minute
            val oneMinuteInMillis = 60 * 1000L
            val mainApps = appsToShow.filter { it.usageTimeMillis >= oneMinuteInMillis }
            val minorApps = appsToShow.filter { it.usageTimeMillis < oneMinuteInMillis }

            if (mainApps.isNotEmpty()) {
                items(mainApps) { app ->
                    StatsAppRow(
                        app = app,
                        limitMillis = appLimitsMap[app.packageName],
                        onTimerClick = {
                            selectedApp = app
                            showTimerDialog = true
                        }
                    )
                }
            } else {
                item {
                    Text(
                        text = "No significant usage",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

            // Expandable section for apps with < 1 minute usage
            if (minorApps.isNotEmpty()) {
                item {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))

                        // "Show all apps" button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAllApps = !showAllApps }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Show all apps (${minorApps.size})",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(
                                imageVector = if (showAllApps) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (showAllApps) "Hide" else "Show",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Animated expandable content
                        AnimatedVisibility(
                            visible = showAllApps,
                            enter = expandVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + fadeIn(),
                            exit = shrinkVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + fadeOut()
                        ) {
                            Column {
                                minorApps.forEach { app ->
                                    StatsAppRow(
                                        app = app,
                                        limitMillis = appLimitsMap[app.packageName],
                                        onTimerClick = {
                                            selectedApp = app
                                            showTimerDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Timer Dialog Logic
    if (showTimerDialog && selectedApp != null) {
        val app = selectedApp!!
        val existingLimit = appLimitsMap[app.packageName] ?: 0L
        val hasExistingTimer = existingLimit > 0

        // Calculate initial hours and minutes from existing timer
        val initialHours = (existingLimit / (3600 * 1000)).toInt()
        val initialMinutes = ((existingLimit % (3600 * 1000)) / (60 * 1000)).toInt()

        // Get coin balance
        val coinBalance = viewModel.coinBalance.collectAsState().value

        // Calculate coin cost dynamically
        var coinCost by remember { mutableStateOf(0) }
        var selectedTimerMillis by remember { mutableStateOf(if (hasExistingTimer) existingLimit else (30 * 60 * 1000L)) }

        LaunchedEffect(selectedTimerMillis) {
            coinCost = if (hasExistingTimer && selectedTimerMillis > existingLimit) {
                // Only charge for increasing an existing timer
                val diff = selectedTimerMillis - existingLimit
                (diff / (60 * 1000)).toInt()
            } else {
                // No charge for new timers or decreasing existing timers
                0
            }
        }

        AndroidTimerPicker(
            appName = app.appName,
            initialHours = if (hasExistingTimer) initialHours else 0,
            initialMinutes = if (hasExistingTimer) initialMinutes else 30,
            hasExistingTimer = hasExistingTimer,
            coinCost = coinCost,
            coinBalance = coinBalance,
            onTimeChanged = { hours, minutes ->
                selectedTimerMillis = (hours * 3600 * 1000L) + (minutes * 60 * 1000L)
            },
            onDismiss = { showTimerDialog = false },
            onTimeSelected = { hours, minutes ->
                val totalMillis = (hours * 3600 * 1000L) + (minutes * 60 * 1000L)
                scope.launch {
                    if (totalMillis > 0) {
                        viewModel.setAppTimer(app.packageName, app.appName, totalMillis)

                        // Check if accessibility service is enabled
                        if (!accessibilityEnabled) {
                            showPermissionDialog = true
                        }
                    } else {
                        viewModel.removeAppTimer(app.packageName)
                    }
                    showTimerDialog = false
                }
            },
            onDelete = if (hasExistingTimer) {
                {
                    scope.launch {
                        viewModel.removeAppTimer(app.packageName)
                        showTimerDialog = false
                        selectedApp = null
                    }
                }
            } else null,
            onGetCoins = onOpenShop
        )
    }
    
    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .windowInsetsPadding(WindowInsets.systemBars),
            title = { Text("Permission Required") },
            text = { Text("To enforce app limits, you need to enable the Accessibility Service for Digital Wellbeing.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.requestAccessibilityPermission()
                        showPermissionDialog = false
                    }
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatsAppRow(
    app: AppUsageInfo,
    limitMillis: Long?,
    onTimerClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon
        AppIcon(
            packageName = app.packageName,
            appName = app.appName,
            size = 40.dp,
            modifier = Modifier.padding(end = 16.dp)
        )
        
        // App name + usage time
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = DateUtils.formatDuration(app.usageTimeMillis),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 14.sp
                ),
                color = if (limitMillis != null && app.usageTimeMillis >= limitMillis) 
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        
        // Vertical separator (always present)
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Timer section - always use hourglass icon
        if (limitMillis != null) {
            // Hourglass icon on top, limit text below
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onTimerClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HourglassEmpty,
                        contentDescription = "Edit Timer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = DateUtils.formatDuration(limitMillis),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Just hourglass icon if no limit
            IconButton(
                onClick = onTimerClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.HourglassEmpty,
                    contentDescription = "Set Timer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun Modifier.drawBehindBottomBorder(
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    strokeWidth: Dp = 1.dp
): Modifier = this.drawBehind {
    val strokePx = strokeWidth.toPx()
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = strokePx
    )
}

