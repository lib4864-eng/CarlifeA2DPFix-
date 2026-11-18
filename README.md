# CarLife Audio Route (LSPosed 模块)

这是一个专为百度 CarLife (含三星定制版) 开发的 LSPosed 模块。旨在解决 CarLife 连接车机时抢占音频焦点、导致手机后台音乐被打断或声音输出通道错误的问题。

## ✨ 功能特性
- **屏蔽音频焦点抢占**：拦截 CarLife 的 `requestAudioFocus`，导航播报不再打断后台音乐。
- **强制 A2DP 路由**：Hook `AudioTrack`，强制将 CarLife 的音频流路由至系统当前的蓝牙 A2DP 设备。
- **底层兜底拦截**：拦截 CarLife 试图通过反射调用的私有方法 `BluetoothA2dp.connectSink`。

## 📱 适配说明
- **适配系统**：Android 12+ (理论支持 Android 16 / OneUI)
- **作用域**：
  - `com.baidu.carlife` (通用版)
  - `com.baidu.carlifeplus` (CarLife+)
  - `com.baidu.carlife.samsung` (三星定制版)

## 🛠️ 构建方法

### 方法一：通过 GitHub Actions 自动构建（推荐）
1. Fork 本仓库或 Push 代码到你的仓库。
2. 点击仓库顶部的 **Actions** 标签页。
3. 选择左侧的 **Build LSPosed Module**，点击右侧的 **Run workflow**。
4. 构建完成后，在运行记录的底部下载 `CarLifeAudioRoute-Release` Artifacts，解压即可获得 APK。

### 方法二：本地 Android Studio 构建
1. 使用 Android Studio 打开本项目。
2. 确保 Gradle JDK 为 **JDK 17**。
3. 点击菜单栏 `Build` -> `Generate App Bundles or APKs` -> `Generate APK(s)`。

## ⚠️ 局限性
- 仅限 **Java 层 Hook**。如果 CarLife 将音频输出下沉到 Native (AAudio) 或蓝牙 HAL 层，本模块将无法拦截。
- 不同车机的音频策略 (AudioPolicy) 不同，极少数车型可能仍需结合车机端的设置。

## 📜 开源协议
MIT License
