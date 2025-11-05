package com.example.digitalwellbeing.data.repository

import com.example.digitalwellbeing.data.local.ChallengeDao
import com.example.digitalwellbeing.data.model.Challenge
import com.example.digitalwellbeing.data.model.ChallengeType
import com.example.digitalwellbeing.util.DateUtils
import kotlinx.coroutines.flow.Flow

class ChallengeRepository(private val challengeDao: ChallengeDao) {

    fun getAllChallenges(): Flow<List<Challenge>> = challengeDao.getAllChallenges()

    fun getTodayCompletedCount(): Flow<Int> =
        challengeDao.getChallengesCompletedCount(DateUtils.getStartOfToday())

    suspend fun completeChallenge(type: ChallengeType, timeTakenSeconds: Int = 0) {
        challengeDao.insertChallenge(
            Challenge(
                type = type,
                timeTakenSeconds = timeTakenSeconds
            )
        )
    }

    suspend fun cleanOldChallenges() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        challengeDao.deleteOldChallenges(thirtyDaysAgo)
    }
}
