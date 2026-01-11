package com.focus.digitalwellbeing.util

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
        var isEnabled = false
        try {
            // Method 1: Check using AccessibilityManager (More reliable for checking if service is actually bound)
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            
            val myPackageName = context.packageName
            val myServiceClass = "$myPackageName.service.AppBlockingAccessibilityService"
            
            for (service in enabledServices) {
                val serviceId = service.id
                val componentName = android.content.ComponentName.unflattenFromString(serviceId)
                if (componentName != null && componentName.packageName == myPackageName) {
                    isEnabled = true
                    break
                }
            }
            
            // Method 2: Fallback to Settings Secure (Checks if the switch is toggled on)
            if (!isEnabled) {
                val enabledServicesString = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                val serviceClass = "$myPackageName.service.AppBlockingAccessibilityService"
                isEnabled = enabledServicesString?.contains(serviceClass) == true
            }
            
            android.util.Log.d("AccessibilityUtils", "isAccessibilityServiceEnabled: $isEnabled")
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

