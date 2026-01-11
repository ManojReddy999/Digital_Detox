package com.focus.digitalwellbeing.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.focus.digitalwellbeing.data.model.FocusGroup
import com.focus.digitalwellbeing.ui.screens.focus.CreateFocusGroupScreen
import com.focus.digitalwellbeing.ui.screens.focus.FocusScreen
import com.focus.digitalwellbeing.ui.screens.home.HomeScreen
import com.focus.digitalwellbeing.ui.screens.profile.ProfileScreen
import com.focus.digitalwellbeing.ui.screens.setup.PermissionSetupScreen
import com.focus.digitalwellbeing.ui.screens.setup.UserOnboardingScreen
import com.focus.digitalwellbeing.ui.screens.stats.StatsScreen
import com.focus.digitalwellbeing.ui.screens.shop.CoinShopScreen
import com.focus.digitalwellbeing.ui.theme.*

@Composable
fun DigitalWellbeingMain(
    viewModel: MainViewModel,
    showStopFocusDialog: Boolean = false,
    openShop: Boolean = false
) {
    val dashboardState by viewModel.dashboardState.collectAsState()

    // Simple routing logic
    android.util.Log.d("DigitalWellbeingMain", "Composing with state: permission=${dashboardState.usagePermissionGranted}, overlay=${dashboardState.overlayPermissionGranted}, accessibility=${dashboardState.accessibilityEnabled}, setup=${dashboardState.setupCompleted}, loading=${dashboardState.isLoading}")
    android.util.Log.d("DigitalWellbeingMain", "State details: $dashboardState")

    when {
        // dashboardState is StateFlow with initial value, so it won't be null
        // If we needed a loading state, we'd check a specific field in DashboardState
        !dashboardState.usagePermissionGranted || !dashboardState.overlayPermissionGranted || !dashboardState.accessibilityEnabled -> {
            android.util.Log.d("DigitalWellbeingMain", "Showing PermissionSetupScreen")
            // Show permission setup
            PermissionSetupScreen(
                viewModel = viewModel
            )
        }
        !dashboardState.setupCompleted -> {
            android.util.Log.d("DigitalWellbeingMain", "Showing UserOnboardingScreen")
            // Show user onboarding
            UserOnboardingScreen(
                viewModel = viewModel,
                onSetupComplete = {
                    // Setup marked completed in ViewModel
                }
            )
        }
        else -> {
            android.util.Log.d("DigitalWellbeingMain", "Showing Main App Content")
            // Main app content
            var selectedTab by remember { mutableStateOf(0) }
            var showCreateFocusGroup by remember { mutableStateOf(false) }
            var editingFocusGroup by remember { mutableStateOf<FocusGroup?>(null) }
            var showShop by remember { mutableStateOf(openShop) }

            // Handle openShop parameter changes
            LaunchedEffect(openShop) {
                if (openShop) {
                    showShop = true
                }
            }

            // Navigate to Focus tab if requested from notification
            LaunchedEffect(showStopFocusDialog) {
                if (showStopFocusDialog) {
                    selectedTab = 2 // Focus tab
                }
            }

    Box(modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Main app with navigation
        AnimatedVisibility(
            visible = !showCreateFocusGroup && editingFocusGroup == null && !showShop,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(400))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Main content
                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        if (targetState > initialState) {
                            // Swiping left (going to next tab)
                            slideInHorizontally(
                                animationSpec = tween(400),
                                initialOffsetX = { it }
                            ) + fadeIn(animationSpec = tween(400)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(400),
                                targetOffsetX = { -it }
                            ) + fadeOut(animationSpec = tween(400))
                        } else {
                            // Swiping right (going to previous tab)
                            slideInHorizontally(
                                animationSpec = tween(400),
                                initialOffsetX = { -it }
                            ) + fadeIn(animationSpec = tween(400)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(400),
                                targetOffsetX = { it }
                            ) + fadeOut(animationSpec = tween(400))
                        }
                    },
                    label = "tab_animation"
                ) { tab ->
                    when (tab) {
                        0 -> HomeScreen(
                            viewModel = viewModel,
                            onEditFocusGroup = { group -> editingFocusGroup = group },
                            onOpenShop = { showShop = true }
                        )
                        1 -> StatsScreen(
                            viewModel = viewModel,
                            onOpenShop = { showShop = true }
                        )
                        2 -> FocusScreen(
                            viewModel = viewModel,
                            onCreateFocusGroup = { showCreateFocusGroup = true },
                            onEditFocusGroup = { group -> editingFocusGroup = group },
                            showStopDialogOnLoad = showStopFocusDialog,
                            onOpenShop = { showShop = true }
                        )
                        3 -> ProfileScreen(viewModel = viewModel)
                    }
                }

                // Floating Navigation Bar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(28.dp),
                                clip = false,
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            ),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 3.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val navItems = listOf(
                                Triple(0, Icons.Default.Home, "Home"),
                                Triple(1, Icons.Default.BarChart, "Stats"),
                                Triple(2, Icons.Default.Timer, "Focus"),
                                Triple(3, Icons.Default.Person, "Profile")
                            )

                            navItems.forEach { (index, icon, label) ->
                                IconButton(
                                    onClick = { selectedTab = index },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                color = if (selectedTab == index)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    Color.Transparent,
                                                shape = RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (selectedTab == index)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Create/Edit Focus Group Screen
        AnimatedVisibility(
            visible = showCreateFocusGroup || editingFocusGroup != null,
            enter = slideInHorizontally(
                animationSpec = tween(400),
                initialOffsetX = { it }
            ) + fadeIn(animationSpec = tween(400)),
            exit = slideOutHorizontally(
                animationSpec = tween(400),
                targetOffsetX = { it }
            ) + fadeOut(animationSpec = tween(400))
        ) {
            CreateFocusGroupScreen(
                viewModel = viewModel,
                existingGroup = editingFocusGroup,
                onNavigateBack = { 
                    showCreateFocusGroup = false
                    editingFocusGroup = null
                }
            )
        }

        // Coin Shop Screen
        AnimatedVisibility(
            visible = showShop,
            enter = slideInVertically(
                animationSpec = tween(400),
                initialOffsetY = { it }
            ) + fadeIn(animationSpec = tween(400)),
            exit = slideOutVertically(
                animationSpec = tween(400),
                targetOffsetY = { it }
            ) + fadeOut(animationSpec = tween(400))
        ) {
            com.focus.digitalwellbeing.ui.screens.shop.CoinShopScreen(
                viewModel = viewModel,
                onNavigateBack = { showShop = false }
            )
        }
    }
        }
    }
}

