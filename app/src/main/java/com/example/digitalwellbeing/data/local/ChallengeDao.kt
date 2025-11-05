package com.example.digitalwellbeing.data.local

import androidx.room.*
import com.example.digitalwellbeing.data.model.Challenge
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges ORDER BY completedAt DESC")
    fun getAllChallenges(): Flow<List<Challenge>>

    @Query("SELECT COUNT(*) FROM challenges WHERE completedAt >= :startDate")
    fun getChallengesCompletedCount(startDate: Long): Flow<Int>

    @Insert
    suspend fun insertChallenge(challenge: Challenge)

    @Query("DELETE FROM challenges WHERE completedAt < :beforeDate")
    suspend fun deleteOldChallenges(beforeDate: Long)
}
