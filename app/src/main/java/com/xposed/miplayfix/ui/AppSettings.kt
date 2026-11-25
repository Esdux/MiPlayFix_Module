package com.xposed.miplayfix.ui

import android.app.UiModeManager
import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

/**
 * App 自身的外观设置 —— 只放"界面长什么样"这一类。
 *
 * **硬规则：同一个值只能有一个真相。** vcut_lower 的注释把这条写死了：模块自己的设置
 * （封面偏移、时钟缩放……）属于跑在 SystemUI 里的 hook，由 hook 自己持久化、界面通过
 * 桥去问；在这里再存一份，只会得到两份互相矛盾的真相。
 *
 * 什么时候该拆成两份：当"谁写"不同。界面设置只有 App 写，放这里；被 hook 读的设置只有
 * hook 写，放 hook 那边。两边都要写的东西，必须定一个所有者，另一边只读。
 */
data class AppSettings(
    val themeMode: String = ColorSchemeMode.System.name,
    val isFloatingNavbar: Boolean = false,
    val isBlurEnabled: Boolean = true,
) {
    // 曾经有过 isLiquidGlass（底栏第三种形态：玻璃高光 + 更强模糊）。
    // 它被删掉的原因不是实现不了，而是**那个开关是假的** ——
    // 打开后除了模糊半径从 25 变 32，用户看不出任何区别，却要为此多背一个
    // 设置项、一条 FoldUnder 折叠逻辑、一份 539 行的自建底栏组件。
    // 一个拨了跟没拨一样的开关，比没有这个开关更糟。
    fun toJson(): String = JSONObject().apply {
        put("themeMode", themeMode)
        put("isFloatingNavbar", isFloatingNavbar)
        put("isBlurEnabled", isBlurEnabled)
    }.toString(2)

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_FLOATING_NAVBAR = "floating_navbar"
        private const val KEY_BLUR_ENABLED = "blur_enabled"

        // 启动图背景：冷启动时窗口先于 Compose 绘制，用这一层颜色顶住白闪
        private const val PREFS_LAUNCH = "launch_background"
        private const val KEY_SURFACE_ARGB = "surface_argb"
        private const val KEY_NIGHT_APPLIED = "night_mode"

        fun load(context: Context): AppSettings {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return AppSettings(
                themeMode = p.getString(KEY_THEME_MODE, ColorSchemeMode.System.name)
                    ?: ColorSchemeMode.System.name,
                isFloatingNavbar = p.getBoolean(KEY_FLOATING_NAVBAR, false),
                isBlurEnabled = p.getBoolean(KEY_BLUR_ENABLED, true),
            )
        }

        fun save(context: Context, s: AppSettings) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putString(KEY_THEME_MODE, s.themeMode)
                putBoolean(KEY_FLOATING_NAVBAR, s.isFloatingNavbar)
                putBoolean(KEY_BLUR_ENABLED, s.isBlurEnabled)
            }
        }

        /** 记住上次绘制用的 surface 色，下次冷启动先拿它涂窗口 */
        fun rememberSurface(context: Context, themeName: String, argb: Int) {
            context.getSharedPreferences(PREFS_LAUNCH, Context.MODE_PRIVATE).edit {
                putInt(KEY_SURFACE_ARGB, argb)
                putString("theme_name", themeName)
            }
        }

        /** 冷启动时窗口的底色；没记录过就返回 null，让系统按主题自己决定 */
        fun surfaceArgb(context: Context): Int? {
            val p = context.getSharedPreferences(PREFS_LAUNCH, Context.MODE_PRIVATE)
            val v = p.getInt(KEY_SURFACE_ARGB, 0)
            return if (v == 0) null else v
        }

        /**
         * 让系统按"App 内选的主题"去解析 -night 资源，而不是按系统设置。
         *
         * 这是为了启动图：windowSplashScreenBackground 是主题属性，在主题解析时就定了，
         * 早于我们任何一行代码。App 内强制浅色而系统是深色时，启动图会是黑的，闪一下
         * 才变成浅色。事后补救不了，只能改主题解析所依据的 configuration —— 就是这个 API。
         *
         * 它是一次 configuration change，会重建 Activity，所以只在值真的变了才写。
         */
        fun applyNightMode(context: Context) {
            val wanted = when (load(context).themeMode) {
                ColorSchemeMode.Light.name, ColorSchemeMode.MonetLight.name ->
                    UiModeManager.MODE_NIGHT_NO
                ColorSchemeMode.Dark.name, ColorSchemeMode.MonetDark.name ->
                    UiModeManager.MODE_NIGHT_YES
                else -> UiModeManager.MODE_NIGHT_AUTO
            }
            val p = context.getSharedPreferences(PREFS_LAUNCH, Context.MODE_PRIVATE)
            if (p.getInt(KEY_NIGHT_APPLIED, Int.MIN_VALUE) == wanted) return
            runCatching {
                context.getSystemService(UiModeManager::class.java)
                    ?.setApplicationNightMode(wanted)
                p.edit { putInt(KEY_NIGHT_APPLIED, wanted) }
            }
        }
    }
}
