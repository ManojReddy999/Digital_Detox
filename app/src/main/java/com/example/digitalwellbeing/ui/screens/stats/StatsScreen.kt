package com.example.digitalwellbeing.ui.screens.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.statsState.collectAsState()
    var showTimerDialog by remember { mutableStateOf(false) }
    var selectedAppForTimer by remember { mutableStateOf<com.example.digitalwellbeing.data.model.AppUsageInfo?>(null) }

    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    var selectedDayApps by remember { mutableStateOf<List<com.example.digitalwellbeing.data.model.AppUsageInfo>>(emptyList()) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Digital Wellbeing",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = com.example.digitalwellbeing.ui.theme.Surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 100.dp)
        ) {
            if (!uiState.usagePermissionGranted) {
                UsagePermissionCard(
                    onGrantClick = { viewModel.requestUsagePermission() }
                )
            } else {
                // Total Screen Time Card with Weekly Chart
                WellbeingCard {
                    // Capture selected day index for stable reference
                    val currentSelectedDay = selectedDayIndex

                    // Calculate the total to display based on selection
                    val displayTotal = if (currentSelectedDay != null) {
                        // Show selected day's total (even if 0)
                        val totalMillis = selectedDayApps.sumOf { it.usageTimeMillis }
                        val hours = totalMillis / (1000 * 60 * 60)
                        val minutes = (totalMillis % (1000 * 60 * 60)) / (1000 * 60)
                        "${hours}h ${minutes}m"
                    } else {
                        // Show today's total only when no day is selected
                        uiState.totalUsage
                    }

                    // Determine the label to display
                    val fullDayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    val displayLabel = if (currentSelectedDay != null) {
                        fullDayNames[currentSelectedDay]
                    } else {
                        "Today"
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = displayTotal,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-1.5).sp
                        )
                        Text(
                            text = displayLabel,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Weekly bar chart
                    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val weeklyData = if (uiState.weeklyStats.isNotEmpty()) {
                        uiState.weeklyStats
                    } else {
                        dayLabels.map { day -> day to 0L }
                    }
                    WeeklyBarChart(
                        weeklyStats = weeklyData,
                        onDayClick = { dayIndex ->
                            selectedDayIndex = if (selectedDayIndex == dayIndex) null else dayIndex
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date navigation
                    DateNavigationRow(
                        dateText = uiState.currentWeekDate,
                        onPreviousClick = { viewModel.navigateToPreviousWeek() },
                        onNextClick = { viewModel.navigateToNextWeek() },
                        canGoNext = uiState.canGoNextWeek
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Apps Card - Show selected day or all apps
                WellbeingCard {
                    CardHeader(title = "All Apps")

                    // Determine which apps to show
                    val appsToShow = if (selectedDayIndex != null && selectedDayApps.isNotEmpty()) {
                        selectedDayApps
                    } else if (selectedDayIndex != null) {
                        emptyList()
                    } else {
                        uiState.allApps
                    }

                    if (appsToShow.isNotEmpty()) {
                        val maxUsage = appsToShow.maxOfOrNull { it.usageTimeMillis } ?: 1L

                        appsToShow.forEach { app ->
                            AppUsageItem(
                                appInfo = app,
                                maxUsage = maxUsage,
                                timerLimitMillis = uiState.appLimits[app.packageName],
                                onSetTimerClick = {
                                    selectedAppForTimer = app
                                    showTimerDialog = true
                                }
                            )
                            if (app != appsToShow.last()) {
                                Divider(color = com.example.digitalwellbeing.ui.theme.Border)
                            }
                        }
                    } else {
                        Text(
                            text = if (selectedDayIndex != null) "No usage data for this day" else "No usage data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }

    // Clear selected day when week changes
    LaunchedEffect(uiState.currentWeekDate) {
        selectedDayIndex = null
        selectedDayApps = emptyList()
    }

    // Load day's apps when a day is selected
    LaunchedEffect(selectedDayIndex) {
        selectedDayIndex?.let { dayIndex ->
            viewModel.getAppUsageForDay(dayIndex).collect { apps ->
                selectedDayApps = apps
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
