package com.parsa.contentguard.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Once this is enabled by the user (Settings > Security > Device Admin Apps),
 * Android requires it to be deactivated here before the app can be uninstalled.
 * There is no way to remove that "Deactivate" step from the OS side - that's
 * the actual friction. We just make deactivation itself go through a gate
 * (see DisableGateActivity) instead of being one tap.
 */
class GuardDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // Shown by the OS in its own confirmation dialog before deactivation.
        // This is the one hook the OS gives us here - the real gate (cooldown,
        // reflection, passphrase) lives in DisableGateActivity, launched from
        // MainActivity's "Disable protection" button, not from this system dialog.
        return "Deactivating disables all content blocking immediately. " +
            "Use the in-app disable flow instead if you want the accountability steps."
    }
}
