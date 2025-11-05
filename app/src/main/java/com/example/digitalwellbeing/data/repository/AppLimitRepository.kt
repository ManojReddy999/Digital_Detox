package com.example.digitalwellbeing.data.repository

import com.example.digitalwellbeing.data.local.AppLimitDao
import com.example.digitalwellbeing.data.model.AppLimit
import kotlinx.coroutines.flow.Flow

class AppLimitRepository(private val appLimitDao: AppLimitDao) {

    fun getEnabledLimits(): Flow<List<AppLimit>> = appLimitDao.getEnabledLimits()

    fun getLimitForApp(packageName: String): Flow<AppLimit?> =
        appLimitDao.getLimitForApp(packageName)

    suspend fun setAppLimit(packageName: String, appName: String, limitMillis: Long) {
        appLimitDao.insertLimit(
            AppLimit(
                packageName = packageName,
                appName = appName,
                limitMillis = limitMillis,
                isEnabled = true
            )
        )
    }

    suspend fun updateLimit(limit: AppLimit) {
        appLimitDao.updateLimit(limit)
    }

    suspend fun deleteLimit(limit: AppLimit) {
        appLimitDao.deleteLimit(limit)
    }
}
