package com.xposed.miplayfix.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xposed.miplayfix.Config
import com.xposed.miplayfix.ui.RemoteConfig
import com.xposed.miplayfix.ui.util.PageScaffold
import com.xposed.miplayfix.ui.util.firstCard
import com.xposed.miplayfix.ui.util.nextCard
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

/**
 * 设置页只放「开关」与「会重绘整个 App 的页面入口」。
 * 具体配置（延迟值、通道选择）不在这里再开一个入口 —— 它们在「功能」页，
 * 一个配置只准一个入口。
 */
@Composable
fun SettingsPage(isBlurEnabled: Boolean, extraBottomPadding: Dp = 0.dp) {
    val ready by RemoteConfig.readyState
    val context = LocalContext.current

    var master by remember(ready) { mutableStateOf(RemoteConfig.getBool(Config.MASTER, true)) }
    var audio by remember(ready) { mutableStateOf(RemoteConfig.getBool(Config.AUDIO_ENABLED, true)) }
    var medium by remember(ready) { mutableStateOf(RemoteConfig.getBool(Config.MEDIUM_ENABLED, true)) }
    var reconnect by remember(ready) { mutableStateOf(RemoteConfig.getBool(Config.RECONNECT_ENABLED, true)) }

    PageScaffold(title = "设置", isBlurEnabled = isBlurEnabled, extraBottomPadding = extraBottomPadding) {
        item(key = "settings") {
            Column {
                SmallTitle(text = "功能开关")
                Card(modifier = Modifier.firstCard()) {
                    SwitchPreference(
                        title = "功能总开关",
                        summary = "关闭时所有 Hook 不加载，改完需重启目标应用",
                        checked = master,
                        onCheckedChange = { v ->
                            master = v
                            RemoteConfig.putBool(Config.MASTER, v)
                        },
                    )
                }
                Card(modifier = Modifier.nextCard()) {
                    SwitchPreference(
                        title = "音频延迟调整",
                        summary = "拦截 MultiMirrorControl 的延迟设置",
                        checked = audio && master,
                        onCheckedChange = { v ->
                            audio = v
                            RemoteConfig.putBool(Config.AUDIO_ENABLED, v)
                        },
                    )
                }
                Card(modifier = Modifier.nextCard()) {
                    SwitchPreference(
                        title = "连接介质改写",
                        summary = "拦截 ChannelOptions 的通道类型设置",
                        checked = medium && master,
                        onCheckedChange = { v ->
                            medium = v
                            RemoteConfig.putBool(Config.MEDIUM_ENABLED, v)
                        },
                    )
                }
                Card(modifier = Modifier.nextCard()) {
                    SwitchPreference(
                        title = "重新连接入口",
                        summary = "主页的重新连接按钮",
                        checked = reconnect && master,
                        onCheckedChange = { v ->
                            reconnect = v
                            RemoteConfig.putBool(Config.RECONNECT_ENABLED, v)
                        },
                    )
                }

                SmallTitle(text = "外观")
                Card(modifier = Modifier.nextCard()) {
                    ArrowPreference(
                        title = "主题与外观",
                        summary = "深色模式、悬浮底栏、背景模糊",
                        onClick = {
                            context.startActivity(Intent(context, ThemeActivity::class.java))
                        },
                    )
                }
            }
        }
    }
}