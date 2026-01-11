package com.focus.digitalwellbeing.data.local

import androidx.room.*
import com.focus.digitalwellbeing.data.model.CoinTransaction
import com.focus.digitalwellbeing.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinTransactionDao {
    
    @Insert
    suspend fun insertTransaction(transaction: CoinTransaction): Long
    
    @Query("SELECT * FROM coin_transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 50): Flow<List<CoinTransaction>>
    
    @Query("SELECT * FROM coin_transactions WHERE timestamp >= :startDate ORDER BY timestamp DESC")
    fun getTransactionsSince(startDate: Long): Flow<List<CoinTransaction>>
    
    @Query("SELECT SUM(amount) FROM coin_transactions WHERE type = :type AND timestamp >= :startDate AND amount > 0")
    suspend fun getTotalRewardsSince(type: TransactionType = TransactionType.REWARD, startDate: Long): Int?
    
    @Query("SELECT ABS(SUM(amount)) FROM coin_transactions WHERE type = :type AND timestamp >= :startDate AND amount < 0")
    suspend fun getTotalChargesSince(type: TransactionType = TransactionType.CHARGE, startDate: Long): Int?
    
    @Query("DELETE FROM coin_transactions WHERE timestamp < :beforeDate")
    suspend fun deleteOldTransactions(beforeDate: Long)
}

