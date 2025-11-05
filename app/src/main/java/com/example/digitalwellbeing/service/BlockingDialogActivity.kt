package com.example.digitalwellbeing.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import com.example.digitalwellbeing.ui.theme.*

/**
 * Dialog activity shown when an app is blocked due to timer limit
 */
class BlockingDialogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make it a true overlay without bringing app to foreground
        window.apply {
            setFlags(
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            )
            // Make background translucent
            setBackgroundDrawableResource(android.R.color.transparent)
            // Show on top of everything including status bar
            addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }

        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "This app"
        val limitFormatted = intent.getStringExtra(EXTRA_LIMIT_FORMATTED) ?: "time limit"

        setContent {
            DigitalWellbeingTheme {
                // Semi-transparent background overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    BlockingScreen(
                        appName = appName,
                        limitFormatted = limitFormatted,
                        onClose = {
                            // Go to home screen
                            val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                                addCategory(android.content.Intent.CATEGORY_HOME)
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_LIMIT_FORMATTED = "extra_limit_formatted"
    }
}

@Composable
private fun BlockingScreen(
    appName: String,
    limitFormatted: String,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        color = Surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Time's Up!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.7).sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Message
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You've reached your $limitFormatted limit for",
                    fontSize = 15.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = appName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.4).sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Take a break and come back later.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Close button
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Black
                )
            ) {
                Text(
                    text = "OK",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
