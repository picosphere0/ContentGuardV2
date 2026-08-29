package com.parsa.contentguard.util

import android.content.Context

/**
 * Stores the user's own keyword list in a private SharedPreferences set,
 * edited from MainActivity. Kept separate from the DNS blocklist (which is
 * a maintained domain list) since keywords are personal and freeform.
 */
object KeywordStore {
    private const val PREFS = "contentguard_keywords"
    private const val KEY = "keywords"

    // Sensible defaults - user can add/remove from the main screen.
    private val DEFAULTS = setOf(
        "porn", "xxx", "nsfw", "onlyfans", "xvideos", "xnxx", "pornhub"
    )

    fun load(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getStringSet(KEY, null)
        return stored ?: DEFAULTS
    }

    fun save(context: Context, keywords: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY, keywords)
            .apply()
    }

    fun add(context: Context, keyword: String) {
        val current = load(context).toMutableSet()
        current.add(keyword.trim().lowercase())
        save(context, current)
    }

    fun remove(context: Context, keyword: String) {
        val current = load(context).toMutableSet()
        current.remove(keyword.trim().lowercase())
        save(context, current)
    }
}
