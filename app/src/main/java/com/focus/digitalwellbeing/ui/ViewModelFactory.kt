package com.focus.digitalwellbeing.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.focus.digitalwellbeing.DigitalWellbeingApp
import com.focus.digitalwellbeing.data.repository.AppLimitRepository
import com.focus.digitalwellbeing.data.repository.AppPreferencesRepository
import com.focus.digitalwellbeing.data.repository.ChallengeRepository
import com.focus.digitalwellbeing.data.repository.CoinWalletRepository
import com.focus.digitalwellbeing.data.repository.StreakRepository
import com.focus.digitalwellbeing.data.repository.TaskRepository
import com.focus.digitalwellbeing.data.repository.UsageRepository
import com.focus.digitalwellbeing.data.repository.FocusRepository
import com.focus.digitalwellbeing.util.UsageStatsHelper

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

        val coinWalletRepository = CoinWalletRepository(
            walletDao = database.coinWalletDao(),
            transactionDao = database.coinTransactionDao()
        )

        val streakRepository = StreakRepository(
            streakDao = database.streakTrackerDao()
        )

        val focusRepository = FocusRepository(
            focusDao = database.focusDao(),
            scheduler = com.focus.digitalwellbeing.service.FocusScheduler(context.applicationContext),
            context = context.applicationContext
        )

        val billingManager = com.focus.digitalwellbeing.data.billing.BillingManager(
            context = context.applicationContext,
            coinWalletRepository = coinWalletRepository
        )

        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(
                    context = context.applicationContext,
                    usageRepository = usageRepository,
                    challengeRepository = challengeRepository,
                    taskRepository = taskRepository,
                    appLimitRepository = appLimitRepository,
                    appPreferencesRepository = appPreferencesRepository,
                    coinWalletRepository = coinWalletRepository,
                    streakRepository = streakRepository,
                    focusRepository = focusRepository,
                    billingManager = billingManager
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

