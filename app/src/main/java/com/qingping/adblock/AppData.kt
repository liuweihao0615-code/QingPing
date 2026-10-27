package com.qingping.adblock

import android.content.Context

/**
 * Small local-only store for QingPing settings and candidate-event statistics.
 * Data stays on this device in SharedPreferences.
 */
object AppData {
    private const val PREFS = "qingping_local"
    private const val KEY_ENABLED = "detection_enabled"
    private const val KEY_RULES = "keyword_rules"
    private const val KEY_IGNORED = "ignored_packages"
    private const val STAT_PREFIX = "candidate_count_"

    private val defaultRules = setOf("广告", "sponsored", "advertisement", "跳过", "skip")

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isDetectionEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setDetectionEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getRules(context: Context): Set<String> {
        val stored = prefs(context).getStringSet(KEY_RULES, null)
        return (stored ?: defaultRules).map { it.trim() }
            .filter { it.isNotEmpty() }.toSet()
    }

    fun addRule(context: Context, rule: String): Boolean {
        val clean = rule.trim()
        if (clean.isEmpty()) return false
        val rules = getRules(context).toMutableSet()
        val added = rules.add(clean)
        if (added) prefs(context).edit().putStringSet(KEY_RULES, rules).apply()
        return added
    }

    fun removeRule(context: Context, rule: String) {
        val rules = getRules(context).toMutableSet()
        rules.remove(rule)
        prefs(context).edit().putStringSet(KEY_RULES, rules).apply()
    }

    fun getIgnoredPackages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_IGNORED, emptySet())?.toSet() ?: emptySet()

    fun addIgnoredPackage(context: Context, packageName: String): Boolean {
        val clean = packageName.trim()
        if (clean.isEmpty() || !clean.contains('.')) return false
        val ignored = getIgnoredPackages(context).toMutableSet()
        val added = ignored.add(clean)
        if (added) prefs(context).edit().putStringSet(KEY_IGNORED, ignored).apply()
        return added
    }

    fun removeIgnoredPackage(context: Context, packageName: String) {
        val ignored = getIgnoredPackages(context).toMutableSet()
        ignored.remove(packageName)
        prefs(context).edit().putStringSet(KEY_IGNORED, ignored).apply()
    }

    fun isPackageIgnored(context: Context, packageName: String): Boolean =
        packageName in getIgnoredPackages(context)

    fun recordCandidate(context: Context, packageName: String) {
        if (packageName.isBlank()) return
        val p = prefs(context)
        val key = STAT_PREFIX + packageName
        p.edit().putInt(key, p.getInt(key, 0) + 1).apply()
    }

    fun getStatistics(context: Context): Map<String, Int> {
        return prefs(context).all
            .filterKeys { it.startsWith(STAT_PREFIX) }
            .mapNotNull { (key, value) ->
                val count = value as? Int ?: return@mapNotNull null
                key.removePrefix(STAT_PREFIX) to count
            }.toMap()
    }

    fun clearStatistics(context: Context) {
        val editor = prefs(context).edit()
        prefs(context).all.keys
            .filter { it.startsWith(STAT_PREFIX) }
            .forEach { editor.remove(it) }
        editor.apply()
    }
}
