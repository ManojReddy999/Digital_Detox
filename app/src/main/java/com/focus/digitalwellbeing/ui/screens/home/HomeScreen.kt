package com.focus.digitalwellbeing.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focus.digitalwellbeing.ui.MainViewModel
import com.focus.digitalwellbeing.ui.components.AndroidTimerPicker
import com.focus.digitalwellbeing.ui.components.AppHeader
import com.focus.digitalwellbeing.ui.components.AppIcon
import com.focus.digitalwellbeing.ui.theme.BackgroundBlack
import com.focus.digitalwellbeing.ui.theme.CardBackground
import com.focus.digitalwellbeing.ui.theme.CardBorder
import com.focus.digitalwellbeing.ui.theme.DividerAlpha
import com.focus.digitalwellbeing.ui.theme.ErrorRed
import com.focus.digitalwellbeing.ui.theme.NeonGreen
import com.focus.digitalwellbeing.ui.theme.TextSecondaryAlpha
import com.focus.digitalwellbeing.ui.theme.WarningOrange
import com.focus.digitalwellbeing.util.DateUtils
import kotlinx.coroutines.launch
import com.focus.digitalwellbeing.ui.components.FocusGroupItem
import com.focus.digitalwellbeing.data.model.FocusGroup
import com.focus.digitalwellbeing.ui.components.EarlyTerminationConfirmationDialog
import com.focus.digitalwellbeing.ui.components.FocusSessionManager
import com.focus.digitalwellbeing.data.model.FocusType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onEditFocusGroup: (FocusGroup) -> Unit,
    onOpenShop: () -> Unit
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val scope = rememberCoroutineScope()

    // Timer dialog state
    var showTimerDialog by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<com.focus.digitalwellbeing.data.model.AppUsageInfo?>(null) }

    // Recent Focus Group state
    var recentFocusGroup by remember { mutableStateOf<FocusGroup?>(null) }
    val recentSession by viewModel.mostRecentFocusSession.collectAsState()
    
    LaunchedEffect(recentSession) {
        recentSession?.focusGroupId?.let { groupId ->
            recentFocusGroup = viewModel.getFocusGroupById(groupId)
        }
    }

    // Active Focus Session state
    val activeSession by viewModel.activeFocusSession.collectAsState()
    var activeFocusGroup by remember { mutableStateOf<FocusGroup?>(null) }
    val coinBalance by viewModel.coinBalance.collectAsState()

    LaunchedEffect(activeSession) {
        activeSession?.focusGroupId?.let { groupId ->
            activeFocusGroup = viewModel.getFocusGroupById(groupId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        AppHeader(
            title = "Focus",
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

            // --- Daily Screen Time Tracking ---
            item {
                val currentUsage = dashboardState.todayStats?.totalUsageTimeMillis ?: 0L
                val averageUsage = dashboardState.averageDailyUsage

                FocusProgressSection(
                    currentUsage = currentUsage,
                    averageUsage = averageUsage
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Quick Start Recent Focus Session or Suggested Session ---
            item {
                val groupToShow = if (activeSession?.isActive == true) activeFocusGroup else recentFocusGroup
                val sectionTitle = if (activeSession?.isActive == true) "Current Session" else "Recent Session"

                // Get all user-created focus groups
                val allFocusGroups by viewModel.focusGroups.collectAsState()

                // Get suggestion: prefer user-created groups, fall back to templates
                val appsOrderedByUsage = remember { viewModel.getAppsOrderedByUsage() }
                val installedPackages = remember(appsOrderedByUsage) {
                    appsOrderedByUsage.map { it.first }.toSet()
                }

                // Suggested group (from user-created groups if available)
                val suggestedUserGroup = remember(allFocusGroups) {
                    allFocusGroups.randomOrNull()
                }

                // Template groups (only used if no user-created groups exist)
                // Template groups (only used if no user-created groups exist)
                val templateGroups = remember(installedPackages) {
                    val allTemplates = listOf(
                        "Study" to listOf("com.instagram.android", "com.twitter.android", "com.facebook.katana", "com.snapchat.android", "com.zhiliaoapp.musically", "com.reddit.frontpage", "com.netflix.mediaclient"),
                        "Work" to listOf("com.instagram.android", "com.twitter.android", "com.facebook.katana", "com.whatsapp", "com.snapchat.android", "com.zhiliaoapp.musically", "com.reddit.frontpage"),
                        "Wake Up" to listOf("com.instagram.android", "com.twitter.android", "com.facebook.katana", "com.reddit.frontpage", "com.zhiliaoapp.musically", "com.snapchat.android"),
                        "Sleep" to listOf("com.instagram.android", "com.twitter.android", "com.facebook.katana", "com.snapchat.android", "com.zhiliaoapp.musically", "com.reddit.frontpage", "com.netflix.mediaclient"),
                        "Exercise" to listOf("com.instagram.android", "com.twitter.android", "com.facebook.katana", "com.reddit.frontpage", "com.snapchat.android", "com.zhiliaoapp.musically")
                    )

                    allTemplates.map { (name, apps) ->
                        val installedApps = apps.filter { it in installedPackages }
                        Triple(name, installedApps, apps.size)
                    }.filter { it.second.isNotEmpty() }
                }

                val randomTemplate = remember(templateGroups) {
                    templateGroups.randomOrNull()
                }

                if (groupToShow != null) {
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    FocusSessionManager(
                        activeSession = activeSession,
                        coinBalance = coinBalance,
                        onStopSession = { viewModel.stopFocusSession() },
                        onStopSessionWithCharge = { viewModel.stopFocusSessionWithCharge() },
                        onGetCoins = onOpenShop
                    ) { onStopClick ->
                        FocusGroupItem(
                            group = groupToShow,
                            activeSession = activeSession,
                            onStartSession = {
                                // Priority: 1. Group's scheduled duration, 2. Default 60 minutes
                                // Don't use recent session duration as it may have been terminated early
                                val durationMinutes = groupToShow.scheduledDurationMinutes ?: 60
                                viewModel.startFocusSession(groupToShow, durationMinutes)
                            },
                            onStopSession = onStopClick,
                            onEdit = { onEditFocusGroup(groupToShow) },
                            onDelete = { viewModel.deleteFocusGroup(groupToShow) }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                } else if (suggestedUserGroup != null) {
                    // Show user-created group as suggestion
                    Text(
                        text = "Suggested Session",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    FocusGroupItem(
                        group = suggestedUserGroup,
                        activeSession = null,
                        onStartSession = {
                            viewModel.startFocusSession(suggestedUserGroup, null)
                        },
                        onStopSession = {},
                        onEdit = { onEditFocusGroup(suggestedUserGroup) },
                        onDelete = { viewModel.deleteFocusGroup(suggestedUserGroup) }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                } else if (randomTemplate != null) {
                    // Show template when no recent session
                    val (name, apps, _) = randomTemplate

                    Text(
                        text = "Suggested Session",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newGroup = FocusGroup(
                                    id = 0L,
                                    name = name,
                                    type = FocusType.BLOCKLIST,
                                    appPackages = apps,
                                    icon = name,
                                    scheduledStartTime = null,
                                    scheduledDurationMinutes = null
                                )
                                onEditFocusGroup(newGroup)
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = com.focus.digitalwellbeing.ui.components.FocusIcons.getIcon(name),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Blocks ${apps.size} distracting apps",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // --- Top Distractions ---
            item {
                Text(
                    text = "Top Distractions",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            val topApps = dashboardState.topApps
            val appLimitsMap = dashboardState.appLimits

            if (topApps.isNotEmpty()) {
                items(topApps) { app ->
                    AppRowTight(
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
                        text = "No app usage data yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }


    // Timer dialog
    if (showTimerDialog && selectedApp != null) {
        // ... (existing timer dialog code)
        val app = selectedApp!!
        val appLimitsMap = dashboardState.appLimits
        val existingLimit = appLimitsMap[app.packageName] ?: 0L
        val hasExistingTimer = existingLimit > 0

        // Calculate initial hours and minutes
        val initialHours = (existingLimit / (3600 * 1000)).toInt()
        val initialMinutes = ((existingLimit % (3600 * 1000)) / (60 * 1000)).toInt()
        
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
            onDismiss = {
                showTimerDialog = false
                selectedApp = null
            },
            onTimeSelected = { hours, minutes ->
                val totalMillis = (hours * 3600 * 1000L) + (minutes * 60 * 1000L)
                scope.launch {
                    if (totalMillis > 0) {
                        viewModel.setAppTimer(app.packageName, app.appName, totalMillis)
                    } else {
                        viewModel.removeAppTimer(app.packageName)
                    }
                    showTimerDialog = false
                    selectedApp = null
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


}

@Composable
fun FocusProgressSection(
    currentUsage: Long,
    averageUsage: Long
) {
    // Progress of today vs average
    val progress = if (averageUsage > 0) (currentUsage.toFloat() / averageUsage).coerceIn(0f, 1f) else 0f
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = DateUtils.formatDuration(averageUsage),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 64.sp, // Reduced from 96.sp to fit text
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-2).sp,
                    lineHeight = 64.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Text(
            text = "AVERAGE SCREEN TIME",
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
        )
        
        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        
        // Time Breakdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Today: " + DateUtils.formatDuration(currentUsage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Avg: " + DateUtils.formatDuration(averageUsage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AppRowTight(
    app: com.focus.digitalwellbeing.data.model.AppUsageInfo,
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
        
        // Timer section
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
fun Modifier.drawBehindBottomBorder(
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

