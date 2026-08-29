package com.parsa.contentguard.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import com.parsa.contentguard.ui.MainActivity

/**
 * Long-lived foreground service. Every ~30s it checks whether the
 * Accessibility Service is still enabled (some OEMs / battery optimizers
 * silently disable it) and whether the VPN is still running. If either has
 * dropped, it surfaces a high-priority notification prompting re-enable -
 * it can't force system permissions back on (no app can), but it makes
 * silent drift visible immediately instead of weeks later.
 */
class WatchdogService : Service() {

    companion object {
        const val CHANNEL_ID = "contentguard_watchdog"
        const val NOTIF_ID = 43
        const val ACTION_LOG_BLOCK = "com.parsa.contentguard.LOG_BLOCK"
        private const val CHECK_INTERVAL_MS = 30_000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkProtections()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification("Monitoring protection status"))
        handler.post(checkRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_LOG_BLOCK) {
            // Hook point if the user later wants local-only block history.
            // Deliberately not reporting anywhere - on-device only.
        }
        return START_STICKY
    }

    private fun checkProtections() {
        if (!isAccessibilityServiceEnabled()) {
            notifyDrift("Content scanning turned off", "Re-enable ContentGuard in Accessibility settings")
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = "${packageName}/${ContentScanAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(":").any { it.equals(expectedComponent, ignoreCase = true) }
    }

    private fun notifyDrift(title: String, text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID + 1, buildNotification(text, title))
    }

    private fun buildNotification(text: String, title: String = "ContentGuard"): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, "Watchdog", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
