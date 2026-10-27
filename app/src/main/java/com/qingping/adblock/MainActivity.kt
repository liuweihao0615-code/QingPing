package com.qingping.adblock

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 48, 32, 32)
        }

        val title = TextView(this).apply {
            text = "清屏"
            textSize = 32f
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "开屏广告识别原型"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 24)
        }
        val explanation = TextView(this).apply {
            text = "请手动启用无障碍服务。当前版本只检测部分可访问文本中的疑似广告线索，不会自动点击或返回，也不保证拦截所有广告。"
            textSize = 16f
            setPadding(0, 0, 0, 24)
        }
        val settingsButton = Button(this).apply {
            text = "打开无障碍设置"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        val status = TextView(this).apply {
            text = "统计与规则管理将在后续版本接入。"
            textSize = 14f
            setPadding(0, 24, 0, 0)
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(explanation)
        layout.addView(settingsButton)
        layout.addView(status)
        setContentView(layout)
    }
}
