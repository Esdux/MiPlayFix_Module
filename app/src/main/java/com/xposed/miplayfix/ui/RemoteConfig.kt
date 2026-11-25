package com.xposed.miplayfix.ui

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.xposed.miplayfix.Config
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

/**
 * UI 侧访问 Hook 进程配置的通道。
 *
 * **必须在调 AppShellScreen 的 Activity 的 onCreate 里 attach(this)。**
 * 少了这一步 prefs 恒为 null，所有 get 都返回默认值、所有 put 静默丢弃 ——
 * 表现就是「改完返回上一页又变回原样、看起来没保存」。
 *
 * readyState 是 Compose 可观察的绑定状态：页面拿它当 remember 的 key，
 * 通道 bind 完成后自动重读一次，否则首帧读到的仍是默认值。
 */
object RemoteConfig {

    private const val TAG = "MiPlayFix"

    /** bind 完成变 true，页面用 remember(ready) 重读 */
    val readyState = mutableStateOf(false)

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var attached = false

    fun attach(ctx: Context) {
        if (attached) return
        attached = true
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                runCatching {
                    prefs = service.getRemotePreferences(Config.PREF_NAME)
                    readyState.value = prefs != null
                    Log.i(TAG, "配置通道已连上: ${Config.PREF_NAME}")
                }.onFailure { Log.w(TAG, "打开远程配置失败: ${it.message}") }
            }

            override fun onServiceDied(service: XposedService) {
                prefs = null
                readyState.value = false
                Log.w(TAG, "配置通道已断开")
            }
        })
    }

    val available: Boolean get() = prefs != null

    fun getBool(key: String, def: Boolean): Boolean =
        runCatching { prefs?.getBoolean(key, def) ?: def }.getOrDefault(def)

    /** 用 commit 而不是 apply：跨进程场景下 apply 的异步落盘可能赶不上界面返回 */
    fun putBool(key: String, v: Boolean) {
        runCatching { prefs?.edit()?.putBoolean(key, v)?.commit() }
            .onFailure { Log.w(TAG, "写配置失败 $key: ${it.message}") }
    }

    fun getInt(key: String, def: Int): Int =
        runCatching { prefs?.getInt(key, def) ?: def }.getOrDefault(def)

    fun putInt(key: String, v: Int) {
        runCatching { prefs?.edit()?.putInt(key, v)?.commit() }
            .onFailure { Log.w(TAG, "写配置失败 $key: ${it.message}") }
    }

    fun remove(key: String) {
        runCatching { prefs?.edit()?.remove(key)?.commit() }
    }
}
