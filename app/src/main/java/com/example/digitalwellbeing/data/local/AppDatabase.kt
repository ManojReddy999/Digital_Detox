package com.example.digitalwellbeing.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.digitalwellbeing.data.model.*

@Database(
    entities = [
        AppUsageInfo::class,
        DailyStats::class,
        Task::class,
        AppLimit::class,
        Challenge::class,
        HistoricalUsageData::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun taskDao(): TaskDao
    abstract fun appLimitDao(): AppLimitDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun historicalUsageDao(): HistoricalUsageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "digital_wellbeing_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
