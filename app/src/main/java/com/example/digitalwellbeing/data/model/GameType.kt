package com.example.digitalwellbeing.data.model

/**
 * Types of mini games that can be used to unlock screen time
 */
enum class GameType(val displayName: String, val icon: String, val bonusMinutes: Int) {
    SUDOKU("Sudoku", "🔢", 5),
    MATH_PUZZLE("Math Puzzle", "➗", 3),
    CHESS_MOVE("Chess Move", "♟️", 5),
    MATH_QUESTION("Math Question", "📊", 2),
    RIDDLE("Riddle", "🧩", 4),
    JIGSAW_PUZZLE("Jigsaw Puzzle", "🧩", 6),
    PROBABILITY("Probability Question", "🎲", 4),
    LOGIC_PUZZLE("Logic Puzzle", "💡", 5)
}
