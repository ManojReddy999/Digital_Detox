package com.focus.digitalwellbeing.service

import android.content.Context
import android.content.Intent

/**
 * Manages the blocking overlay by launching BlockingDialogActivity.
 * Simplified to avoid crashes with complex Compose overlays.
 */
class OverlayManager(private val context: Context) {

    private var isShowing = false

    fun showOverlay(packageName: String, appName: String, limitFormatted: String) {
        if (isShowing) {
            android.util.Log.d("OverlayManager", "Overlay already showing")
            return
        }

        android.util.Log.d("OverlayManager", "Showing blocking overlay on top of $appName")

        try {
            // Show the blocking dialog on top of the current app
            val blockingIntent = Intent(context, BlockingDialogActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                putExtra(BlockingDialogActivity.EXTRA_PACKAGE_NAME, packageName)
                putExtra(BlockingDialogActivity.EXTRA_APP_NAME, appName)
                putExtra(BlockingDialogActivity.EXTRA_LIMIT_FORMATTED, limitFormatted)
            }
            context.startActivity(blockingIntent)
            isShowing = true

            android.util.Log.d("OverlayManager", "Blocking overlay launched successfully")
        } catch (e: Exception) {
            android.util.Log.e("OverlayManager", "Error showing blocking overlay", e)
            e.printStackTrace()
            isShowing = false
        }
    }

    fun dismiss() {
        isShowing = false
        android.util.Log.d("OverlayManager", "Dismissed")
    }

    fun isShowing(): Boolean = isShowing
}

