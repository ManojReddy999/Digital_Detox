package com.example.digitalwellbeing.ui.screens.setup

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalwellbeing.ui.MainViewModel
import com.example.digitalwellbeing.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSetupScreen(
    viewModel: MainViewModel,
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    var currentStep by remember { mutableStateOf(0) }

    val steps = listOf(
        PermissionStep.UsagePermission,
        PermissionStep.AccessibilityPermission,
        PermissionStep.OverlayPermission
    )

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Setup Your Digital Wellbeing",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.8).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Let's get you started with healthier digital habits",
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
                PermissionStep.UsagePermission -> {
                    PermissionStepContent(
                        icon = "📊",
                        title = "Usage Access Permission",
                        description = "Grant usage stats permission to track your screen time and app usage patterns.",
                        buttonText = "Grant Usage Permission",
                        isGranted = dashboardState.usagePermissionGranted,
                        onGrantClick = { viewModel.requestUsagePermission() },
                        onNextClick = {
                            if (dashboardState.usagePermissionGranted) {
                                currentStep++
                            }
                        }
                    )
                }
                PermissionStep.AccessibilityPermission -> {
                    PermissionStepContent(
                        icon = "🔒",
                        title = "Accessibility Service",
                        description = "Enable accessibility service to automatically block apps when your timers run out.",
                        buttonText = "Enable App Blocking",
                        isGranted = dashboardState.accessibilityEnabled,
                        onGrantClick = { viewModel.requestAccessibilityPermission() },
                        onNextClick = {
                            if (dashboardState.accessibilityEnabled) {
                                currentStep++
                            }
                        }
                    )
                }
                PermissionStep.OverlayPermission -> {
                    PermissionStepContent(
                        icon = "📱",
                        title = "Display Over Other Apps",
                        description = "Allow the app to show blocking overlays on top of apps when you exceed your limit. This creates a true overlay experience.",
                        buttonText = "Grant Overlay Permission",
                        isGranted = dashboardState.overlayPermissionGranted,
                        onGrantClick = { viewModel.requestOverlayPermission() },
                        onNextClick = {
                            onPermissionsGranted()
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
                val isCompleted = when (index) {
                    0 -> dashboardState.usagePermissionGranted
                    1 -> dashboardState.accessibilityEnabled
                    2 -> dashboardState.overlayPermissionGranted
                    else -> false
                }
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
}

enum class PermissionStep {
    UsagePermission,
    AccessibilityPermission,
    OverlayPermission
}

@Composable
private fun PermissionStepContent(
    icon: String,
    title: String,
    description: String,
    buttonText: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit,
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
            text = icon,
            fontSize = 72.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Title
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = description,
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Button
        if (!isGranted) {
            Button(
                onClick = onGrantClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Black
                )
            ) {
                Text(
                    text = buttonText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            // Show success state
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "✅",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = "Permission granted!",
                        fontSize = 16.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
}
