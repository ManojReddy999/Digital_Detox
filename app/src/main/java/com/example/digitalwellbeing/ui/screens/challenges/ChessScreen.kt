package com.example.digitalwellbeing.ui.screens.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalwellbeing.data.model.ChallengeType
import com.example.digitalwellbeing.ui.MainViewModel
import com.example.digitalwellbeing.ui.components.*
import com.example.digitalwellbeing.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Chess Puzzle",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TimerBadge(text = "5:00")

            Spacer(modifier = Modifier.height(24.dp))

            WellbeingProgressBar(progress = 0.6f)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "White to move and win in 2 moves",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.2).sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Chess Board Placeholder
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBackground)
                    .border(1.dp, Divider, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♔ ♕ ♖ ♗ ♘ ♙\n\nChess puzzle\nwould appear here",
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryButton(
                text = "Submit Move",
                onClick = {
                    viewModel.completeChallenge(ChallengeType.CHESS, 300)
                    onNavigateBack()
                }
            )
        }
    }
}
