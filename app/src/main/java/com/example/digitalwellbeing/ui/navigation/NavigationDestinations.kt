package com.example.digitalwellbeing.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable

sealed class Screen(val route: String) {
    object Stats : Screen("stats")
}

