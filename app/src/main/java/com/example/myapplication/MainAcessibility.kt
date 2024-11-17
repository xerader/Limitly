package com.example.myapplication

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class MyAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Configure service info
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        serviceInfo = info

        Log.d("AccessibilityService", "Service connected and ready.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val packageName = it.packageName.toString()
                Log.d("ForegroundApp", "Current app in foreground: $packageName")
                if (packageName == "com.google.android.youtube") {
                    Log.d("AccessibilityCheck", "YouTube app detected!")
                    // You can add additional actions like notifications
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d("AccessibilityService", "Service interrupted.")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Log.d("AccessibilityService", "Service unbound.")
        return super.onUnbind(intent)
    }
}