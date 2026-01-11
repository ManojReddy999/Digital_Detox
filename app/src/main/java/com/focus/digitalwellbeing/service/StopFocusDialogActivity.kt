package com.focus.digitalwellbeing.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.focus.digitalwellbeing.data.local.AppDatabase
import com.focus.digitalwellbeing.data.repository.FocusRepository
import com.focus.digitalwellbeing.ui.components.EarlyTerminationConfirmationDialog
import com.focus.digitalwellbeing.ui.theme.DigitalWellbeingTheme
import kotlinx.coroutines.launch

class StopFocusDialogActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(applicationContext)
        val scheduler = FocusScheduler(applicationContext)
        val repository = FocusRepository(database.focusDao(), scheduler, applicationContext)
        
        setContent {
            DigitalWellbeingTheme {
                val scope = rememberCoroutineScope()
                var activeSession by remember { mutableStateOf<com.focus.digitalwellbeing.data.model.FocusSession?>(null) }
                var coinBalance by remember { mutableStateOf(0) }
                
                LaunchedEffect(Unit) {
                    repository.activeSession.collect { session ->
                        activeSession = session
                        // If no active session, close the dialog
                        if (session == null || !session.isActive) {
                            finish()
                        }
                    }
                }
                
                LaunchedEffect(Unit) {
                    val coinDao = database.coinWalletDao()
                    coinDao.getWallet().collect { wallet ->
                        coinBalance = wallet?.balance ?: 0
                    }
                }
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    activeSession?.let { session ->
                        EarlyTerminationConfirmationDialog(
                            session = session,
                            coinBalance = coinBalance,
                            onDismiss = { finish() },
                            onConfirm = {
                                scope.launch {
                                    // Charge coins
                                    val coinRepo = com.focus.digitalwellbeing.data.repository.CoinWalletRepository(
                                        database.coinWalletDao(),
                                        database.coinTransactionDao()
                                    )
                                    
                                    // Calculate charge based on remaining time logic in dialog
                                    // Ideally this logic should be shared or passed, but for now we replicate the simple calculation
                                    // The dialog calculates it for display, we need it for transaction
                                    val currentTime = System.currentTimeMillis()
                                    val isIndefinite = session.endTime == null
                                    
                                    val remainingMinutes = session.endTime?.let {
                                        ((it - currentTime) / (60 * 1000)).toInt().coerceAtLeast(0)
                                    } ?: 0
                                    val charge = if (isIndefinite) 200 else remainingMinutes
                                    
                                    if (coinBalance >= charge) {
                                        coinRepo.chargeCoins(
                                            amount = charge,
                                            reason = "Early focus session termination",
                                            category = com.focus.digitalwellbeing.data.model.TransactionCategory.FOCUS_SKIP
                                        )
                                        
                                        // Stop session
                                        repository.stopSession()
                                        finish()
                                    }
                                }
                            },
                            onGetCoins = {
                                val intent = android.content.Intent(this@StopFocusDialogActivity, com.focus.digitalwellbeing.MainActivity::class.java).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    putExtra("extra_open_shop", true)
                                }
                                startActivity(intent)
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}
