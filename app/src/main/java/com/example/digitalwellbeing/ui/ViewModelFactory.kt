package com.example.digitalwellbeing.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.digitalwellbeing.DigitalWellbeingApp
import com.example.digitalwellbeing.data.repository.AppLimitRepository
import com.example.digitalwellbeing.data.repository.AppPreferencesRepository
import com.example.digitalwellbeing.data.repository.ChallengeRepository
import com.example.digitalwellbeing.data.repository.TaskRepository
import com.example.digitalwellbeing.data.repository.UsageRepository
import com.example.digitalwellbeing.util.UsageStatsHelper

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val application = context.applicationContext as DigitalWellbeingApp
        val database = application.database

        val usageStatsHelper = UsageStatsHelper(context.applicationContext)

        val usageRepository = UsageRepository(
            appUsageDao = database.appUsageDao(),
            dailyStatsDao = database.dailyStatsDao(),
            usageStatsHelper = usageStatsHelper
        )

        val challengeRepository = ChallengeRepository(
            challengeDao = database.challengeDao()
        )

        val taskRepository = TaskRepository(
            taskDao = database.taskDao()
        )

        val appLimitRepository = AppLimitRepository(
            appLimitDao = database.appLimitDao()
        )

        val appPreferencesRepository = AppPreferencesRepository(
            context = context.applicationContext
        )

        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(
                    context = context.applicationContext,
                    usageRepository = usageRepository,
                    challengeRepository = challengeRepository,
                    taskRepository = taskRepository,
                    appLimitRepository = appLimitRepository,
                    appPreferencesRepository = appPreferencesRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
