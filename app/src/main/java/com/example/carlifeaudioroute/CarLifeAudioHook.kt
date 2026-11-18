package com.example.carlifeaudioroute

import android.app.AndroidAppHelper
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioTrack
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class CarLifeAudioHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // 兼容普通版、Plus版以及三星定制版
        if (lpparam.packageName != "com.baidu.carlife" && 
            lpparam.packageName != "com.baidu.carlifeplus" &&
            lpparam.packageName != "com.baidu.carlife.samsung") return

        XposedBridge.log("[CarLifeAudioRoute] 模块已加载: ${lpparam.packageName}")

        hookRequestAudioFocus(lpparam.classLoader)
        hookAudioTrack(lpparam.classLoader)
        hookBluetoothA2dpConnectSink(lpparam.classLoader)
    }

    // 1. 屏蔽音频焦点抢占
    private fun hookRequestAudioFocus(classLoader: ClassLoader) {
        try {
            val audioManagerClass = XposedHelpers.findClass("android.media.AudioManager", classLoader)

            XposedHelpers.findAndHookMethod(
                audioManagerClass, "requestAudioFocus", "android.media.AudioFocusRequest",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                        XposedBridge.log("[CarLifeAudioRoute] 拦截: requestAudioFocus(AudioFocusRequest)")
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                audioManagerClass, "requestAudioFocus", "android.media.AudioManager\$OnAudioFocusChangeListener",
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                        XposedBridge.log("[CarLifeAudioRoute] 拦截: 旧版 requestAudioFocus")
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("[CarLifeAudioRoute] Hook requestAudioFocus 失败: ${t.message}")
        }
    }

    // 2. 强制 AudioTrack 输出到 A2DP
    private fun hookAudioTrack(classLoader: ClassLoader) {
        try {
            val builderClass = XposedHelpers.findClass("android.media.AudioTrack\$Builder", classLoader)
            XposedHelpers.findAndHookMethod(builderClass, "build", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val track = param.result as? AudioTrack ?: return
                    forceRouteToA2dp(track)
                }
            })
        } catch (t: Throwable) {
            XposedBridge.log("[CarLifeAudioRoute] Hook AudioTrack.Builder 失败: ${t.message}")
        }
    }

    private fun forceRouteToA2dp(track: AudioTrack) {
        try {
            val appContext = AndroidAppHelper.currentApplication() ?: return
            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val a2dpDevice = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP } ?: return
            val success = track.setPreferredDevice(a2dpDevice)
            XposedBridge.log("[CarLifeAudioRoute] 尝试路由至 A2DP: $success")
        } catch (t: Throwable) {
            XposedBridge.log("[CarLifeAudioRoute] 强制路由失败: ${t.message}")
        }
    }

    // 3. 拦截私有 connectSink
    private fun hookBluetoothA2dpConnectSink(classLoader: ClassLoader) {
        try {
            val a2dpClass = XposedHelpers.findClass("android.bluetooth.BluetoothA2dp", classLoader)
            val signatures = listOf(arrayOf("android.bluetooth.BluetoothDevice"), arrayOf(String::class.java))
            for (sig in signatures) {
                try {
                    XposedHelpers.findAndHookMethod(a2dpClass, "connectSink", *sig, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result = false
                            XposedBridge.log("[CarLifeAudioRoute] 拦截: connectSink")
                        }
                    })
                } catch (_: Throwable) {}
            }
        } catch (t: Throwable) {
            XposedBridge.log("[CarLifeAudioRoute] Hook connectSink 失败: ${t.message}")
        }
    }
}
