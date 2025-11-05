package com.example.digitalwellbeing.ui.components

import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.digitalwellbeing.data.model.AppUsageInfo
import com.example.digitalwellbeing.ui.theme.*
import kotlin.math.absoluteValue

/**
 * Card component with modern elevation and shadows
 */
@Composable
fun WellbeingCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = CardBackground,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp  // Subtle shadow for depth
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
        ) {
            content()
        }
    }
}

/**
 * Card header with title and optional action
 */
@Composable
fun CardHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

/**
 * Circular progress indicator for screen time
 */
@Composable
fun StatCircle(
    time: String,
    label: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(200.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Progress arc
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.size(168.dp),
            color = Black,
            strokeWidth = 8.dp,
            trackColor = Border
        )

        // Inner content
        Box(
            modifier = Modifier
                .size(152.dp)
                .clip(CircleShape)
                .background(CardBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = time,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.8).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * App usage item
 */
@Composable
fun AppUsageItem(
    appInfo: AppUsageInfo,
    maxUsage: Long,
    onSetTimerClick: () -> Unit = {},
    timerLimitMillis: Long? = null, // Timer limit in milliseconds, null if no timer set
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Load icon bitmap using remember to avoid recomposition and handle exception outside Composable
    val iconBitmap = remember(appInfo.packageName) {
        try {
            val icon = context.packageManager.getApplicationIcon(appInfo.packageName)
            icon.toBitmap(width = 88, height = 88) // 2x for quality
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    // Grayscale color matrix for black & white minimal theme
    val grayscaleMatrix = remember {
        ColorMatrix().apply {
            setToSaturation(0f) // Remove all color (grayscale)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon with grayscale filter for minimal black & white theme
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Surface)
                .border(1.dp, Divider, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap.asImageBitmap(),
                    contentDescription = "${appInfo.appName} icon",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    colorFilter = ColorFilter.colorMatrix(grayscaleMatrix)
                )
            } else {
                // Fallback to placeholder if icon not found
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(TextPrimary.copy(alpha = 0.2f))
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // App info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appInfo.appName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Show usage time with timer limit if set
            val usageText = if (timerLimitMillis != null) {
                val limitHours = timerLimitMillis / (1000 * 60 * 60)
                val limitMinutes = (timerLimitMillis % (1000 * 60 * 60)) / (1000 * 60)
                val limitFormatted = when {
                    limitHours > 0 -> "${limitHours}h ${limitMinutes}m"
                    limitMinutes > 0 -> "${limitMinutes}m"
                    else -> "<1m"
                }
                "${appInfo.usageTimeFormatted} / $limitFormatted"
            } else {
                appInfo.usageTimeFormatted
            }

            // Check if timer limit exceeded
            val isLimitExceeded = timerLimitMillis != null && appInfo.usageTimeMillis >= timerLimitMillis

            Text(
                text = usageText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isLimitExceeded) Color(0xFFD32F2F) else TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }

        // Hourglass icon button to set app timer
        IconButton(
            onClick = onSetTimerClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.HourglassEmpty,
                contentDescription = if (timerLimitMillis != null) "Edit timer for ${appInfo.appName}" else "Set timer for ${appInfo.appName}",
                tint = if (timerLimitMillis != null) TextPrimary else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Primary button matching HTML design
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Black,
            contentColor = White,
            disabledContainerColor = Black.copy(alpha = 0.5f)
        ),
        enabled = enabled
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp
        )
    }
}

/**
 * Secondary button matching HTML design
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = CardBackground,
            contentColor = Black
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Divider)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp
        )
    }
}

/**
 * Progress bar
 */
@Composable
fun WellbeingProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Border)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(Black)
        )
    }
}

/**
 * Timer badge
 */
@Composable
fun TimerBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            letterSpacing = (-0.2).sp
        )
    }
}

/**
 * Stat badge
 */
@Composable
fun StatBadge(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            fontSize = 13.sp,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun UsagePermissionCard(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WellbeingCard(modifier = modifier) {
        Text(
            text = "Usage access needed",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "To display your screen time, allow the app to access usage data in system settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryButton(
            text = "Grant permission",
            onClick = onGrantClick
        )
    }
}

@Composable
fun AccessibilityPermissionCard(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WellbeingCard(modifier = modifier) {
        Text(
            text = "App blocking needed",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Enable accessibility service to block apps when timer limits are exceeded. This allows the app to detect when you open blocked apps.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryButton(
            text = "Enable blocking",
            onClick = onGrantClick
        )
    }
}

/**
 * Weekly bar chart matching Digital Wellbeing UI
 */
@Composable
fun WeeklyBarChart(
    weeklyStats: List<Pair<String, Long>>, // List of (day label, usage in millis)
    onDayClick: ((Int) -> Unit)? = null, // Callback with day index (0-6 for Mon-Sun)
    modifier: Modifier = Modifier
) {
    val maxUsage = weeklyStats.maxOfOrNull { it.second } ?: 1L
    val maxUsageHours = maxUsage / (1000 * 60 * 60f)

    // Calculate y-axis labels (0 to max in 3 steps)
    val yAxisSteps = 3
    val stepValue = kotlin.math.ceil(maxUsageHours / yAxisSteps).toInt().coerceAtLeast(1)
    val yAxisLabels = (0..yAxisSteps).map { it * stepValue }

    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .width(32.dp)
                .height(140.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            yAxisLabels.reversed().forEach { hours ->
                Text(
                    text = "${hours}h",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Bar chart
        Row(
            modifier = Modifier
                .weight(1f)
                .height(140.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            weeklyStats.forEachIndexed { index, (day, usage) ->
                val interactionSource = remember { MutableInteractionSource() }
                val indication = rememberRipple(bounded = true, radius = 30.dp)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (onDayClick != null) {
                                Modifier.clickable(
                                    interactionSource = interactionSource,
                                    indication = indication,
                                    onClick = { onDayClick(index) }
                                )
                            } else Modifier
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Bar
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .weight(1f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val fillHeight = if (maxUsage > 0) {
                            (usage.toFloat() / maxUsage).coerceIn(0.02f, 1f) // Minimum 2% for visibility
                        } else 0.02f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fillHeight)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (usage > 0) Black else Border)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Day label
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Date navigation component
 */
@Composable
fun DateNavigationRow(
    dateText: String,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    canGoNext: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousClick) {
            Text(
                text = "<",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )
        }

        Text(
            text = dateText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        IconButton(
            onClick = onNextClick,
            enabled = canGoNext
        ) {
            Text(
                text = ">",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (canGoNext) Black else Border
            )
        }
    }
}

/**
 * App Timer Dialog matching Digital Wellbeing design with scrollable pickers
 */
@Composable
fun AppTimerDialog(
    appName: String,
    currentLimitMillis: Long?, // Existing timer limit, null if not set
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit, // Timer duration in minutes
    onDeleteTimer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Initialize with current timer values if exists
    val initialHours = currentLimitMillis?.let { (it / (1000 * 60 * 60)).toInt() } ?: 0
    val initialMinutes = currentLimitMillis?.let { ((it % (1000 * 60 * 60)) / (1000 * 60)).toInt() } ?: 15
    // Round to nearest 5 minutes
    val initialMinutesRounded = ((initialMinutes + 2) / 5) * 5

    var selectedHours by remember { mutableStateOf(initialHours) }
    var selectedMinutes by remember { mutableStateOf(initialMinutesRounded) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set app timer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal
            )
        },
        text = {
            Column {
                Text(
                    text = "This app timer for $appName will reset at midnight",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Time picker with hour and minute wheels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours picker
                    TimerPicker(
                        value = selectedHours,
                        range = 0..23,
                        label = if (selectedHours == 1) "hr" else "hrs",
                        onValueChange = { selectedHours = it },
                        modifier = Modifier.weight(1f),
                        displayValue = selectedHours.toString()
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Minutes picker (5-minute intervals: 0, 5, 10, 15...)
                    TimerPicker(
                        value = selectedMinutes / 5,
                        range = 0..11,
                        label = "mins",
                        onValueChange = { selectedMinutes = it * 5 },
                        modifier = Modifier.weight(1f),
                        displayValue = (selectedMinutes / 5 * 5).toString()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            Row {
                // Delete button if timer exists
                if (currentLimitMillis != null) {
                    TextButton(onClick = {
                        onDeleteTimer()
                        onDismiss()
                    }) {
                        Text(
                            text = "Delete",
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD32F2F) // Red color for delete
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                TextButton(
                    onClick = {
                        val totalMinutes = selectedHours * 60 + selectedMinutes
                        if (totalMinutes > 0) {
                            onSetTimer(totalMinutes)
                        }
                    }
                ) {
                    Text(
                        text = "OK",
                        fontWeight = FontWeight.SemiBold,
                        color = Black
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        },
        containerColor = CardBackground,
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Simple timer picker with +/- buttons
 */
@Composable
private fun TimerPicker(
    value: Int,
    range: IntRange,
    label: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    displayValue: String = value.toString()
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Increment button
        IconButton(
            onClick = {
                val newValue = (value + 1).coerceIn(range)
                onValueChange(newValue)
            },
            enabled = value < range.last
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Increase",
                tint = if (value < range.last) TextPrimary else TextPrimary.copy(alpha = 0.3f)
            )
        }

        // Value display
        Text(
            text = displayValue,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Decrement button
        IconButton(
            onClick = {
                val newValue = (value - 1).coerceIn(range)
                onValueChange(newValue)
            },
            enabled = value > range.first
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrease",
                tint = if (value > range.first) TextPrimary else TextPrimary.copy(alpha = 0.3f)
            )
        }

        // Label
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
