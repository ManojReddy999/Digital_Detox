package com.focus.digitalwellbeing.data.repository

import com.focus.digitalwellbeing.data.local.CoinWalletDao
import com.focus.digitalwellbeing.data.local.CoinTransactionDao
import com.focus.digitalwellbeing.data.model.*
import com.focus.digitalwellbeing.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CoinWalletRepository(
    private val walletDao: CoinWalletDao,
    private val transactionDao: CoinTransactionDao
) {
    
    companion object {
        const val INITIAL_BALANCE = 1000 // Free coins for new users
    }
    
    /**
     * Get the current coin balance
     */
    fun getBalance(): Flow<Int> = walletDao.getBalance().map { it ?: 0 }
    
    /**
     * Get the full wallet info
     */
    fun getWallet(): Flow<CoinWallet?> = walletDao.getWallet()
    
    /**
     * Initialize wallet for new user
     */
    suspend fun initializeWallet() {
        // Check if wallet already exists
        // If not, create with initial balance
        walletDao.insertWallet(
            CoinWallet(
                id = 1,
                balance = INITIAL_BALANCE,
                lifetimeEarned = INITIAL_BALANCE,
                lifetimeSpent = 0,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }
    
    /**
     * Charge coins from the wallet
     * Returns true if successful, false if insufficient balance
     */
    suspend fun chargeCoins(
        amount: Int,
        reason: String,
        category: TransactionCategory
    ): Boolean {
        // Since this is Flow, we need to get the value
        // For now, we'll use a query to get balance directly
        val currentBalance = getBalanceSync()
        
        if (currentBalance < amount) {
            return false // Insufficient balance
        }
        
        // Deduct coins
        walletDao.deductCoins(amount, System.currentTimeMillis())
        
        // Record transaction
        val newBalance = currentBalance - amount
        transactionDao.insertTransaction(
            CoinTransaction(
                type = TransactionType.CHARGE,
                amount = -amount, // Negative for charges
                reason = reason,
                category = category,
                timestamp = System.currentTimeMillis(),
                balanceAfter = newBalance
            )
        )
        
        return true
    }
    
    /**
     * Award coins to the wallet
     */
    suspend fun awardCoins(
        amount: Int,
        reason: String,
        category: TransactionCategory
    ) {
        // Add coins
        walletDao.addCoins(amount, System.currentTimeMillis())
        
        // Record transaction
        val newBalance = getBalanceSync() // Get updated balance
        transactionDao.insertTransaction(
            CoinTransaction(
                type = TransactionType.REWARD,
                amount = amount, // Positive for rewards
                reason = reason,
                category = category,
                timestamp = System.currentTimeMillis(),
                balanceAfter = newBalance
            )
        )
    }
    
    /**
     * Add purchased coins
     */
    suspend fun purchaseCoins(amount: Int, purchaseId: String) {
        walletDao.addCoins(amount, System.currentTimeMillis())
        
        val newBalance = getBalanceSync()
        transactionDao.insertTransaction(
            CoinTransaction(
                type = TransactionType.PURCHASE,
                amount = amount,
                reason = "Purchased $amount coins - $purchaseId",
                category = TransactionCategory.COIN_PURCHASE,
                timestamp = System.currentTimeMillis(),
                balanceAfter = newBalance
            )
        )
    }
    
    /**
     * Get transaction history
     */
    fun getTransactionHistory(limit: Int = 50): Flow<List<CoinTransaction>> {
        return transactionDao.getRecentTransactions(limit)
    }
    
    /**
     * Get today's stats (rewards vs charges)
     * Returns Pair of (rewards, charges)
     */
    suspend fun getTodayStats(): Pair<Int, Int> {
        val todayStart = DateUtils.getStartOfToday()
        val rewards = transactionDao.getTotalRewardsSince(TransactionType.REWARD, todayStart) ?: 0
        val charges = transactionDao.getTotalChargesSince(TransactionType.CHARGE, todayStart) ?: 0
        return Pair(rewards, charges)
    }

    /**
     * Get total stats (all-time earned vs spent)
     * Returns Pair of (earned, spent)
     */
    suspend fun getTotalStats(): Pair<Int, Int> {
        val allRewards = transactionDao.getTotalRewardsSince(TransactionType.REWARD, 0L) ?: 0
        val allCharges = transactionDao.getTotalChargesSince(TransactionType.CHARGE, 0L) ?: 0
        return Pair(allRewards, allCharges)
    }

    /**
     * Helper to get balance synchronously (for internal use)
     */
    private suspend fun getBalanceSync(): Int {
        return walletDao.getBalanceValue() ?: 0
    }
}

