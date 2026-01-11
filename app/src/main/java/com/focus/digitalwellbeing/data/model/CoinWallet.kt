package com.focus.digitalwellbeing.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the user's coin wallet with current balance and lifetime stats
 */
@Entity(tableName = "coin_wallet")
data class CoinWallet(
    @PrimaryKey val id: Int = 1, // Single row
    val balance: Int, // Current coin balance
    val lifetimeEarned: Int, // Total coins ever earned
    val lifetimeSpent: Int, // Total coins ever spent
    val lastUpdated: Long
)

