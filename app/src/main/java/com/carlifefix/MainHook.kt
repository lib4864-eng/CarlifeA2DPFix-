package com.carlifefix

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 百度CarLife包名
        val pkgCarLife = "com.baidu.carlife"
        if (lpparam.packageName != pkgCarLife) return

        try {
            val audioManagerClass = XposedHelpers.findClass("android.media.AudioManager", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(
                audioManagerClass,
                "requestAudioFocus",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // 直接返回：拒绝抢占音频焦点
                        param.result = -1
                    }
                }
            )
            XposedHelpers.findAndHookMethod(
                audioManagerClass,
                "abandonAudioFocus",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // 放行放弃焦点
                        super.beforeHookedMethod(param)
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("CarLifeA2DPFix Hook Err: ${e.message}")
        }
    }
}
