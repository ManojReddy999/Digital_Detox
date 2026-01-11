package com.focus.digitalwellbeing

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.focus.digitalwellbeing.data.repository.AppTheme
import com.focus.digitalwellbeing.ui.DigitalWellbeingMain
import com.focus.digitalwellbeing.ui.MainViewModel
import com.focus.digitalwellbeing.ui.ViewModelFactory
import com.focus.digitalwellbeing.ui.theme.DigitalWellbeingTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        ViewModelFactory(this)
    }

    private var showStopFocusDialog by mutableStateOf(false)
    private var openShop by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display for Android 15+
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Ensure window background is white to avoid black screen flash
        window.setBackgroundDrawableResource(android.R.color.white)

        try {
            // Trigger immediate sync if permission is already granted
            viewModel.syncUsageData()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "ViewModel init failed", e)
        }
        
        // Check if we should show stop focus dialog (from notification)
        handleIntent(intent)

        setContent {
            val currentTheme by viewModel.currentTheme.collectAsState()
            val systemInDarkTheme = isSystemInDarkTheme()

            val darkTheme = when (currentTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> systemInDarkTheme
            }

            DigitalWellbeingTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DigitalWellbeingMain(
                        viewModel = viewModel,
                        showStopFocusDialog = showStopFocusDialog,
                        openShop = openShop
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent?) {
        val shouldShowDialog = intent?.getBooleanExtra("SHOW_STOP_FOCUS_DIALOG", false) ?: false
        val shouldOpenShop = intent?.getBooleanExtra("extra_open_shop", false) ?: false
        android.util.Log.d("MainActivity", "handleIntent: showStopDialog=$shouldShowDialog, openShop=$shouldOpenShop")
        showStopFocusDialog = shouldShowDialog
        openShop = shouldOpenShop
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
        

    }
}
