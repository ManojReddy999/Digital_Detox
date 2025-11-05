package com.example.digitalwellbeing.ui.screens.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalwellbeing.data.model.ChallengeType
import com.example.digitalwellbeing.ui.MainViewModel
import com.example.digitalwellbeing.ui.components.*
import com.example.digitalwellbeing.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var answer by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Math Challenge",
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
            TimerBadge(text = "2:00")

            Spacer(modifier = Modifier.height(24.dp))

            WellbeingProgressBar(progress = 0.6f)

            Spacer(modifier = Modifier.height(24.dp))

            StatBadge(value = "Question 3", label = "of 5")

            Spacer(modifier = Modifier.height(24.dp))

            // Math Problem
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardBackground
            ) {
                Column(
                    modifier = Modifier
                        .border(1.dp, Border, RoundedCornerShape(16.dp))
                        .padding(48.dp, 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Solve for x",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "3x + 7 = 25",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.8).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Answer Input
            TextField(
                value = answer,
                onValueChange = { answer = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Your answer",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Surface,
                    focusedIndicatorColor = Black,
                    unfocusedIndicatorColor = Divider
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Number pad
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) { i ->
                        Button(
                            onClick = { answer += (i + 7) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("${i + 7}", fontSize = 20.sp)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) { i ->
                        Button(
                            onClick = { answer += (i + 4) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("${i + 4}", fontSize = 20.sp)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) { i ->
                        Button(
                            onClick = { answer += (i + 1) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("${i + 1}", fontSize = 20.sp)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { answer += "0" },
                        modifier = Modifier
                            .weight(2f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("0", fontSize = 20.sp)
                    }
                    Button(
                        onClick = { if (answer.isNotEmpty()) answer = answer.dropLast(1) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardBackground,
                            contentColor = Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("×", fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Submit",
                onClick = {
                    viewModel.completeChallenge(ChallengeType.MATH, 120)
                    onNavigateBack()
                }
            )
        }
    }
}
