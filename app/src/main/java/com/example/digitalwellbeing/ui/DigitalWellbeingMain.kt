package com.example.digitalwellbeing.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.digitalwellbeing.ui.navigation.Screen
import com.example.digitalwellbeing.ui.screens.setup.OnboardingScreen
import com.example.digitalwellbeing.ui.screens.setup.PermissionSetupScreen
import com.example.digitalwellbeing.ui.screens.stats.StatsScreen
import com.example.digitalwellbeing.ui.theme.*

enum class AppScreen {
    PermissionSetup,
    Onboarding,
    Stats
}

@Composable
fun DigitalWellbeingMain(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val dashboardState by viewModel.dashboardState.collectAsState()

    // Simple routing logic
    when {
        dashboardState == null -> {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        !dashboardState.usagePermissionGranted || !dashboardState.accessibilityEnabled -> {
            // Show permission setup
            PermissionSetupScreen(
                viewModel = viewModel,
                onPermissionsGranted = {
                    // This will trigger a recomposition
                }
            )
        }
        !dashboardState.setupCompleted -> {
            // Show onboarding
            OnboardingScreen(
                viewModel = viewModel,
                onSetupComplete = {
                    viewModel.markSetupCompleted()
                }
            )
        }
        else -> {
            // Show stats screen
            StatsScreen(
                viewModel = viewModel,
                onNavigateBack = { /* No back navigation needed */ }
            )
        }
    }
}
