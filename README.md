# 清屏（QingPing）Android 原型

这是可在 Android Studio 中打开并构建的原生 Android 项目源代码，不是已经编译好的 APK。

## 当前包含
- 清屏基础启动界面
- 打开 Android 无障碍设置的入口
- 无障碍服务事件监听
- 对可访问文本进行简单的疑似广告线索检测
- 检测到线索时显示轻提示

## 当前不包含
- 真正可靠的广告识别规则
- 自动关闭广告或自动返回
- Room/SQLite 持久化统计
- 开机恢复/前台服务
- 对各品牌手机和第三方 APP 的兼容性验证

## 构建 APK
1. 在电脑安装 Android Studio。
2. 在 Android Studio 中打开仓库根目录。
3. 等待 Gradle 同步并安装 Android SDK 35（若 Android Studio 提示缺少组件）。
4. 选择 `Build > Build Bundle(s) / APK(s) > Build APK(s)`。
5. APK 通常会生成在 `app/build/outputs/apk/debug/app-debug.apk`。
6. 将 APK 传到手机，打开文件并按系统提示允许“安装未知应用”。

## 手机启用
安装后打开“清屏”，点击“打开无障碍设置”，找到“清屏广告识别服务”并由用户手动启用。

## 重要说明
此版本仅为功能原型。无障碍服务能读取的界面内容因应用而异；Toast 提示不等于成功拦截。请勿把本版本视为能屏蔽所有广告的成品。

## 云端构建 APK（无需本地安装 Android Studio）
1. 打开仓库的 **Actions** 页面，进入 **Build QingPing APK** 工作流。
2. 等待构建完成，打开该次运行记录，在 **Artifacts** 中下载 `QingPing-debug-apk`。
3. 解压 artifact，取得 `app-debug.apk` 后传到手机安装。

注意：这会构建当前原型 APK，不会自动补齐尚未实现的广告拦截、统计数据库和开机恢复功能。
