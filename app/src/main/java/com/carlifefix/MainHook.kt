package com.carlifefix

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 同时兼容原版和Plus版
        val targetPkgs = setOf("com.baidu.carlife", "com.baidu.carlifeplus")
        if (lpparam.packageName !in targetPkgs) return

        try {
            val audioManagerClass = XposedHelpers.findClass("android.media.AudioManager", lpparam.classLoader)

            // 老版本 requestAudioFocus(AudioManager.OnAudioFocusChangeListener, int, int)
            XposedHelpers.findAndHookMethod(
                audioManagerClass,
                "requestAudioFocus",
                "android.media.AudioManager\$OnAudioFocusChangeListener",
                Int::class.java,
                Int::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = -1
                        XposedBridge.log("Block old requestAudioFocus")
                    }
                }
            )

            // Android O+ 新版：requestAudioFocus(AudioFocusRequest)
            XposedHelpers.findAndHookMethod(
                audioManagerClass,
                "requestAudioFocus",
                "android.media.AudioFocusRequest",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = -1
                        XposedBridge.log("Block new AudioFocusRequest")
                    }
                }
            )

            // abandonAudioFocus 放行
            XposedHelpers.findAndHookMethod(
                audioManagerClass,
                "abandonAudioFocus",
                "android.media.AudioManager\$OnAudioFocusChangeListener",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        super.beforeHookedMethod(param)
                    }
                }
            )

            // 新版 abandonAudioFocus(AudioFocusRequest)
            XposedHelpers.findAndHookMethod(
                audioManagerClass,
                "abandonAudioFocus",
                "android.media.AudioFocusRequest",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        super.beforeHookedMethod(param)
                    }
                }
            )

        } catch (e: Throwable) {
            XposedBridge.log("CarLifeA2DPFix Hook Err: ${e.message}")
        }
    }
}
