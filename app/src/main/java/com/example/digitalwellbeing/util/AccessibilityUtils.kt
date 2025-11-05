package com.example.digitalwellbeing.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

object AccessibilityUtils {

    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        try {
            val enabledServicesString = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )

            val packageName = context.packageName
            val serviceClass = "$packageName.service.AppBlockingAccessibilityService"

            // Debug logging
            android.util.Log.d("AccessibilityUtils", "Looking for service: $serviceClass")
            android.util.Log.d("AccessibilityUtils", "Enabled services string: $enabledServicesString")

            val isEnabled = enabledServicesString?.contains(serviceClass) == true
            android.util.Log.d("AccessibilityUtils", "Is enabled: $isEnabled")

            return isEnabled
        } catch (e: Exception) {
            android.util.Log.e("AccessibilityUtils", "Error checking accessibility service", e)
            return false
        }
    }

    /**
     * Open accessibility settings
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
