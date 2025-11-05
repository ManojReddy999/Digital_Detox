package com.example.digitalwellbeing.data.model

/**
 * Represents a game challenge that users must solve to unlock screen time
 */
sealed class GameChallenge {
    abstract val question: String
    abstract val correctAnswer: String
    abstract val gameType: GameType

    /**
     * Math problem challenge
     */
    data class MathChallenge(
        override val question: String,
        override val correctAnswer: String,
        val options: List<String> = emptyList()
    ) : GameChallenge() {
        override val gameType = GameType.MATH_QUESTION
    }

    /**
     * Logic/pattern challenge
     */
    data class LogicChallenge(
        override val question: String,
        override val correctAnswer: String,
        val options: List<String>
    ) : GameChallenge() {
        override val gameType = GameType.LOGIC_PUZZLE
    }

    /**
     * Riddle challenge
     */
    data class RiddleChallenge(
        override val question: String,
        override val correctAnswer: String,
        val hint: String? = null
    ) : GameChallenge() {
        override val gameType = GameType.RIDDLE
    }
}

/**
 * Generates random game challenges
 */
object ChallengeGenerator {

    /**
     * Generate a random challenge based on enabled game types
     */
    fun generateChallenge(enabledGames: Set<GameType>): GameChallenge {
        // Filter to games we have generators for
        val availableTypes = enabledGames.filter {
            it == GameType.MATH_QUESTION ||
            it == GameType.LOGIC_PUZZLE ||
            it == GameType.RIDDLE
        }

        val selectedType = if (availableTypes.isNotEmpty()) {
            availableTypes.random()
        } else {
            GameType.MATH_QUESTION // Fallback
        }

        return when (selectedType) {
            GameType.MATH_QUESTION -> generateMathChallenge()
            GameType.LOGIC_PUZZLE -> generateLogicChallenge()
            GameType.RIDDLE -> generateRiddle()
            else -> generateMathChallenge()
        }
    }

    /**
     * Generate a simple math challenge
     */
    fun generateMathChallenge(): GameChallenge.MathChallenge {
        val operations = listOf(
            Triple("addition", "+", { a: Int, b: Int -> a + b }),
            Triple("subtraction", "-", { a: Int, b: Int -> a - b }),
            Triple("multiplication", "×", { a: Int, b: Int -> a * b })
        )

        val (opName, opSymbol, operation) = operations.random()

        val (num1, num2) = when (opName) {
            "addition" -> Pair((10..50).random(), (10..50).random())
            "subtraction" -> {
                val a = (20..80).random()
                val b = (10 until a).random()
                Pair(a, b)
            }
            "multiplication" -> Pair((2..12).random(), (2..12).random())
            else -> Pair(10, 5)
        }

        val answer = operation(num1, num2)
        val question = "$num1 $opSymbol $num2 = ?"

        // Generate wrong options
        val wrongOptions = mutableSetOf<Int>()
        while (wrongOptions.size < 3) {
            val offset = (-5..5).random()
            if (offset != 0) {
                val wrong = answer + offset
                if (wrong > 0 && wrong != answer) {
                    wrongOptions.add(wrong)
                }
            }
        }

        val options = (wrongOptions.toList() + answer).shuffled().map { it.toString() }

        return GameChallenge.MathChallenge(
            question = question,
            correctAnswer = answer.toString(),
            options = options
        )
    }

    /**
     * Generate a logic/pattern challenge
     */
    fun generateLogicChallenge(): GameChallenge.LogicChallenge {
        val challenges = listOf(
            // Number sequences
            {
                val start = (1..10).random()
                val step = listOf(2, 3, 5).random()
                val sequence = List(4) { start + it * step }
                val next = start + 4 * step

                GameChallenge.LogicChallenge(
                    question = "What comes next?\n${sequence.joinToString(", ")}, ?",
                    correctAnswer = next.toString(),
                    options = listOf(
                        next.toString(),
                        (next + step).toString(),
                        (next - step).toString(),
                        (next + 1).toString()
                    ).shuffled()
                )
            },
            // Pattern completion
            {
                val patterns = listOf(
                    Triple("A, B, C, D, ?", "E", listOf("E", "F", "D", "A")),
                    Triple("2, 4, 8, 16, ?", "32", listOf("32", "24", "20", "18")),
                    Triple("1, 4, 9, 16, ?", "25", listOf("25", "20", "21", "24")),
                    Triple("Monday, Tuesday, Wednesday, ?", "Thursday",
                        listOf("Thursday", "Friday", "Saturday", "Sunday"))
                )
                val (q, a, opts) = patterns.random()
                GameChallenge.LogicChallenge(
                    question = q,
                    correctAnswer = a,
                    options = opts.shuffled()
                )
            }
        )

        return challenges.random()()
    }

    /**
     * Generate a simple riddle
     */
    fun generateRiddle(): GameChallenge.RiddleChallenge {
        val riddles = listOf(
            Triple(
                "I have keys but no locks. I have space but no room. You can enter but can't go inside. What am I?",
                "keyboard",
                "Think about something you use every day with computers"
            ),
            Triple(
                "What has hands but cannot clap?",
                "clock",
                "It tells you something important"
            ),
            Triple(
                "What gets wet while drying?",
                "towel",
                "You use it after a shower"
            ),
            Triple(
                "What has a head and tail but no body?",
                "coin",
                "You keep them in your wallet"
            ),
            Triple(
                "What can you catch but not throw?",
                "cold",
                "It's related to being sick"
            ),
            Triple(
                "What goes up but never comes down?",
                "age",
                "Everyone has it and it increases"
            )
        )

        val (question, answer, hint) = riddles.random()

        return GameChallenge.RiddleChallenge(
            question = question,
            correctAnswer = answer.lowercase(),
            hint = hint
        )
    }
}
