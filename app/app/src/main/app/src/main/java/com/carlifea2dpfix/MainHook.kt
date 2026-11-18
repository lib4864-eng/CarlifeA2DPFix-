package com.carlifea2dpfix

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.bluetooth.BluetoothA2dp
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainHook : IXposedHookLoadPackage {
    private val targetPkgs = setOf("com.baidu.carlife", "com.baidu.carlifeplus")
    private var targetA2dpId: Int = -1

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName !in targetPkgs) return
        val cl = lpparam.classLoader
        XposedBridge.log("[CarLifeA2DPFix] 模块加载，目标包：${lpparam.packageName}")

        Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate({ targetA2dpId = getConnectedA2dpId() }, 0, 1500, TimeUnit.MILLISECONDS)

        try {
            val clsA2dp = XposedHelpers.findClass("android.bluetooth.BluetoothA2dp", cl)
            XposedHelpers.findAndHookMethod(
                clsA2dp, "connectSink",
                XposedHelpers.findClass("android.bluetooth.BluetoothDevice", cl),
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        XposedBridge.log("[CarLifeA2DPFix] 🚫 拦截 connectSink 私有A2DP")
                        param.result = false
                    }
                }
            )
            XposedBridge.log("[CarLifeA2DPFix] ✅ Hook connectSink OK")
        } catch (e: Throwable) {
            XposedBridge.log("[CarLifeA2DPFix] ❌ Hook connectSink: ${e.message}")
        }

        try {
            val builderCls = XposedHelpers.findClass("android.media.AudioTrack\$Builder", cl)
            XposedHelpers.findAndHookMethod(
                builderCls, "setPreferredDevice",
                AudioDeviceInfo::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (targetA2dpId <= 0) return
                        val devBuilderCls = XposedHelpers.findClass("android.media.AudioDeviceInfo\$Builder", cl)
                        val devInfoBuilder = XposedHelpers.newInstance(devBuilderCls, targetA2dpId)
                        val newDev = XposedHelpers.callMethod(devInfoBuilder, "build")
                        param.args[0] = newDev
                        XposedBridge.log("[CarLifeA2DPFix] 🔊 强制路由A2DP id=$targetA2dpId")
                    }
                }
            )
            XposedBridge.log("[CarLifeA2DPFix] ✅ Hook AudioTrack.Builder OK")
        } catch (e: Throwable) {
            XposedBridge.log("[CarLifeA2DPFix] ❌ Hook AudioTrack.Builder: ${e.message}")
        }

        try {
            val methodCls = XposedHelpers.findClass("java.lang.reflect.Method", cl)
            XposedHelpers.findAndHookMethod(
                methodCls, "invoke",
                Any::class.java, Array<Any>::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val m = param.thisObject as java.lang.reflect.Method
                        val keywords = listOf("connectSink", "createA2dpSession", "connectAudioSink")
                        if (keywords.any { m.name.contains(it) }) {
                            XposedBridge.log("[CarLifeA2DPFix] 🚫 反射拦截: ${m.name}")
                            param.result = null
                        }
                    }
                }
            )
            XposedBridge.log("[CarLifeA2DPFix] ✅ Hook Method.invoke OK")
        } catch (e: Throwable) {
            XposedBridge.log("[CarLifeA2DPFix] ❌ Hook Method.invoke: ${e.message}")
        }

        try {
            val audioManagerCls = XposedHelpers.findClass("android.media.AudioManager", cl)
            val focusReqCls = XposedHelpers.findClass("android.media.AudioFocusRequest", cl)
            XposedHelpers.findAndHookMethod(
                audioManagerCls, "requestAudioFocus", focusReqCls,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        XposedBridge.log("[CarLifeA2DPFix] 🚫 拦截音频焦点抢占")
                        param.result = AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                    }
                }
            )
            XposedBridge.log("[CarLifeA2DPFix] ✅ Hook requestAudioFocus OK")
        } catch (e: Throwable) {
            XposedBridge.log("[CarLifeA2DPFix] ❌ Hook requestAudioFocus: ${e.message}")
        }
    }

    private fun getConnectedA2dpId(): Int {
        return try {
            val ctx = XposedHelpers.callStaticMethod(
                XposedHelpers.findClass("android.app.ActivityThread", null),
                "currentActivityThread"
            )
            val appCtx = XposedHelpers.callMethod(ctx, "getApplication")
            val context = XposedHelpers.callMethod(appCtx, "getApplicationContext") as android.content.Context
            val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
            val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (d in devices) {
                if (d.type == 0x400) {
                    XposedBridge.log("[CarLifeA2DPFix] 🎧 识别A2DP id=${d.id}, name=${d.productName}")
                    return d.id
                }
            }
            -1
        } catch (e: Throwable) {
            -1
        }
    }
}
