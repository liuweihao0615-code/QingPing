package com.qingping.adblock

import android.content.Context
import java.util.Locale

/**
 * Local-only preferences for candidate detection, keyword rules, ignored packages and counters.
 * Counts represent suspected-ad detections, not confirmed ad blocks.
 */
object QingPingStore {
    private const val PREFS_NAME = "qingping_settings"
    private const val KEY_ENABLED = "monitor_enabled"
    private const val KEY_KEYWORDS = "keywords"
    private const val KEY_IGNORED_PACKAGES = "ignored_packages"
    private const val KEY_TOTAL = "total_candidates"
    private const val COUNT_PREFIX = "count:"

    private val defaultKeywords = setOf("广告", "sponsored", "advertisement", "跳过", "skip")

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun keywords(context: Context): List<String> =
        (prefs(context).getStringSet(KEY_KEYWORDS, defaultKeywords)?.toSet() ?: defaultKeywords)
            .filter { it.isNotBlank() }
            .sorted()

    fun addKeyword(context: Context, value: String): Boolean {
        val keyword = value.trim().lowercase(Locale.ROOT)
        if (keyword.isBlank()) return false
        val items = keywords(context).toMutableSet()
        if (!items.add(keyword)) return false
        prefs(context).edit().putStringSet(KEY_KEYWORDS, items).apply()
        return true
    }

    fun removeKeyword(context: Context, value: String) {
        val items = keywords(context).toMutableSet()
        items.remove(value)
        prefs(context).edit().putStringSet(KEY_KEYWORDS, items).apply()
    }

    fun ignoredPackages(context: Context): List<String> =
        (prefs(context).getStringSet(KEY_IGNORED_PACKAGES, emptySet())?.toSet() ?: emptySet())
            .filter { it.isNotBlank() }
            .sorted()

    fun addIgnoredPackage(context: Context, value: String): Boolean {
        val packageName = value.trim()
        if (packageName.isBlank() || !packageName.contains('.')) return false
        val items = ignoredPackages(context).toMutableSet()
        if (!items.add(packageName)) return false
        prefs(context).edit().putStringSet(KEY_IGNORED_PACKAGES, items).apply()
        return true
    }

    fun removeIgnoredPackage(context: Context, value: String) {
        val items = ignoredPackages(context).toMutableSet()
        items.remove(value)
        prefs(context).edit().putStringSet(KEY_IGNORED_PACKAGES, items).apply()
    }

    /** Increment a local counter for a suspected-ad detection event. */
    fun recordCandidate(context: Context, packageName: String): Long {
        val p = prefs(context)
        val key = COUNT_PREFIX + packageName
        val next = p.getLong(key, 0L) + 1L
        p.edit()
            .putLong(key, next)
            .putLong(KEY_TOTAL, p.getLong(KEY_TOTAL, 0L) + 1L)
            .apply()
        return next
    }

    fun counts(context: Context): Map<String, Long> =
        prefs(context).all
            .filterKeys { it.startsWith(COUNT_PREFIX) }
            .mapNotNull { (key, value) ->
                val count = value as? Long ?: return@mapNotNull null
                key.removePrefix(COUNT_PREFIX).takeIf { it.isNotBlank() }?.let { it to count }
            }
            .toMap()
            .toList()
            .sortedByDescending { it.second }
            .toMap()

    fun totalCandidates(context: Context): Long =
        prefs(context).getLong(KEY_TOTAL, 0L)

    fun clearCounts(context: Context) {
        val editor = prefs(context).edit().remove(KEY_TOTAL)
        prefs(context).all.keys.filter { it.startsWith(COUNT_PREFIX) }.forEach(editor::remove)
        editor.apply()
    }
}
