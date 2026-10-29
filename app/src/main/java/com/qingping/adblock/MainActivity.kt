package com.qingping.adblock

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private var awaitingAccessibilityAuthorization = false

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (awaitingAccessibilityAuthorization) {
            awaitingAccessibilityAuthorization = false
            if (!isAccessibilityServiceEnabled()) {
                AppData.setDetectionEnabled(this, false)
                Toast.makeText(this, "未授权无障碍服务，清屏将退出", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            Toast.makeText(this, "无障碍服务已授权，自动识别已就绪", Toast.LENGTH_SHORT).show()
        }
        if (::content.isInitialized) render()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expected = ComponentName(this, AdGuardAccessibilityService::class.java)
            .flattenToString()
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun openAccessibilitySettings() {
        awaitingAccessibilityAuthorization = true
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun render() {
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }
        scroll.addView(content)

        content.addView(TextView(this).apply {
            text = "清屏"
            textSize = 30f
            gravity = Gravity.CENTER
        })
        content.addView(TextView(this).apply {
            text = "开屏广告识别与安全跳过 · 本地规则与统计"
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(18))
        })

        val enabled = CheckBox(this).apply {
            text = "自动清屏（疑似广告识别）"
            textSize = 17f
            isChecked = AppData.isDetectionEnabled(this@MainActivity)
            setOnCheckedChangeListener { _, checked ->
                AppData.setDetectionEnabled(this@MainActivity, checked)
                if (!checked) {
                    Toast.makeText(this@MainActivity, "已暂停识别", Toast.LENGTH_SHORT).show()
                } else if (isAccessibilityServiceEnabled()) {
                    Toast.makeText(this@MainActivity, "自动识别已启用", Toast.LENGTH_SHORT).show()
                } else {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("需要无障碍授权")
                        .setMessage("自动清屏需要无障碍服务权限。请选择前往系统设置授权；如果不授权，清屏应用将自动退出。Android 不允许应用代替用户开启此权限。")
                        .setCancelable(false)
                        .setNegativeButton("不授权，退出") { _, _ ->
                            AppData.setDetectionEnabled(this@MainActivity, false)
                            finish()
                        }
                        .setPositiveButton("前往授权") { _, _ ->
                            openAccessibilitySettings()
                        }
                        .show()
                }
            }
        }
        content.addView(enabled)
        content.addView(TextView(this).apply {
            text = if (serviceEnabled()) {
                "无障碍服务：已开启。当前只识别并记录疑似广告线索，不会自动点击或返回。"
            } else {
                "无障碍服务：未开启。启用自动清屏后需在系统设置中手动授权；拒绝授权将退出应用。"
            }
            textSize = 14f
            setPadding(0, dp(4), 0, dp(12))
        })
        content.addView(button("打开无障碍设置") {
            if (isAccessibilityServiceEnabled()) {
                openAccessibilitySettings()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("需要无障碍授权")
                    .setMessage("请在系统设置中手动开启“清屏广告识别服务”。如果返回时仍未授权，清屏将自动退出。")
                    .setNegativeButton("取消并退出") { _, _ -> finish() }
                    .setPositiveButton("前往授权") { _, _ -> openAccessibilitySettings() }
                    .show()
            }
        })

        addSection("识别规则（关键词）")
        content.addView(TextView(this).apply {
            text = "界面可访问文本包含下列任一关键词时，可能记为疑似广告事件。规则保存在本机。"
            textSize = 13f
        })
        val ruleInput = EditText(this).apply {
            hint = "输入关键词，例如：广告、跳过"
            setSingleLine(true)
        }
        content.addView(ruleInput)
        content.addView(button("添加关键词") {
            val rule = ruleInput.text.toString().trim()
            if (rule.isEmpty()) {
                Toast.makeText(this, "请先输入关键词", Toast.LENGTH_SHORT).show()
            } else if (AppData.addRule(this, rule)) {
                Toast.makeText(this, "已添加规则", Toast.LENGTH_SHORT).show()
                render()
            } else {
                Toast.makeText(this, "规则已存在", Toast.LENGTH_SHORT).show()
            }
        })
        AppData.getRules(this).sorted().forEach { rule ->
            content.addView(itemRow(rule, "删除") {
                AppData.removeRule(this, rule)
                render()
            })
        }

        addSection("忽略的应用")
        content.addView(TextView(this).apply {
            text = "填写应用包名后，该应用不会参与识别或计数。例如：com.example.app。"
            textSize = 13f
        })
        val packageInput = EditText(this).apply {
            hint = "应用包名，例如 com.example.app"
            setSingleLine(true)
        }
        content.addView(packageInput)
        content.addView(button("添加忽略应用") {
            val pkg = packageInput.text.toString().trim()
            if (AppData.addIgnoredPackage(this, pkg)) {
                Toast.makeText(this, "已加入忽略列表", Toast.LENGTH_SHORT).show()
                render()
            } else {
                Toast.makeText(this, "包名格式不正确或已存在", Toast.LENGTH_SHORT).show()
            }
        })
        val ignored = AppData.getIgnoredPackages(this).sorted()
        if (ignored.isEmpty()) {
            content.addView(note("暂无忽略应用"))
        } else {
            ignored.forEach { pkg ->
                content.addView(itemRow(pkg, "移除") {
                    AppData.removeIgnoredPackage(this, pkg)
                    render()
                })
            }
        }

        addSection("疑似广告统计")
        content.addView(TextView(this).apply {
            text = "按应用统计疑似广告线索，以及成功触发无障碍点击的跳过按钮次数。点击次数不保证广告最终关闭。统计只保存在本机。"
            textSize = 13f
        })
        val stats = AppData.getStatistics(this).toList().sortedByDescending { it.second }
        val dismissStats = AppData.getDismissStatistics(this)
        val dismissTotal = dismissStats.values.sum()
        val total = stats.sumOf { it.second }
        content.addView(TextView(this).apply {
            text = "累计疑似事件：$total\n成功触发跳过按钮点击：$dismissTotal"
            textSize = 19f
            setPadding(0, dp(8), 0, dp(8))
        })
        if (stats.isEmpty()) {
            content.addView(note("暂无记录。启用识别服务后，出现匹配规则的界面时会尝试记录。"))
        } else {
            stats.forEach { (pkg, count) ->
                val label = try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(pkg, 0)
                    ).toString()
                } catch (_: Exception) {
                    pkg
                }
                content.addView(TextView(this).apply {
                    text = "$label（$pkg）：$count 次疑似线索；${dismissStats[pkg] ?: 0} 次按钮点击成功"
                    textSize = 15f
                    setPadding(0, dp(5), 0, dp(5))
                })
            }
        }
        content.addView(button("清空统计") {
            AlertDialog.Builder(this)
                .setTitle("清空统计")
                .setMessage("确定删除本机保存的全部疑似广告统计吗？此操作无法撤销。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清空") { _, _ ->
                    AppData.clearStatistics(this)
                    render()
                }
                .show()
        })

        setContentView(scroll)
    }

    private fun serviceEnabled(): Boolean = isAccessibilityServiceEnabled()

    private fun addSection(title: String) {
        content.addView(View(this).apply {
            setBackgroundColor(0xFFE0E0E0.toInt())
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
        ).apply {
            topMargin = dp(18)
            bottomMargin = dp(12)
        })
        content.addView(TextView(this).apply {
            text = title
            textSize = 20f
            setPadding(0, 0, 0, dp(8))
        })
    }

    private fun note(textValue: String) = TextView(this).apply {
        text = textValue
        textSize = 14f
        setPadding(0, dp(6), 0, dp(8))
    }

    private fun button(label: String, action: () -> Unit) =
        Button(this).apply {
            text = label
            setOnClickListener { action() }
        }

    private fun itemRow(value: String, actionLabel: String, action: () -> Unit) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val text = TextView(this@MainActivity).apply {
                this.text = value
                textSize = 14f
                setPadding(0, dp(4), dp(8), dp(4))
            }
            addView(text, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(button(actionLabel, action))
        }
}
