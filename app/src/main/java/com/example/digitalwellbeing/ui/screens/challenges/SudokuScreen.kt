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
fun SudokuScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Sudoku",
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
            TimerBadge(text = "8:32")

            Spacer(modifier = Modifier.height(24.dp))

            WellbeingProgressBar(progress = 0.35f)

            Spacer(modifier = Modifier.height(32.dp))

            // Sudoku Grid Placeholder
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBackground)
                    .border(1.dp, Divider, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sudoku puzzle\nwould appear here",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Number buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) { i ->
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("${i + 1}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) { i ->
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (i < 4) Black else CardBackground,
                            contentColor = if (i < 4) White else Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (i < 4) "${i + 6}" else "×")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Submit",
                onClick = {
                    viewModel.completeChallenge(ChallengeType.SUDOKU, 512)
                    onNavigateBack()
                }
            )
        }
    }
}
