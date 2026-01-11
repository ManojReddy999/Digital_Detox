package com.focus.digitalwellbeing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focus.digitalwellbeing.data.model.AppUsageInfo
import com.focus.digitalwellbeing.util.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush

/**
 * Modern card with minimal border (dashed style from mockup)
 */
@Composable
fun WellbeingCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(8.dp), // Less rounded, more minimal
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp), // Less padding
            content = content
        )
    }
}

/**
 * Modern Progress Bar with gradient
 */
@Composable
fun WellbeingProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.7f),
                            color
                        )
                    )
                )
        )
    }
}

/**
 * App Usage Item Row
 */
@Composable
fun AppUsageItem(
    app: AppUsageInfo,
    maxUsage: Long,
    appLimit: com.focus.digitalwellbeing.data.model.AppLimit? = null,
    onClick: () -> Unit
) {
    val progress = if (maxUsage > 0) app.usageTimeMillis.toFloat() / maxUsage else 0f
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon
        AppIcon(
            packageName = app.packageName,
            appName = app.appName,
            size = 48.dp
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = DateUtils.formatDuration(app.usageTimeMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Timer status
            if (appLimit != null) {
                val remaining = appLimit.limitMillis - app.usageTimeMillis
                val percentUsed = (app.usageTimeMillis.toFloat() / appLimit.limitMillis).coerceIn(0f, 1f)
                val statusColor = when {
                    percentUsed >= 1.0f -> MaterialTheme.colorScheme.error
                    percentUsed >= 0.8f -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.primary
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "â±ï¸",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = if (remaining > 0) {
                            "${DateUtils.formatDuration(appLimit.limitMillis)} limit â€¢ ${DateUtils.formatDuration(remaining)} left"
                        } else {
                            "${DateUtils.formatDuration(appLimit.limitMillis)} limit â€¢ Exceeded"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Text(
                    text = "â±ï¸ No limit set",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            WellbeingProgressBar(
                progress = if (appLimit != null) {
                    (app.usageTimeMillis.toFloat() / appLimit.limitMillis).coerceIn(0f, 1f)
                } else {
                    progress
                },
                color = if (appLimit != null) {
                    val percentUsed = app.usageTimeMillis.toFloat() / appLimit.limitMillis
                    when {
                        percentUsed >= 1.0f -> MaterialTheme.colorScheme.error
                        percentUsed >= 0.8f -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.primary
                    }
                } else {
                    if (app.usageTimeMillis > 2 * 60 * 60 * 1000) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

/**
 * Modern Primary Button (Pill shape)
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
        shape = RoundedCornerShape(50), // Pill shape
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD0D0D0), // Light gray background
            contentColor = Color.Black, // Black text
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        enabled = enabled,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Modern Secondary Button
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
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Timer Badge
 */
@Composable
fun TimerBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Activity Chart with time labels
 */
@Composable
fun ActivityChart(
    dataPoints: List<Float>, // Normalized 0..1 values or raw values
    labels: List<String> = listOf("12a", "6a", "12p", "6p", "12a"),
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            dataPoints.forEach { value ->
                Box(
                    modifier = Modifier
                        .width(4.dp) // Thin bars
                        .fillMaxHeight(value.coerceAtLeast(0.05f))
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(barColor)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Time Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Weekly Bar Chart (Wrapper for backward compatibility or specific weekly view)
 */
@Composable
fun WeeklyBarChart(
    weeklyStats: List<Pair<String, Long>>,
    maxUsage: Long
) {
    // Adapt weekly stats to ActivityChart format
    // This is a simplified adaptation. For real hourly data, we'd need a different data source.
    val dataPoints = weeklyStats.map { 
        if (maxUsage > 0) it.second.toFloat() / maxUsage else 0f 
    }
    
    ActivityChart(
        dataPoints = dataPoints,
        labels = weeklyStats.map { it.first.take(1) }, // M, T, W...
        barColor = MaterialTheme.colorScheme.primary
    )
}

/**
 * Coin Balance Card showing today's rewards vs charges (Compact Design)
 */
@Composable
fun CoinBalanceCard(
    todayRewards: Int,
    todayCharges: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Today's Balance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                val netChange = todayRewards - todayCharges
                Text(
                    text = "${if (netChange >= 0) "+" else ""}$${String.format("%.2f", netChange / 100f)}", // Assuming coins are cents
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (netChange >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rewards Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("â†‘", color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rewards", style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        text = "$${String.format("%.2f", todayRewards / 100f)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Charges Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("â†“", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Charges", style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        text = "$${String.format("%.2f", todayCharges / 100f)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

