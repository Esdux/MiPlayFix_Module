package com.xposed.miplayfix.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.xposed.miplayfix.ui.AppSettings
import com.xposed.miplayfix.ui.theme.MiuixAppTheme
import com.xposed.miplayfix.ui.util.PageScaffold
import com.xposed.miplayfix.ui.util.firstCard
import com.xposed.miplayfix.ui.util.nextCard
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

/**
 * 主题二级页 —— 会重绘整个 App 的设置单独开一个 Activity。
 *
 * 理由（HyperMusicCover 的注释原意）：主题这种设置一改，页面当场重排，
 * 用户的手指还停在开关上，下面几行会从指头底下消失。放到二级页里改，
 * 回主壳时 onResume 让 settingsEpoch 自增重读，界面才跟着换。
 *
 * 夜间模式在 onStop 应用而不是这里 —— 见 AppShell.kt 的第 1 条注释。
 */
class ThemeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var settings by remember { mutableStateOf(AppSettings.load(this)) }
            val themeMode = remember(settings.themeMode) {
                runCatching { ColorSchemeMode.valueOf(settings.themeMode) }
                    .getOrDefault(ColorSchemeMode.System)
            }
            MiuixAppTheme(themeMode = themeMode) {
                PageScaffold(
                    title = "主题",
                    isBlurEnabled = settings.isBlurEnabled,
                    onBack = { finish() },
                ) {
                    item(key = "mode") {
                        Column {
                            SmallTitle(text = "外观")
                            Card(modifier = Modifier.firstCard()) {
                                // 六个模式是**互斥单选**，用下拉，不要平铺成六行 ArrowPreference。
                                //
                                // 平铺的代价：占半屏、改一次要点两下（先点行再返回）、
                                // 而且「当前是哪个」只能靠某一行底下的一行小字去认。
                                // 下拉把「当前值」直接写在行尾，点开才展开全部选项 ——
                                // HyperMusicCover 的 ThemePage 就是这么做的（WindowDropdownPreference）。
                                //
                                // 什么时候该平铺：选项 ≤ 3 且每项都需要一句说明时（如「浅色/深色」二选一）。
                                // 什么时候该下拉：选项 ≥ 4、或选项名很长、或用户通常只关心当前值。
                                val modeKeys = listOf(
                                    "System" to "跟随系统",
                                    "Light" to "浅色",
                                    "Dark" to "深色",
                                    "MonetSystem" to "动态取色 · 跟随系统",
                                    "MonetLight" to "动态取色 · 浅色",
                                    "MonetDark" to "动态取色 · 深色",
                                )
                                val labels = modeKeys.map { it.second }
                                val currentIndex = modeKeys
                                    .indexOfFirst { it.first == settings.themeMode }
                                    .takeIf { it >= 0 } ?: 0
                                WindowDropdownPreference(
                                    items = labels,
                                    selectedIndex = currentIndex,
                                    title = "主题模式",
                                    // summary 放当前值：收起状态下用户唯一需要知道的就是它
                                    summary = labels[currentIndex],
                                    onSelectedIndexChange = { index ->
                                        settings = settings.copy(themeMode = modeKeys[index].first)
                                        AppSettings.save(this@ThemeActivity, settings)
                                    },
                                )
                            }
                        }
                    }
                    item(key = "navbar") {
                        Column {
                            SmallTitle(text = "底栏")
                            Card(modifier = Modifier.nextCard()) {
                                SwitchPreference(
                                    title = "悬浮底栏",
                                    summary = "底栏离开屏幕边缘，浮在内容之上",
                                    checked = settings.isFloatingNavbar,
                                    onCheckedChange = {
                                        settings = settings.copy(isFloatingNavbar = it)
                                        AppSettings.save(this@ThemeActivity, settings)
                                    },
                                )
                                // 这里曾经有一个 FoldUnder 折叠的「液态玻璃」子开关。
                                // 它只在悬浮时出现，打开后只是把模糊半径 25 → 32 并加一道描边，
                                // 用户看不出区别 —— 一个拨了跟没拨一样的开关，比没有这个开关更糟。
                                // 底栏现在只有两态：贴底 / 悬浮。
                                SwitchPreference(
                                    title = "背景模糊",
                                    summary = "关闭可省电，低端机建议关",
                                    checked = settings.isBlurEnabled,
                                    onCheckedChange = {
                                        settings = settings.copy(isBlurEnabled = it)
                                        AppSettings.save(this@ThemeActivity, settings)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
