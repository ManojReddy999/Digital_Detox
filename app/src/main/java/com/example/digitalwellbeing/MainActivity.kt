package com.example.digitalwellbeing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.digitalwellbeing.ui.DigitalWellbeingMain
import com.example.digitalwellbeing.ui.MainViewModel
import com.example.digitalwellbeing.ui.ViewModelFactory
import com.example.digitalwellbeing.ui.theme.DigitalWellbeingTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        ViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DigitalWellbeingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DigitalWellbeingMain(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }
}
