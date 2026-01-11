package com.focus.digitalwellbeing.ui.screens.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focus.digitalwellbeing.data.model.FocusGroup
import com.focus.digitalwellbeing.data.model.FocusType
import com.focus.digitalwellbeing.ui.MainViewModel
import com.focus.digitalwellbeing.ui.components.AppIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFocusGroupScreen(
    viewModel: MainViewModel,
    existingGroup: FocusGroup? = null,
    onNavigateBack: () -> Unit
) {
    var groupName by remember { mutableStateOf(existingGroup?.name ?: "") }
    var focusType by remember { mutableStateOf(existingGroup?.type ?: FocusType.BLOCKLIST) }
    var selectedPackages by remember { mutableStateOf(existingGroup?.appPackages?.toSet() ?: setOf()) }
    var scheduledTime by remember { mutableStateOf(existingGroup?.scheduledStartTime) }
    var scheduledDuration by remember { mutableStateOf(existingGroup?.scheduledDurationMinutes) }
    var searchQuery by remember { mutableStateOf("") }

    val appsOrderedByUsage = remember { viewModel.getAppsOrderedByUsage() }

    val filteredApps = if (searchQuery.isBlank()) {
        appsOrderedByUsage
    } else {
        appsOrderedByUsage.filter { (_, appName) ->
            appName.contains(searchQuery, ignoreCase = true)
        }
    }

    val isEditMode = existingGroup != null && existingGroup.id != 0L
    
    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Focus Group" else "Create Focus Group", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    Button(
                        onClick = {
                            if (groupName.isNotBlank() && selectedPackages.isNotEmpty()) {
                                val focusGroup = FocusGroup(
                                    id = existingGroup?.id ?: 0L,
                                    name = groupName,
                                    type = focusType,
                                    appPackages = selectedPackages.toList(),
                                    scheduledStartTime = scheduledTime,
                                    scheduledDurationMinutes = scheduledDuration
                                )
                                
                                if (isEditMode) {
                                    viewModel.updateFocusGroup(focusGroup)
                                } else {
                                    viewModel.createFocusGroup(focusGroup)
                                }
                                onNavigateBack()
                            }
                        },
                        enabled = groupName.isNotBlank() && selectedPackages.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // Fixed content at top
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Group Name Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Group Name",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        
                        TextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                            placeholder = { Text("Enter name", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Focus Type Card
                var expandedFocusType by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Focus Type",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        
                        ExposedDropdownMenuBox(
                            expanded = expandedFocusType,
                            onExpandedChange = { expandedFocusType = !expandedFocusType }
                        ) {
                            Row(
                                modifier = Modifier
                                    .menuAnchor()
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .width(150.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (focusType == FocusType.BLOCKLIST) "Blocklist" else "Allowlist",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = if (expandedFocusType) 
                                        androidx.compose.material.icons.Icons.Default.KeyboardArrowUp 
                                    else 
                                        androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Dropdown",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            ExposedDropdownMenu(
                                expanded = expandedFocusType,
                                onDismissRequest = { expandedFocusType = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Blocklist (Block selected)") },
                                    onClick = {
                                        focusType = FocusType.BLOCKLIST
                                        expandedFocusType = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Allowlist (Allow selected)") },
                                    onClick = {
                                        focusType = FocusType.ALLOWLIST
                                        expandedFocusType = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Schedule & Duration Row
                var showTimePicker by remember { mutableStateOf(false) }
                var expandedDuration by remember { mutableStateOf(false) }
                val durationOptions = listOf(
                    "15 minutes" to 15,
                    "30 minutes" to 30,
                    "1 hour" to 60,
                    "2 hours" to 120,
                    "4 hours" to 240
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Schedule Start Time Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePicker = true }
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Schedule Start",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                                if (scheduledTime != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = scheduledTime!!,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            if (scheduledTime != null) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clickable { scheduledTime = null }
                                )
                            } else {
                                Icon(Icons.Default.DateRange, "Select", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Select Default Duration Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expandedDuration,
                            onExpandedChange = { expandedDuration = !expandedDuration }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                                    .menuAnchor(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Select Duration",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                    durationOptions.find { it.second == scheduledDuration }?.first?.let { duration ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = duration,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                Icon(
                                    imageVector = if (expandedDuration)
                                        androidx.compose.material.icons.Icons.Default.KeyboardArrowUp
                                    else
                                        androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Dropdown",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            ExposedDropdownMenu(
                                expanded = expandedDuration,
                                onDismissRequest = { expandedDuration = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None (Manual stop)") },
                                    onClick = {
                                        scheduledDuration = null
                                        expandedDuration = false
                                    }
                                )
                                durationOptions.forEach { (label, minutes) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            scheduledDuration = minutes
                                            expandedDuration = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Time Picker Dialog
                if (showTimePicker) {
                    val timeState = rememberTimePickerState(
                        initialHour = scheduledTime?.split(":")?.get(0)?.toIntOrNull() ?: 9,
                        initialMinute = scheduledTime?.split(":")?.get(1)?.toIntOrNull() ?: 0
                    )
                    
                    AlertDialog(
                        onDismissRequest = { showTimePicker = false },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
                        containerColor = MaterialTheme.colorScheme.surface,
                        confirmButton = {
                            TextButton(onClick = {
                                val hour = timeState.hour.toString().padStart(2, '0')
                                val minute = timeState.minute.toString().padStart(2, '0')
                                scheduledTime = "$hour:$minute"
                                showTimePicker = false
                            }) { Text("OK", color = MaterialTheme.colorScheme.primary) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        },
                        text = { TimePicker(state = timeState) }
                    )
                }

                // Select Apps Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Selected Apps:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    // Show app icons inline
                    if (selectedPackages.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val maxIconsToShow = 5
                            val packagesToShow = selectedPackages.take(maxIconsToShow)
                            val remainingCount = (selectedPackages.size - maxIconsToShow).coerceAtLeast(0)

                            packagesToShow.forEach { packageName ->
                                AppIcon(
                                    packageName = packageName,
                                    appName = "",
                                    size = 24.dp,
                                    modifier = Modifier
                                )
                            }

                            if (remainingCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+$remainingCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Search
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable App List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredApps) { (packageName, appName) ->
                    AppSelectionItem(
                        packageName = packageName,
                        appName = appName,
                        isSelected = selectedPackages.contains(packageName),
                        onToggle = { isSelected ->
                            selectedPackages = if (isSelected) {
                                selectedPackages + packageName
                            } else {
                                selectedPackages - packageName
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppSelectionItem(
    packageName: String,
    appName: String,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isSelected) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(
            packageName = packageName,
            appName = appName,
            size = 40.dp,
            modifier = Modifier.padding(end = 16.dp)
        )
        
        Text(
            text = appName,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        
        Checkbox(
            checked = isSelected,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}

