package com.parsa.contentguard.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.parsa.contentguard.util.KeywordStore

/**
 * Scans visible on-screen text (window content, not keystrokes) for keywords
 * configured in KeywordStore. On a match it backs out to the home screen.
 * This catches content DNS blocking never covered anyway - search result
 * snippets, cached pages, in-app browsers, thumbnails' alt text, etc.
 *
 * Everything runs on-device. Nothing is transmitted anywhere.
 */
class ContentScanAccessibilityService : AccessibilityService() {

    private val keywords: Set<String> by lazy { KeywordStore.load(applicationContext) }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val root = rootInActiveWindow ?: return
        try {
            if (containsBlockedText(root)) {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        } catch (e: Exception) {
            Log.w("ContentScan", "scan failed", e)
        } finally {
            root.recycle()
        }
    }

    private fun containsBlockedText(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        if (depth > 40) return false // guard against pathological view trees
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        if (!text.isNullOrEmpty() && matchesKeyword(text)) return true
        if (!desc.isNullOrEmpty() && matchesKeyword(desc)) return true

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val hit = containsBlockedText(child, depth + 1)
            child.recycle()
            if (hit) return true
        }
        return false
    }

    private fun matchesKeyword(text: String): Boolean {
        val lower = text.lowercase()
        return keywords.any { lower.contains(it) }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("ContentScan", "Accessibility scanning active")
    }
}
