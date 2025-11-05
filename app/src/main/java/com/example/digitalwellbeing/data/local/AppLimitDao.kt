package com.example.digitalwellbeing.data.local

import androidx.room.*
import com.example.digitalwellbeing.data.model.AppLimit
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limits WHERE isEnabled = 1")
    fun getEnabledLimits(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName LIMIT 1")
    fun getLimitForApp(packageName: String): Flow<AppLimit?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLimit(limit: AppLimit)

    @Update
    suspend fun updateLimit(limit: AppLimit)

    @Delete
    suspend fun deleteLimit(limit: AppLimit)

    @Query("DELETE FROM app_limits")
    suspend fun deleteAll()
}
