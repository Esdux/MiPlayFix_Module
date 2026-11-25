package com.xposed.miplayfix

import android.content.SharedPreferences
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Method

/**
 * 模块入口。三个功能各自独立开关，总开关关闭时一律不挂 Hook。
 *
 * 目标包：
 *  - com.milink.service：音频延迟（原 MiPlayFix 功能）+ 连接介质
 *  - com.xiaomi.mirror：连接介质（手机端构造 ChannelOptions 的地方）
 *
 * 逆向口径（见全局规范）：不认混淆名认结构。类名走候选表，找不到就跳过，不崩进程。
 */
class MainHook : XposedModule() {

    private companion object {
        const val TAG = "MiPlayFix"

        const val PKG_MILINK = "com.milink.service"
        const val PKG_MIRROR = "com.xiaomi.mirror"

        /** 音频延迟：native 方法 setAudioPlayDelayTime(long handle, int delayMicros) */
        const val CLS_MIRROR_CONTROL = "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl"
        const val MTH_AUDIO_DELAY = "setAudioPlayDelayTime"

        /** 连接介质：SDK 里所有请求最终都落到这两个 Options 类 */
        val CLS_CHANNEL_OPTIONS = arrayOf(
            "com.xiaomi.continuity.channel.ClientChannelOptionsV2",
            "com.xiaomi.continuity.channel.ClientChannelOptions",
        )
        const val MTH_SET_MEDIUM = "setConnectMediumType"
        const val MTH_SET_TRUST = "setTrustLevel"

        /** trustLevel → 请求类型：48=互传，16=投屏，其它=通用 */
        const val TRUST_TRANSFER = 48
        const val TRUST_CAST = 16
    }

    private lateinit var prefs: SharedPreferences

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        prefs = getRemotePreferences(Config.PREF_NAME)
        log("模块已加载，总开关=" + master())
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        val pkg = param.packageName ?: return
        if (pkg != PKG_MILINK && pkg != PKG_MIRROR) return

        // 总开关关闭 → 一个 Hook 都不挂
        if (!master()) {
            log("$pkg 总开关关闭，跳过")
            return
        }

        val cl = param.defaultClassLoader ?: return

        if (pkg == PKG_MILINK && bool(Config.AUDIO_ENABLED, true)) {
            runCatching { hookAudioDelay(cl) }.onFailure { log("音频延迟注入失败: ${it.message}") }
        }
        if (bool(Config.MEDIUM_ENABLED, true)) {
            runCatching { hookChannelMedium(cl) }.onFailure { log("连接介质注入失败: ${it.message}") }
        }
    }

    // ---------------------------------------------------------------- 音频延迟

    private fun hookAudioDelay(cl: ClassLoader) {
        val cls = Class.forName(CLS_MIRROR_CONTROL, false, cl)
        val m = cls.getDeclaredMethod(MTH_AUDIO_DELAY, Long::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        hook(m).intercept { chain: XposedInterface.Chain ->
            val args = chain.args.toMutableList()
            val handle = (args[0] as? Long) ?: 0L
            val mac = lookupDeviceMac(chain.thisObject, handle)
            args[1] = delayFor(mac)
            chain.proceed(args.toTypedArray())
        }
        log("已挂音频延迟 Hook")
    }

    /** optionMap 是 deviceMac -> handle，用 handle 反查 deviceMac。拿不到就返回 null（用全局值） */
    private fun lookupDeviceMac(thisObj: Any?, handle: Long): String? {
        if (thisObj == null) return null
        return runCatching {
            val f = thisObj.javaClass.getDeclaredField("optionMap")
            f.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val map = f.get(thisObj) as? Map<String, Long> ?: return null
            map.entries.firstOrNull { it.value == handle }?.key
        }.getOrNull()
    }

    private fun delayFor(mac: String?): Int {
        if (mac != null) {
            val per = prefs.getInt(Config.deviceKey(mac), -1)
            if (per > 0) return per
        }
        return prefs.getInt(Config.AUDIO_DEFAULT_DELAY, Config.DEFAULT_AUDIO_DELAY)
    }

    // ---------------------------------------------------------------- 连接介质

    private fun hookChannelMedium(cl: ClassLoader) {
        var hit = 0
        for (name in CLS_CHANNEL_OPTIONS) {
            val cls = runCatching { Class.forName(name, false, cl) }.getOrNull() ?: continue
            val m = runCatching {
                cls.getDeclaredMethod(MTH_SET_MEDIUM, Int::class.javaPrimitiveType)
            }.getOrNull() ?: continue

            hook(m).intercept { chain: XposedInterface.Chain ->
                val args = chain.args.toMutableList()
                val orig = (args[0] as? Int) ?: 0
                // 只在对方要 P2P 时改写；其余保持原样，避免影响本来正常的链路
                if (orig == Config.MEDIUM_P2P) {
                    val target = mediumFor(chain.thisObject)
                    args[0] = target
                    log("介质改写 P2P -> ${Config.mediumLabel(target)}")
                }
                chain.proceed(args.toTypedArray())
            }
            hit++
            log("已挂连接介质 Hook: $name")
        }
        if (hit == 0) log("未找到任何 ChannelOptions 类（版本可能变化）")
    }

    /** 按 Options 实例上的 trustLevel 判断是哪类请求 */
    private fun mediumFor(thisObj: Any?): Int {
        val trust = runCatching {
            val m = thisObj?.javaClass?.getDeclaredMethod("getTrustLevel")
            m?.isAccessible = true
            m?.invoke(thisObj) as? Int
        }.getOrNull() ?: -1
        return when (trust) {
            TRUST_TRANSFER -> prefs.getInt(Config.MEDIUM_TRANSFER, Config.DEFAULT_MEDIUM_TRANSFER)
            TRUST_CAST -> prefs.getInt(Config.MEDIUM_CAST, Config.DEFAULT_MEDIUM_CAST)
            else -> prefs.getInt(Config.MEDIUM_OTHER, Config.DEFAULT_MEDIUM_OTHER)
        }
    }

    // ---------------------------------------------------------------- 工具

    private fun master(): Boolean = bool(Config.MASTER, true)

    private fun bool(key: String, def: Boolean): Boolean =
        runCatching { prefs.getBoolean(key, def) }.getOrDefault(def)

    private fun log(message: String) {
        runCatching { log(Log.INFO, TAG, message) }
    }
}