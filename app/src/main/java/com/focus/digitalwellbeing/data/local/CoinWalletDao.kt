package com.focus.digitalwellbeing.data.local

import androidx.room.*
import com.focus.digitalwellbeing.data.model.CoinWallet
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinWalletDao {
    
    @Query("SELECT * FROM coin_wallet WHERE id = 1")
    fun getWallet(): Flow<CoinWallet?>
    
    @Query("SELECT balance FROM coin_wallet WHERE id = 1")
    fun getBalance(): Flow<Int?>

    @Query("SELECT balance FROM coin_wallet WHERE id = 1")
    suspend fun getBalanceValue(): Int?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWallet(wallet: CoinWallet)
    
    @Update
    suspend fun updateWallet(wallet: CoinWallet)
    
    @Query("UPDATE coin_wallet SET balance = balance + :amount, lifetimeEarned = lifetimeEarned + :amount, lastUpdated = :timestamp WHERE id = 1")
    suspend fun addCoins(amount: Int, timestamp: Long)
    
    @Query("UPDATE coin_wallet SET balance = balance - :amount, lifetimeSpent = lifetimeSpent + :amount, lastUpdated = :timestamp WHERE id = 1")
    suspend fun deductCoins(amount: Int, timestamp: Long)
}

