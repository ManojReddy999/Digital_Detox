package com.focus.digitalwellbeing.data.model

/**
 * Types of mini games that can be used to unlock screen time
 */
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class GameType(val displayName: String, val icon: ImageVector, val bonusMinutes: Int) {
    SUDOKU("Sudoku", Icons.Default.Grid3x3, 5),
    MATH_PUZZLE("Math Puzzle", Icons.Default.Calculate, 3),
    CHESS_MOVE("Chess Move", Icons.Default.SportsEsports, 5),
    MATH_QUESTION("Math Question", Icons.Default.Functions, 2),
    RIDDLE("Riddle", Icons.AutoMirrored.Filled.Help, 4),
    JIGSAW_PUZZLE("Jigsaw Puzzle", Icons.Default.Extension, 6),
    PROBABILITY("Probability Question", Icons.Default.Shuffle, 4),
    LOGIC_PUZZLE("Logic Puzzle", Icons.Default.Lightbulb, 5)
}
