package com.example.digitalwellbeing.ui.screens.setup

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalwellbeing.data.model.AppUsageInfo
import com.example.digitalwellbeing.ui.MainViewModel
import com.example.digitalwellbeing.ui.components.*
import com.example.digitalwellbeing.ui.theme.*
import com.example.digitalwellbeing.util.AppCategorizer
import com.example.digitalwellbeing.util.AppCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statsState by viewModel.statsState.collectAsState()
    var currentStep by remember { mutableStateOf(0) }
    var selectedDailyGoal by remember { mutableStateOf(6) } // hours
    var selectedApps by remember { mutableStateOf(setOf<String>()) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var selectedAppForTimer by remember { mutableStateOf<AppUsageInfo?>(null) }

    val steps = listOf(
        OnboardingStep.DailyGoal,
        OnboardingStep.AppTimers,
        OnboardingStep.Complete
    )

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Set Your Goals",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.8).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Configure your daily screen time target and app limits",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Progress indicator
        LinearProgressIndicator(
            progress = (currentStep + 1).toFloat() / steps.size,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            color = TextPrimary,
            trackColor = TextSecondary.copy(alpha = 0.2f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Step content
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) + fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) + fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    )
                } else {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) + fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) + fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    )
                }
            },
            modifier = Modifier.weight(1f)
        ) { stepIndex ->
            when (steps[stepIndex]) {
                OnboardingStep.DailyGoal -> {
                    DailyGoalStep(
                        selectedHours = selectedDailyGoal,
                        onHoursSelected = { selectedDailyGoal = it },
                        onNextClick = { currentStep++ }
                    )
                }
                OnboardingStep.AppTimers -> {
                    AppTimersStep(
                        apps = statsState.allApps, // Show all apps organized by categories
                        selectedApps = selectedApps,
                        appLimits = statsState.appLimits,
                        onAppSelected = { packageName ->
                            selectedApps = if (packageName in selectedApps) {
                                selectedApps - packageName
                            } else {
                                selectedApps + packageName
                            }
                        },
                        onSetTimerClick = { app ->
                            selectedAppForTimer = app
                            showTimerDialog = true
                        },
                        onNextClick = { currentStep++ }
                    )
                }
                OnboardingStep.Complete -> {
                    SetupCompleteStep(
                        dailyGoal = selectedDailyGoal,
                        selectedAppsCount = selectedApps.size,
                        onCompleteClick = {
                            // Save the daily goal (this would need to be implemented in ViewModel)
                            // For now, just complete setup
                            onSetupComplete()
                        }
                    )
                }
            }
        }

        // Step indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, _ ->
                val isCompleted = index < currentStep
                val isCurrent = index == currentStep

                Surface(
                    shape = RoundedCornerShape(50),
                    color = when {
                        isCompleted -> TextPrimary
                        isCurrent -> TextPrimary.copy(alpha = 0.6f)
                        else -> TextSecondary.copy(alpha = 0.2f)
                    },
                    modifier = Modifier.size(8.dp)
                ) {}
                if (index < steps.lastIndex) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }

    // App Timer Dialog
    if (showTimerDialog && selectedAppForTimer != null) {
        AppTimerDialog(
            appName = selectedAppForTimer!!.appName,
            currentLimitMillis = statsState.appLimits[selectedAppForTimer!!.packageName],
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

enum class OnboardingStep {
    DailyGoal,
    AppTimers,
    Complete
}

@Composable
private fun DailyGoalStep(
    selectedHours: Int,
    onHoursSelected: (Int) -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Text(
            text = "🎯",
            fontSize = 72.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Title
        Text(
            text = "Daily Screen Time Goal",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = "Set a daily screen time target to help you stay mindful of your device usage.",
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Hour selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (selectedHours > 1) onHoursSelected(selectedHours - 1) },
                enabled = selectedHours > 1
            ) {
                Text("−", fontSize = 32.sp, color = TextPrimary)
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                modifier = Modifier
                    .width(120.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "${selectedHours}h",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            IconButton(
                onClick = { if (selectedHours < 12) onHoursSelected(selectedHours + 1) },
                enabled = selectedHours < 12
            ) {
                Text("+", fontSize = 32.sp, color = TextPrimary)
            }
        }

        Text(
            text = "hours per day",
            fontSize = 16.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = onNextClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Black
            )
        ) {
            Text(
                text = "Continue",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AppTimersStep(
    apps: List<AppUsageInfo>,
    selectedApps: Set<String>,
    appLimits: Map<String, Long>,
    onAppSelected: (String) -> Unit,
    onSetTimerClick: (AppUsageInfo) -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Group apps by category
    val categorizedApps = remember(apps) {
        AppCategorizer.groupAppsByCategory(apps)
    }

    var expandedCategories by remember { mutableStateOf(setOf<AppCategory>()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // Icon
        Text(
            text = "⏰",
            fontSize = 48.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        )

        // Title
        Text(
            text = "Set App Timers",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = "Set time limits by category or individual apps. Tap a category to expand it.",
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Categories list
        if (categorizedApps.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
            ) {
                categorizedApps.forEach { (category, categoryApps) ->
                    if (categoryApps.isNotEmpty()) {
                        CategoryItem(
                            category = category,
                            apps = categoryApps,
                            isExpanded = category in expandedCategories,
                            selectedApps = selectedApps,
                            appLimits = appLimits,
                            onCategoryClick = {
                                expandedCategories = if (category in expandedCategories) {
                                    expandedCategories - category
                                } else {
                                    expandedCategories + category
                                }
                            },
                            onAppSelected = onAppSelected,
                            onSetTimerClick = onSetTimerClick
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No apps found. Please grant usage permission first.",
                    fontSize = 16.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNextClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Black
            )
        ) {
            Text(
                text = "Continue",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CategoryItem(
    category: AppCategory,
    apps: List<AppUsageInfo>,
    isExpanded: Boolean,
    selectedApps: Set<String>,
    appLimits: Map<String, Long>,
    onCategoryClick: () -> Unit,
    onAppSelected: (String) -> Unit,
    onSetTimerClick: (AppUsageInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalUsage = remember(apps) { apps.sumOf { it.usageTimeMillis } }

    Column(modifier = modifier.fillMaxWidth()) {
        // Category header
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isExpanded) CardBackground else Surface,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onCategoryClick,
                    indication = rememberRipple(bounded = true),
                    interactionSource = remember { MutableInteractionSource() }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category emoji
                Text(
                    text = category.emoji,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )

                // Category info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.displayName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${apps.size} apps • ${totalUsage.formatUsageTime()}",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                // Expand/Collapse icon
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    fontSize = 16.sp,
                    color = TextSecondary
                )
            }
        }

        // Expanded apps list
        if (isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp)
            ) {
                apps.forEach { app ->
                    AppSelectionItem(
                        app = app,
                        isSelected = app.packageName in selectedApps,
                        hasTimer = appLimits.containsKey(app.packageName),
                        onAppClick = { onAppSelected(app.packageName) },
                        onSetTimerClick = { onSetTimerClick(app) }
                    )
                    if (app != apps.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSelectionItem(
    app: AppUsageInfo,
    isSelected: Boolean,
    hasTimer: Boolean,
    onAppClick: () -> Unit,
    onSetTimerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) CardBackground else Surface,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onAppClick,
                indication = rememberRipple(bounded = true),
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon placeholder
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = TextSecondary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Text(
                    text = app.appName.first().toString(),
                    fontSize = 20.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = app.usageTimeMillis.formatUsageTime(),
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            if (hasTimer) {
                Text(
                    text = "⏰",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onAppClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = TextPrimary,
                    uncheckedColor = TextSecondary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun SetupCompleteStep(
    dailyGoal: Int,
    selectedAppsCount: Int,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Text(
            text = "🎉",
            fontSize = 72.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Title
        Text(
            text = "Setup Complete!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.7).sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Summary
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Goals:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${dailyGoal}h",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Daily Goal",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = selectedAppsCount.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Apps Limited",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "You're all set! You can view your stats and modify settings anytime.",
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onCompleteClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Black
            )
        ) {
            Text(
                text = "Get Started",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun Long.formatUsageTime(): String {
    val hours = this / (1000 * 60 * 60)
    val minutes = (this % (1000 * 60 * 60)) / (1000 * 60)
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "< 1m"
    }
}
