package com.example.digitalwellbeing.ui.screens.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalwellbeing.ui.components.*
import com.example.digitalwellbeing.ui.theme.*

@Composable
fun FocusScreen(
    onNavigateToChess: () -> Unit,
    onNavigateToSudoku: () -> Unit,
    onNavigateToMath: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(CardBackground)
                    .border(1.dp, Border, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⏱",
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Time's Up",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.6).sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "You've reached your daily limit for this app.\nTry something productive instead.",
                fontSize = 16.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            TimerBadge(text = "Time saved today: 45 min")

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Choose an Activity",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Alternatives Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AlternativeCard(
                    title = "Chess Puzzle",
                    icon = "♟",
                    onClick = onNavigateToChess,
                    modifier = Modifier.weight(1f)
                )
                AlternativeCard(
                    title = "Sudoku",
                    icon = "#",
                    onClick = onNavigateToSudoku,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AlternativeCard(
                    title = "Math",
                    icon = "÷",
                    onClick = onNavigateToMath,
                    modifier = Modifier.weight(1f)
                )
                AlternativeCard(
                    title = "My Tasks",
                    icon = "✓",
                    onClick = onNavigateToTasks,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            PrimaryButton(
                text = "Start Activity",
                onClick = onNavigateToChess
            )

            Spacer(modifier = Modifier.height(12.dp))

            SecondaryButton(
                text = "Maybe Later",
                onClick = onDismiss
            )
        }
    }
}

@Composable
fun AlternativeCard(
    title: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                letterSpacing = (-0.2).sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
