package com.example

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat

class LockScreenAccessibilityService : AccessibilityService() {

    private val lockScreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.ACTION_LOCK_SCREEN") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        ContextCompat.registerReceiver(
            this,
            lockScreenReceiver,
            IntentFilter("com.example.ACTION_LOCK_SCREEN"),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            val intent = Intent("com.example.ACTION_FOREGROUND_APP_CHANGED")
            intent.putExtra("packageName", packageName)
            intent.setPackage(applicationContext.packageName)
            sendBroadcast(intent)
        }
    }

    override fun onInterrupt() {
        // Required, but we don't do anything
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(lockScreenReceiver)
    }
}
