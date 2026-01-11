package com.focus.digitalwellbeing.ui.screens.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.focus.digitalwellbeing.ui.MainViewModel

@Composable
fun PermissionSetupScreen(
    viewModel: MainViewModel
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val scrollState = rememberScrollState()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "To help you manage your digital wellbeing effectively, this app needs the following permissions.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Usage Stats Permission
        PermissionItem(
            title = "Usage Access",
            description = "Required to track your daily screen time.",
            isGranted = dashboardState.usagePermissionGranted == true,
            onGrant = { viewModel.checkUsageStatsPermission() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Overlay Permission
        PermissionItem(
            title = "Display Over Apps",
            description = "Required to show the timer overlay when you're using other apps.",
            isGranted = dashboardState.overlayPermissionGranted == true,
            onGrant = { viewModel.requestOverlayPermission() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Accessibility Permission
        var showAccessibilityDisclosure by remember { mutableStateOf(false) }
        
        PermissionItem(
            title = "Accessibility Service",
            description = "Required to detect when you open apps and enforce limits.",
            isGranted = dashboardState.accessibilityEnabled == true,
            onGrant = { showAccessibilityDisclosure = true }
        )

        if (showAccessibilityDisclosure) {
            AlertDialog(
                onDismissRequest = { showAccessibilityDisclosure = false },
                title = { Text("Accessibility Service Disclosure") },
                text = {
                    Column {
                        Text("This app uses the AccessibilityService API to detect which app is currently in the foreground.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("This is required to:")
                        Text("• Monitor your app usage in real-time")
                        Text("• Display a blocking overlay when you exceed your set limits")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No personal data or content is collected, stored, or shared via this service.")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAccessibilityDisclosure = false
                            viewModel.requestAccessibilityPermission()
                        }
                    ) {
                        Text("Agree & Continue")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAccessibilityDisclosure = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
    }
}


@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Grant")
                }
            }
        }
    }
}

