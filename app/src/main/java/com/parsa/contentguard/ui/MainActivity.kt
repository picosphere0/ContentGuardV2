package com.parsa.contentguard.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.parsa.contentguard.admin.GuardDeviceAdminReceiver
import com.parsa.contentguard.service.WatchdogService
import com.parsa.contentguard.util.KeywordStore

class MainActivity : AppCompatActivity() {

    private val adminRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshStatus() }

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        ContextCompat.startForegroundService(this, Intent(this, WatchdogService::class.java))
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun buildLayout(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        statusText = TextView(this)
        root.addView(statusText)

        root.addView(Button(this).apply {
            text = "1. Enable Device Admin (uninstall protection)"
            setOnClickListener { requestDeviceAdmin() }
        })

        root.addView(Button(this).apply {
            text = "2. Enable Accessibility Service (content scanning)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        root.addView(Button(this).apply {
            text = "Edit blocked keywords"
            setOnClickListener { showKeywordEditor() }
        })

        root.addView(Button(this).apply {
            text = "Disable protection (verse + confession required)"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, DisableGateActivity::class.java))
            }
        })

        root.addView(TextView(this).apply {
            text = "\nNote: Accessibility can still be turned off directly from " +
                "system Settings without going through the gate above - Android " +
                "doesn't let any app block its own Settings page. The button here " +
                "gates uninstalling, and gates a deliberate 'I'm turning this off' " +
                "moment - it can't override what system Settings itself allows."
        })

        return root
    }

    private fun refreshStatus() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, GuardDeviceAdminReceiver::class.java)
        val adminActive = dpm.isAdminActive(admin)
        statusText.text = "Device Admin: ${if (adminActive) "ON" else "off"}\n" +
            "(Check Settings > Accessibility to confirm scanning is on)"
    }

    private fun requestDeviceAdmin() {
        val admin = ComponentName(this, GuardDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "This makes ContentGuard require deactivation before it can be uninstalled."
            )
        }
        adminRequestLauncher.launch(intent)
    }

    private fun showKeywordEditor() {
        val current = KeywordStore.load(this).joinToString(", ")
        val input = EditText(this).apply { setText(current) }
        android.app.AlertDialog.Builder(this)
            .setTitle("Blocked keywords (comma-separated)")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val set = input.text.toString().split(",").map { it.trim() }
                    .filter { it.isNotEmpty() }.toSet()
                KeywordStore.save(this, set)
                Toast.makeText(this, "Keywords updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
