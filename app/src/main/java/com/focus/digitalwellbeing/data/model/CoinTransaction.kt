package com.focus.digitalwellbeing.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single coin transaction (charge or reward)
 */
@Entity(tableName = "coin_transactions")
data class CoinTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType, // CHARGE, REWARD, or PURCHASE
    val amount: Int, // Positive for rewards, negative for charges
    val reason: String, // Description of transaction
    val category: TransactionCategory, // What triggered this transaction
    val timestamp: Long,
    val balanceAfter: Int // Balance after this transaction
)

enum class TransactionType {
    CHARGE,
    REWARD,
    PURCHASE
}

enum class TransactionCategory {
    // Charges
    TIMER_SET,
    TIMER_ADJUST,
    TIMER_DELETED,
    TIMER_DELETE,
    TIMER_EXTEND,
    SCREEN_TIME_EXCEED,
    FOCUS_SKIP,
    
    // Rewards
    TIMER_SUCCESS,
    FOCUS_COMPLETE,
    SCREEN_TIME_SUCCESS,
    STREAK_BONUS,
    
    // Purchases
    COIN_PURCHASE
}

