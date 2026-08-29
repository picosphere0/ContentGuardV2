package com.parsa.contentguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Nudge the watchdog awake; it re-prompts for VPN/Accessibility
            // re-enable if either got stripped (some OEMs reset these on reboot).
            val watchdogIntent = Intent(context, WatchdogService::class.java)
            ContextCompat.startForegroundService(context, watchdogIntent)
        }
    }
}
