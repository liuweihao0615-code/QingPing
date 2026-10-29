package com.qingping.adblock

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class AdGuardAccessibilityService : AccessibilityService() {
    private val lastFingerprintByPackage = mutableMapOf<String, String>()
    private val lastRecordedAtByPackage = mutableMapOf<String, Long>()
    private var lastToastAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (!AppData.isDetectionEnabled(this)) return

        val sourcePackage = event.packageName?.toString() ?: return
        if (sourcePackage == packageName || AppData.isPackageIgnored(this, sourcePackage)) return

        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val root = rootInActiveWindow ?: return
        val text = buildString { collectText(root, this) }
            .lowercase()
            .take(2500)
        if (text.isBlank()) return

        val matchingRule = AppData.getRules(this)
            .firstOrNull { text.contains(it.lowercase()) } ?: return

        // Only click an explicit, accessible skip/ad-dismiss button. Never click ad content
        // or blindly issue BACK, which could close the user's current app or page.
        if (tryClickSafeDismissButton(root)) {
            AppData.recordDismissAttemptSucceeded(this, sourcePackage)
            Toast.makeText(
                this,
                "清屏：已触发 ${sourcePackage.substringAfterLast('.')} 的跳过按钮点击",
                Toast.LENGTH_SHORT
            ).show()
        }

        val fingerprint = text.hashCode().toString()
        if (lastFingerprintByPackage[sourcePackage] == fingerprint) return
        lastFingerprintByPackage[sourcePackage] = fingerprint

        // Count candidate detections, not guaranteed ad blocks.
        val now = System.currentTimeMillis()
        val previous = lastRecordedAtByPackage[sourcePackage] ?: 0L
        if (now - previous < 10_000L) return

        lastRecordedAtByPackage[sourcePackage] = now
        AppData.recordCandidate(this, sourcePackage)

        if (now - lastToastAt > 10_000L) {
            lastToastAt = now
            Toast.makeText(
                this,
                "清屏：${sourcePackage.substringAfterLast('.')} 出现疑似广告线索（规则：${matchingRule}）",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun tryClickSafeDismissButton(node: AccessibilityNodeInfo): Boolean {
        val label = listOfNotNull(
            node.text?.toString(),
            node.contentDescription?.toString()
        ).joinToString(" ").trim().lowercase()

        if (node.isClickable && isSafeDismissLabel(label)) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val clicked = tryClickSafeDismissButton(child)
            child.recycle()
            if (clicked) return true
        }
        return false
    }

    private fun isSafeDismissLabel(label: String): Boolean {
        val normalized = label.replace(Regex("\\s+"), "")
        if (normalized in setOf("跳过", "跳过广告", "关闭广告", "关闭广告页")) return true
        if (normalized.startsWith("跳过") && normalized.length <= 10) return true
        if (normalized in setOf("skip", "skipad", "closead", "closeadvertisement")) return true
        if (normalized.startsWith("skip") && normalized.length <= 16) return true
        return false
    }

    private fun collectText(node: AccessibilityNodeInfo, out: StringBuilder) {
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let {
            out.append(it).append(' ')
        }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let {
            out.append(it).append(' ')
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectText(child, out)
            child.recycle()
        }
    }

    override fun onInterrupt() = Unit
}
