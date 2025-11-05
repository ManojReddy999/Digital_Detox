package com.example.digitalwellbeing.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.digitalwellbeing.R

/**
 * Service that shows an overlay dialog when an app timer limit is exceeded
 */
class TimerOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: "This app"
        val limitFormatted = intent?.getStringExtra(EXTRA_LIMIT_FORMATTED) ?: "time limit"

        showOverlay(appName, limitFormatted)
        return START_NOT_STICKY
    }

    private fun showOverlay(appName: String, limitFormatted: String) {
        if (overlayView != null) {
            // Already showing, just update text
            updateOverlayText(appName, limitFormatted)
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Inflate the overlay layout
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.timer_overlay_layout, null)

        // Configure layout parameters for overlay
        val layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
            format = PixelFormat.TRANSLUCENT
            // Make it focusable so the button can be clicked
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
        }

        try {
            windowManager?.addView(overlayView, layoutParams)
            setupOverlayContent(appName, limitFormatted)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun setupOverlayContent(appName: String, limitFormatted: String) {
        overlayView?.let { view ->
            // Update text views
            val titleText = view.findViewById<TextView>(R.id.overlayTitleText)
            val messageText = view.findViewById<TextView>(R.id.overlayMessageText)
            val okButton = view.findViewById<Button>(R.id.overlayOkButton)

            titleText?.text = "Time's Up!"
            messageText?.text = "You've reached your $limitFormatted limit for $appName.\n\nTake a break and come back later. Your timer will reset at midnight."

            // Set up OK button click listener
            okButton?.setOnClickListener {
                // Go to home screen
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)

                // Dismiss overlay
                dismissOverlay()
            }
        }
    }

    private fun updateOverlayText(appName: String, limitFormatted: String) {
        overlayView?.let { view ->
            val titleText = view.findViewById<TextView>(R.id.overlayTitleText)
            val messageText = view.findViewById<TextView>(R.id.overlayMessageText)

            titleText?.text = "Time's Up!"
            messageText?.text = "You've reached your $limitFormatted limit for $appName.\n\nTake a break and come back later. Your timer will reset at midnight."
        }
    }

    private fun dismissOverlay() {
        overlayView?.let { view ->
            windowManager?.removeView(view)
            overlayView = null
        }
        stopSelf()
    }

    override fun onDestroy() {
        dismissOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_LIMIT_FORMATTED = "extra_limit_formatted"
    }
}
