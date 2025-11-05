package com.example.digitalwellbeing.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalwellbeing.data.model.Task
import com.example.digitalwellbeing.ui.MainViewModel
import com.example.digitalwellbeing.ui.components.*
import com.example.digitalwellbeing.ui.theme.*

@Composable
fun TasksScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.tasksState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp)
        ) {
            Text(
                text = "Tasks",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.8).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your personal goals",
                fontSize = 15.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-0.2).sp
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 100.dp)
        ) {
            // Progress Card
            WellbeingCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = uiState.completedCount.toString(),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-1.5).sp
                        )
                        Text(
                            text = "/${uiState.totalCount}",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary.copy(alpha = 0.3f),
                            letterSpacing = (-1.5).sp
                        )
                    }
                    Text(
                        text = "Tasks completed",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                WellbeingProgressBar(
                    progress = uiState.progress,
                    modifier = Modifier.height(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Task List
            uiState.tasks.forEach { task ->
                TaskItem(
                    task = task,
                    onToggle = { viewModel.toggleTaskCompletion(task) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Add Task Button
            Spacer(modifier = Modifier.height(14.dp))
            PrimaryButton(
                text = "Add New Task",
                onClick = { showAddDialog = true }
            )
        }
    }

    // Add Task Dialog
    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title ->
                viewModel.addTask(title)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CardBackground
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(18.dp)
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (task.isCompleted) Black else Surface)
                    .border(
                        width = 2.dp,
                        color = if (task.isCompleted) Black else Divider,
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Task Text
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (task.isCompleted) TextTertiary else TextPrimary,
                fontWeight = FontWeight.Medium,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
            )
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var taskTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add New Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            TextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                placeholder = { Text("Enter task title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (taskTitle.isNotBlank()) {
                        onConfirm(taskTitle)
                    }
                },
                enabled = taskTitle.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
