package com.rajk2007.aurawall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LockScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("aurawall_prefs", Context.MODE_PRIVATE)
        val lockEnabled = prefs.getBoolean("lock_screen_enabled", false)
        if (!lockEnabled) return

        when (intent.action) {
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_BOOT_COMPLETED -> {
                context.startForegroundService(
                    Intent(context, LockScreenService::class.java)
                )
            }
        }
    }
}
