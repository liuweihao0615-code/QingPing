package com.qingping.adblock

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class AdGuardAccessibilityService : AccessibilityService() {
    private var lastFingerprint = ""
    private var lastToastAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.packageName?.toString() == packageName) return

        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val root = rootInActiveWindow ?: return
        val text = buildString {
            collectText(root, this)
        }.lowercase().take(1500)

        if (text.isBlank() || text == lastFingerprint) return
        lastFingerprint = text

        val adHint = listOf("广告", "sponsored", "advertisement", "跳过", "skip")
            .any { text.contains(it) }

        // 原型仅展示候选提示，不自动执行系统返回或点击，避免误操作。
        val now = System.currentTimeMillis()
        if (adHint && now - lastToastAt > 10_000L) {
            lastToastAt = now
            Toast.makeText(this, "清屏：发现疑似广告线索", Toast.LENGTH_SHORT).show()
        }
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
