package com.focus.digitalwellbeing.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.navigationBars
import kotlinx.coroutines.launch



@Composable
fun AndroidTimerPicker(
    appName: String,
    initialHours: Int = 0,
    initialMinutes: Int = 30,
    hasExistingTimer: Boolean = false,
    coinCost: Int = 0,
    coinBalance: Int = 0,
    onTimeChanged: ((hours: Int, minutes: Int) -> Unit)? = null,
    onTimeSelected: (hours: Int, minutes: Int) -> Unit,
    onDelete: (() -> Unit)? = null,
    onGetCoins: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialHours) }
    var selectedMinute by remember { mutableStateOf(initialMinutes) }

    // Call onTimeChanged whenever time changes
    LaunchedEffect(selectedHour, selectedMinute) {
        onTimeChanged?.invoke(selectedHour, selectedMinute)
    }

    val hours = (0..23).toList()
    val minutes = (0..55 step 5).toList()

    val deleteCost = 100
    val canAffordDelete = coinBalance >= deleteCost
    val canAffordSet = coinCost == 0 || coinBalance >= coinCost

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
                    .windowInsetsPadding(WindowInsets.navigationBars) // Push up above nav bar
                    .padding(bottom = 24.dp), 
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        if (hasExistingTimer) "Edit app timer" else "Set app timer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Subtitle
                    Text(
                        "Timer for $appName will reset at midnight",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Coin Balance Display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Your Balance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$coinBalance coins",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Time Picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hours Column
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Hours",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            WheelPicker(
                                items = hours,
                                initialItem = initialHours,
                                itemHeight = 40.dp,
                                onItemSelected = { selectedHour = it }
                            )
                        }

                        // Separator
                        Text(
                            ":",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .offset(y = 10.dp)
                        )

                        // Minutes Column
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Minutes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            WheelPicker(
                                items = minutes,
                                initialItem = initialMinutes,
                                itemHeight = 40.dp,
                                onItemSelected = { selectedMinute = it }
                            )
                        }
                    }

                    // Coin cost display (shown when there's a cost)
                    if (coinCost > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Cost to increase",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$coinCost coins",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (canAffordSet) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                            )
                        }
                        
                        if (!canAffordSet) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Insufficient coins! Balance: $coinBalance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel button
                        TextButton(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                "Cancel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Delete button (if existing timer)
                        if (hasExistingTimer && onDelete != null) {
                            if (canAffordDelete) {
                                TextButton(
                                    onClick = onDelete,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(
                                        "Delete ($deleteCost)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                TextButton(
                                    onClick = onGetCoins,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(
                                        "Get Coins",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Set button
                        if (canAffordSet) {
                            TextButton(
                                onClick = { onTimeSelected(selectedHour, selectedMinute) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    "Set",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            TextButton(
                                onClick = onGetCoins,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    "Get Coins",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    initialItem: T,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 3,
    itemHeight: Dp = 60.dp,
    onItemSelected: (T) -> Unit
) {
    val initialPage = items.indexOf(initialItem).coerceAtLeast(0)
    // Use a large number of pages to simulate infinite scrolling, or just clamp.
    // For simplicity and to match the previous behavior (finite list), we stick to finite.
    
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialPage,
        pageCount = { items.size }
    )
    val scope = rememberCoroutineScope()

    // Notify selection change when the settled page changes
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage in items.indices) {
            onItemSelected(items[pagerState.settledPage])
        }
    }

    // Haptics
    val hapticCallback = LocalHapticFeedback.current
    LaunchedEffect(pagerState.currentPage) {
        hapticCallback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
         // Central Divider Lines
         Divider(
             modifier = Modifier
                 .fillMaxWidth()
                 .offset(y = -(itemHeight/2)),
             color = MaterialTheme.colorScheme.outlineVariant,
             thickness = 1.dp
         )
         Divider(
             modifier = Modifier
                 .fillMaxWidth()
                 .offset(y = (itemHeight/2)),
             color = MaterialTheme.colorScheme.outlineVariant,
             thickness = 1.dp
         )
         
         androidx.compose.foundation.pager.VerticalPager(
             state = pagerState,
             // Shows 'visibleItemsCount' items (approx). 
             // We want the current page centered. 
             // contentPadding logic: we need (visibleItemsCount - 1) / 2 * itemHeight padding top/bottom
             contentPadding = PaddingValues(vertical = itemHeight * ((visibleItemsCount - 1) / 2)),
             modifier = Modifier.fillMaxSize(),
             flingBehavior = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(
                 state = pagerState,
                 // Customize snap animation if needed, but default is good for "Wheel" feel
             )
         ) { page ->
             val item = items[page]
             
             // Calculate offset from center for styling
             val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
             val absOffset = kotlin.math.abs(pageOffset)
             
             // Styling based on distance from center
             val scale = 1f + (0.1f * (1f - absOffset.coerceIn(0f, 1f))) // Reduced scale effect
             val alpha = (1f - (absOffset * 0.6f)).coerceIn(0.2f, 1f) // Increased fade
             val fontWeight = if (absOffset < 0.5f) FontWeight.Bold else FontWeight.Normal
             val color = if (absOffset < 0.5f) 
                 MaterialTheme.colorScheme.onSurface 
             else 
                 MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

             Box(
                 modifier = Modifier
                     .height(itemHeight)
                     .fillMaxWidth()
                     .graphicsLayer {
                         this.scaleX = scale
                         this.scaleY = scale
                         this.alpha = alpha
                     }
                     .clickable {
                        // Click to snap behavior
                        scope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                     },
                 contentAlignment = Alignment.Center
             ) {
                  Text(
                      text = item.toString(),
                      style = MaterialTheme.typography.titleLarge, // Slightly smaller font
                      color = color,
                      fontWeight = fontWeight
                  )
             }
         }
    }
}

