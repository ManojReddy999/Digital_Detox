package com.focus.digitalwellbeing.ui.screens.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focus.digitalwellbeing.data.model.FocusGroup
import com.focus.digitalwellbeing.data.model.FocusType
import com.focus.digitalwellbeing.ui.MainViewModel
import com.focus.digitalwellbeing.ui.components.AppHeader
import com.focus.digitalwellbeing.ui.components.AppIcon
import com.focus.digitalwellbeing.ui.utils.AnimatedVisibilitySlide
import com.focus.digitalwellbeing.ui.utils.SlideDirection
import com.focus.digitalwellbeing.data.model.FocusSession
import com.focus.digitalwellbeing.ui.components.FocusGroupItem
import com.focus.digitalwellbeing.ui.components.EarlyTerminationConfirmationDialog
import com.focus.digitalwellbeing.ui.components.FocusSessionManager

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FocusScreen(
    viewModel: MainViewModel,
    onCreateFocusGroup: () -> Unit,
    onEditFocusGroup: (FocusGroup) -> Unit,
    showStopDialogOnLoad: Boolean = false,
    onOpenShop: () -> Unit
) {
    val focusGroups by viewModel.focusGroups.collectAsState()
    val activeSession by viewModel.activeFocusSession.collectAsState()
    val coinBalance by viewModel.coinBalance.collectAsState()

    var showDurationDialog by remember { mutableStateOf<FocusGroup?>(null) }
    var showEarlyTerminationDialog by remember { mutableStateOf(false) }

    // Auto-show stop dialog if triggered from notification
    LaunchedEffect(showStopDialogOnLoad, activeSession) {
        if (showStopDialogOnLoad && activeSession?.isActive == true && !showEarlyTerminationDialog) {
            showEarlyTerminationDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        AppHeader(
            title = "Groups",
            action = {
                IconButton(
                    onClick = onCreateFocusGroup,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Focus Group",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        )

        // Get all installed apps
        val appsOrderedByUsage = remember { viewModel.getAppsOrderedByUsage() }
        val installedPackages = remember(appsOrderedByUsage) {
            appsOrderedByUsage.map { it.first }.toSet()
        }
        
        val (scheduledGroups, unscheduledGroups) = remember(focusGroups) {
            focusGroups.partition { it.scheduledStartTime != null }
        }

        val templateGroups = remember(appsOrderedByUsage) {
            // Helper to categorize apps
            fun getCategory(packageName: String, appName: String): String {
                val lowerName = appName.lowercase()
                val lowerPkg = packageName.lowercase()
                return when {
                    // Social
                    lowerPkg.contains("instagram") || lowerPkg.contains("facebook") || lowerPkg.contains("twitter") || lowerPkg.contains("tiktok") || lowerPkg.contains("snapchat") || lowerPkg.contains("reddit") || lowerPkg.contains("linkedin") || lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") || lowerPkg.contains("discord") || lowerPkg.contains("pinterest") || lowerPkg.contains("threads") -> "Social"
                    // Entertainment
                    lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("prime video") || lowerPkg.contains("disney") || lowerPkg.contains("hulu") || lowerPkg.contains("spotify") || lowerPkg.contains("music") || lowerPkg.contains("twitch") || lowerPkg.contains("hotstar") || lowerPkg.contains("jiocinema") -> "Entertainment"
                    // Games
                    lowerPkg.contains("game") || lowerPkg.contains("play") || lowerPkg.contains("clash") || lowerPkg.contains("candy") || lowerPkg.contains("surfers") || lowerPkg.contains("roblox") || lowerPkg.contains("minecraft") || lowerPkg.contains("pubg") || lowerPkg.contains("bgmi") || lowerPkg.contains("ludo") -> "Games"
                    // Shopping
                    lowerPkg.contains("amazon") || lowerPkg.contains("flipkart") || lowerPkg.contains("myntra") || lowerPkg.contains("meesho") || lowerPkg.contains("ebay") || lowerPkg.contains("walmart") || lowerPkg.contains("target") || lowerPkg.contains("etsy") || lowerPkg.contains("shopping") -> "Shopping"
                    else -> "Other"
                }
            }

            val socialApps = appsOrderedByUsage.filter { getCategory(it.first, it.second) == "Social" }.map { it.first }
            val entertainmentApps = appsOrderedByUsage.filter { getCategory(it.first, it.second) == "Entertainment" }.map { it.first }
            val gameApps = appsOrderedByUsage.filter { getCategory(it.first, it.second) == "Games" }.map { it.first }
            val shoppingApps = appsOrderedByUsage.filter { getCategory(it.first, it.second) == "Shopping" }.map { it.first }

            listOf(
                Triple("Social Detox", socialApps, socialApps.size),
                Triple("Deep Work", (socialApps + entertainmentApps + gameApps + shoppingApps).distinct(), (socialApps + entertainmentApps + gameApps + shoppingApps).distinct().size),
                Triple("Study", (socialApps + entertainmentApps + gameApps).distinct(), (socialApps + entertainmentApps + gameApps).distinct().size),
                Triple("Sleep", (socialApps + entertainmentApps + gameApps + shoppingApps).distinct(), (socialApps + entertainmentApps + gameApps + shoppingApps).distinct().size)
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
                // Unscheduled Groups
                items(
                    items = unscheduledGroups,
                    key = { it.id }
                ) { group ->
                        FocusSessionManager(
                            activeSession = activeSession,
                            coinBalance = coinBalance,
                            onStopSession = { viewModel.stopFocusSession() },
                            onStopSessionWithCharge = { viewModel.stopFocusSessionWithCharge() },
                            onGetCoins = onOpenShop
                        ) { onStopClick ->
                            FocusGroupItem(
                                group = group,
                                activeSession = activeSession,
                                onStartSession = { 
                                    if (group.scheduledDurationMinutes != null) {
                                        viewModel.startFocusSession(group, group.scheduledDurationMinutes)
                                    } else {
                                        showDurationDialog = group 
                                    }
                                },
                                onStopSession = onStopClick,
                                onEdit = { onEditFocusGroup(group) },
                                onDelete = { viewModel.deleteFocusGroup(group) },
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
                }

                // Scheduled Groups Section
                if (scheduledGroups.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scheduled",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(
                        items = scheduledGroups,
                        key = { it.id }
                    ) { group ->
                        FocusSessionManager(
                            activeSession = activeSession,
                            coinBalance = coinBalance,
                            onStopSession = { viewModel.stopFocusSession() },
                            onStopSessionWithCharge = { viewModel.stopFocusSessionWithCharge() },
                            onGetCoins = onOpenShop
                        ) { onStopClick ->
                            FocusGroupItem(
                                group = group,
                                activeSession = activeSession,
                                onStartSession = {
                                    if (group.scheduledDurationMinutes != null) {
                                        viewModel.startFocusSession(group, group.scheduledDurationMinutes)
                                    } else {
                                        showDurationDialog = group
                                    }
                                },
                                onStopSession = onStopClick,
                                onEdit = { onEditFocusGroup(group) },
                                onDelete = { viewModel.deleteFocusGroup(group) },
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
                    }
                }

                // Templates Section (at bottom when groups exist)
                item {
                    // Expand templates by default if no user groups exist
                    var templatesExpanded by remember(focusGroups) { mutableStateOf(focusGroups.isEmpty()) }

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column {
                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { templatesExpanded = !templatesExpanded }
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Templates",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Quick start focus groups",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (templatesExpanded)
                                        Icons.Default.KeyboardArrowUp
                                    else
                                        Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (templatesExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Expandable content
                            androidx.compose.animation.AnimatedVisibility(
                                visible = templatesExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    templateGroups.forEach { (name, apps, _) ->
                                        // Create a temporary FocusGroup for the template
                                        val templateGroup = FocusGroup(
                                            id = 0L,
                                            name = name,
                                            type = FocusType.BLOCKLIST,
                                            appPackages = apps,
                                            icon = name,
                                            scheduledStartTime = null,
                                            scheduledDurationMinutes = null
                                        )

                                        // Use FocusGroupItem for consistent design
                                        // We pass empty lambdas for start/stop as we handle the click on the item itself
                                        // or we can repurpose the "Start" button to "Create"
                                        FocusGroupItem(
                                            group = templateGroup,
                                            activeSession = null,
                                            onStartSession = { 
                                                onEditFocusGroup(templateGroup)
                                            },
                                            onStopSession = {},
                                            onEdit = { onEditFocusGroup(templateGroup) },
                                            onDelete = {},
                                            modifier = Modifier.clickable { onEditFocusGroup(templateGroup) },
                                            isTemplate = true
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }


        // Create Group Button (Floating above bottom nav space is handled by padding, but here we might want it fixed or in list)
        // Keeping it at bottom of column for now, but ensuring it doesn't overlap content if list is long is tricky without Box.
        // Ideally, this should be in a Box with the LazyColumn, or just below it.
        // Given the previous layout had it in Column, I'll keep it there but ensure padding.
        

        
        // Bottom padding for navigation bar
        Spacer(modifier = Modifier.height(60.dp))
    }
    
    // Duration Selection Dialog
    showDurationDialog?.let { group ->
        DurationSelectionDialog(
            onDismiss = { showDurationDialog = null },
            onStartSession = { durationMinutes ->
                viewModel.startFocusSession(group, durationMinutes)
                showDurationDialog = null
            }
        )
    }


}



@Composable
fun DurationSelectionDialog(
    onDismiss: () -> Unit,
    onStartSession: (Int?) -> Unit
) {
    val options = listOf(
        15, 30, 45,
        60, 120, 240
    )

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 24.dp), 
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Focus Duration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // "Until turned off" - prominent option
                    Button(
                        onClick = { onStartSession(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            "Until I turn it off",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Duration Grid
                    val chunkedOptions = options.chunked(3)
                    chunkedOptions.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { mins ->
                                val label = if (mins >= 60) "${mins/60}h" else "${mins}m"
                                FilledTonalButton(
                                    onClick = { onStartSession(mins) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(45.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}



