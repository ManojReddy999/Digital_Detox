package com.focus.digitalwellbeing.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.focus.digitalwellbeing.ui.theme.*

import com.focus.digitalwellbeing.data.repository.AppLimitRepository
import com.focus.digitalwellbeing.data.repository.CoinWalletRepository
import com.focus.digitalwellbeing.DigitalWellbeingApp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.outlined.AccessTime
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.clickable

import androidx.compose.foundation.BorderStroke
import com.focus.digitalwellbeing.data.repository.FocusRepository
import com.focus.digitalwellbeing.service.FocusScheduler
import com.focus.digitalwellbeing.data.model.FocusSession

/**
 * Dialog activity shown when an app is blocked due to timer limit
 */
class BlockingDialogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use modern Activity methods for showing over lock screen (API 27+)
        // These replace deprecated FLAG_SHOW_WHEN_LOCKED, FLAG_DISMISS_KEYGUARD, FLAG_TURN_SCREEN_ON
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        
        // Configure window to appear as overlay with translucent background
        window.apply {
            // Make background translucent to show the semi-transparent overlay
            setBackgroundDrawableResource(android.R.color.transparent)

            // Keep screen on while dialog is shown
            addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // Make it fullscreen
            addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "This app"
        val limitFormatted = intent.getStringExtra(EXTRA_LIMIT_FORMATTED) ?: "time limit"
        val isFocusMode = intent.getBooleanExtra(EXTRA_IS_FOCUS_MODE, false)
        
        val app = applicationContext as DigitalWellbeingApp
        val database = app.database
        
        // Repository for App Limits (only needed if not focus mode, but we init for simplicity if package exists)
        val appLimitRepository = if (packageName != null) {
            AppLimitRepository(database.appLimitDao())
        } else null
        
        // Repository for Coins (needed for both extending limits and exiting focus session)
        val coinRepository = CoinWalletRepository(database.coinWalletDao(), database.coinTransactionDao())
        
        // Repository for Focus (needed for Focus Mode)
        val focusRepository = if (isFocusMode) {
            val scheduler = FocusScheduler(applicationContext)
            FocusRepository(database.focusDao(), scheduler, applicationContext)
        } else null

        // Intercept Back Button to prevent dismissal
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing, or show a toast
                android.widget.Toast.makeText(
                    this@BlockingDialogActivity,
                    if (isFocusMode) "Focus Session Active! Cannot go back." 
                    else "Time limit reached! App is blocked.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
                // Optional: We can choose to mimic the "Close App" behavior if back is pressed
                // onClose() logic could go here if we wanted Back to go Home instead of just blocking.
                // But typically, a strict blocker ignores Back to force the user to interact with the buttons.
            }
        })

        setContent {
            DigitalWellbeingTheme {
                // State for Focus Session
                var activeSession by remember { mutableStateOf<FocusSession?>(null) }
                var coinBalance by remember { mutableStateOf(0) }
                val scope = rememberCoroutineScope()

                // Observe Focus Session if in focus mode
                if (isFocusMode && focusRepository != null) {
                    LaunchedEffect(Unit) {
                        focusRepository.activeSession.collect { 
                            activeSession = it
                            // If session ends while dialog is open, close it
                            if (it == null || !it.isActive) {
                                finish()
                            }
                        }
                    }
                }

                // Observe Coin Balance
                LaunchedEffect(Unit) {
                    database.coinWalletDao().getWallet().collect { wallet ->
                        coinBalance = wallet?.balance ?: 0
                    }
                }

                // Semi-transparent background overlay (more transparent)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    BlockingScreen(
                        appName = appName,
                        limitFormatted = limitFormatted,
                        isFocusMode = isFocusMode,
                        activeSession = activeSession,
                        coinBalance = coinBalance,
                        onClose = {
                            // Set grace period BEFORE navigating away to prevent re-trigger
                            AppBlockingAccessibilityService.setDismissalGracePeriod()
                            
                            // Go to home screen
                            val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                                addCategory(android.content.Intent.CATEGORY_HOME)
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeIntent)
                            finish()
                        },
                        onExtend = if (!isFocusMode && appLimitRepository != null && packageName != null) {
                            { minutes ->
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        // Cost is 1 coin per minute
                                        val cost = minutes
                                        
                                        val success = coinRepository.chargeCoins(
                                            amount = cost,
                                            reason = "Extended limit for $appName by ${minutes}m",
                                            category = com.focus.digitalwellbeing.data.model.TransactionCategory.TIMER_EXTEND
                                        )

                                        if (success) {
                                            val limits = appLimitRepository.getEnabledLimits().first()
                                            val currentLimit = limits.find { it.packageName == packageName }
                                            
                                            if (currentLimit != null) {
                                                // Calculate current usage to ensure we extend FROM current usage if it exceeded limit
                                                val usageStatsManager = getSystemService(android.content.Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
                                                val endTime = System.currentTimeMillis()
                                                val calendar = java.util.Calendar.getInstance()
                                                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                calendar.set(java.util.Calendar.MINUTE, 0)
                                                calendar.set(java.util.Calendar.SECOND, 0)
                                                calendar.set(java.util.Calendar.MILLISECOND, 0)
                                                val startTime = calendar.timeInMillis

                                                val stats = usageStatsManager.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
                                                val currentUsageMillis = stats.filter { it.packageName == packageName }.sumOf { it.totalTimeInForeground }
                                                
                                                // New limit should be at least current usage + extension
                                                val baseTime = kotlin.math.max(currentLimit.limitMillis, currentUsageMillis)
                                                val newLimitMillis = baseTime + (minutes * 60 * 1000L)
                                                
                                                appLimitRepository.setAppLimit(
                                                    packageName = packageName,
                                                    appName = appName,
                                                    limitMillis = newLimitMillis
                                                )
                                                // Finish activity to unblock
                                                finish()
                                            }
                                        } else {
                                            launch(kotlinx.coroutines.Dispatchers.Main) {
                                                android.widget.Toast.makeText(
                                                    this@BlockingDialogActivity,
                                                    "Insufficient coins! Redirecting to shop...",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                                
                                                // Redirect to shop
                                                val intent = android.content.Intent(this@BlockingDialogActivity, com.focus.digitalwellbeing.MainActivity::class.java).apply {
                                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    putExtra(EXTRA_OPEN_SHOP, true)
                                                }
                                                startActivity(intent)
                                                finish()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        } else null,
                        onExitFocusSession = if (isFocusMode && focusRepository != null) {
                            { cost ->
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val success = coinRepository.chargeCoins(
                                        amount = cost,
                                        reason = "Early focus session termination",
                                        category = com.focus.digitalwellbeing.data.model.TransactionCategory.FOCUS_SKIP
                                    )
                                    
                                    if (success) {
                                        focusRepository.stopSession()
                                        finish()
                                    } else {
                                        launch(kotlinx.coroutines.Dispatchers.Main) {
                                            android.widget.Toast.makeText(
                                                this@BlockingDialogActivity,
                                                "Insufficient coins!",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        } else null,
                        onGetCoins = {
                            val intent = android.content.Intent(this@BlockingDialogActivity, com.focus.digitalwellbeing.MainActivity::class.java).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra(EXTRA_OPEN_SHOP, true)
                            }
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_LIMIT_FORMATTED = "extra_limit_formatted"
        const val EXTRA_IS_FOCUS_MODE = "extra_is_focus_mode"
        const val EXTRA_OPEN_SHOP = "extra_open_shop"
    }
}

@Composable
private fun BlockingScreen(
    appName: String,
    limitFormatted: String,
    isFocusMode: Boolean,
    activeSession: FocusSession? = null,
    coinBalance: Int = 0,
    onClose: () -> Unit,
    onExtend: ((Int) -> Unit)? = null,
    onExitFocusSession: ((Int) -> Unit)? = null,
    onGetCoins: () -> Unit
) {
    var showExtendOptions by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    // Calculate Focus Session stats
    val currentTime = System.currentTimeMillis()
    val isIndefinite = activeSession?.endTime == null
    
    val remainingMinutes = activeSession?.endTime?.let {
        ((it - currentTime) / (60 * 1000)).toInt().coerceAtLeast(0)
    } ?: 0
    
    val earlyExitCost = if (isIndefinite) 200 else remainingMinutes

    // Re-calculate periodically to keep UI fresh if dialog stays open
    // In a real app we might use a flow text ticker, but this is sufficient for now

    val canAffordExit = coinBalance >= earlyExitCost

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header Row: Icon + Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFocusMode) "Focus Session Active" else "Time's Up",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (!isFocusMode) {
                            Text(
                                text = "Limit: $limitFormatted",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Message
                Text(
                    text = if (isFocusMode) "This app is blocked during your focus session." else "You've reached your daily limit for $appName.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Inline Exit Confirmation Info
                if (showExitConfirmation && isFocusMode) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Stop Session Early?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Duration Left", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (isIndefinite) "Until turned off" else "${remainingMinutes}m", 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                             Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Early Termination Fee", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "$earlyExitCost coins", 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    fontWeight = FontWeight.Bold,
                                    color = if (canAffordExit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha=0.5f)
                                )
                            }
                            
                            if (!canAffordExit) {
                                Text(
                                    "Insufficient coins! Balance: $coinBalance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                if (showExtendOptions) {
                    Text(
                        text = "Extend for (1 coin/min):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    val options = listOf(5, 10, 15, 30)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.forEach { mins ->
                            FilledTonalButton(
                                onClick = { onExtend?.invoke(mins) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(45.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(
                                    "${mins}m",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = { showExtendOptions = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Close Button (Primary) - Always visible
                        Button(
                            onClick = onClose,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = "Close App",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Extend Button (Secondary)
                        if (onExtend != null && !showExitConfirmation) {
                            OutlinedButton(
                                onClick = { showExtendOptions = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    Icons.Outlined.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Extend Time",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        // Exit Focus Session Button (Secondary) - Only for Focus Mode
                        if (isFocusMode && onExitFocusSession != null) {
                            if (showExitConfirmation) {
                                // STOP & PAY Button
                                Button(
                                    onClick = {
                                        if (canAffordExit) {
                                            onExitFocusSession(earlyExitCost)
                                        } else {
                                            onGetCoins()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Text(
                                        text = if (canAffordExit) "Stop & Pay $earlyExitCost Coins" else "Get Coins",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                // Initial Exit Button
                                OutlinedButton(
                                    onClick = { showExitConfirmation = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(
                                        text = "Exit Focus Session",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
