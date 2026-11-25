package com.xposed.miplayfix.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xposed.miplayfix.Config
import com.xposed.miplayfix.ui.RemoteConfig
import com.xposed.miplayfix.ui.util.PageScaffold
import com.xposed.miplayfix.ui.util.firstCard
import com.xposed.miplayfix.ui.util.nextCard
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

private val MEDIUM_VALUES = Config.MEDIUM_CHOICES.map { it.first }
private val MEDIUM_LABELS = Config.MEDIUM_CHOICES.map { Config.mediumLabel(it.first) }

private fun indexOfMedium(v: Int): Int =
    MEDIUM_VALUES.indexOf(v).let { if (it < 0) 0 else it }

/**
 * 连接介质配置。手机向电脑发起的请求会在 ChannelOptions 里指定通道类型：
 *   32 = P2P / WiFi Direct，需要电脑网卡支持承载网络
 *   128 = WiFi LAN1，普通局域网
 * 只把 32 改写成选定值，其它值原样放行。
 */
@Composable
fun MediumScreen(isBlurEnabled: Boolean, extraBottomPadding: Dp = 0.dp) {
    val ready by RemoteConfig.readyState
    var enabled by remember(ready) { mutableStateOf(RemoteConfig.getBool(Config.MEDIUM_ENABLED, true)) }
    var cast by remember(ready) { mutableStateOf(RemoteConfig.getInt(Config.MEDIUM_CAST, Config.DEFAULT_MEDIUM_CAST)) }
    var transfer by remember {
        mutableStateOf(RemoteConfig.getInt(Config.MEDIUM_TRANSFER, Config.DEFAULT_MEDIUM_TRANSFER))
    }
    var other by remember {
        mutableStateOf(RemoteConfig.getInt(Config.MEDIUM_OTHER, Config.DEFAULT_MEDIUM_OTHER))
    }

    PageScaffold(title = "连接介质", isBlurEnabled = isBlurEnabled, extraBottomPadding = extraBottomPadding) {
        item(key = "medium") {
            Column {
                Card(modifier = Modifier.firstCard()) {
                    SwitchPreference(
                        title = "启用介质改写",
                        summary = "只把 P2P 改写成下面选定的通道，其它值不动",
                        checked = enabled,
                        onCheckedChange = { v ->
                            enabled = v
                            RemoteConfig.putBool(Config.MEDIUM_ENABLED, v)
                        },
                    )
                }

                SmallTitle(text = "各类请求使用的通道")
                Card(modifier = Modifier.nextCard()) {
                    WindowDropdownPreference(
                        items = MEDIUM_LABELS,
                        selectedIndex = indexOfMedium(cast),
                        title = "投屏",
                        summary = MEDIUM_LABELS[indexOfMedium(cast)],
                        onSelectedIndexChange = { i ->
                            cast = MEDIUM_VALUES[i]
                            RemoteConfig.putInt(Config.MEDIUM_CAST, cast)
                        },
                    )
                }
                Card(modifier = Modifier.nextCard()) {
                    WindowDropdownPreference(
                        items = MEDIUM_LABELS,
                        selectedIndex = indexOfMedium(transfer),
                        title = "互传",
                        summary = MEDIUM_LABELS[indexOfMedium(transfer)],
                        onSelectedIndexChange = { i ->
                            transfer = MEDIUM_VALUES[i]
                            RemoteConfig.putInt(Config.MEDIUM_TRANSFER, transfer)
                        },
                    )
                }
                Card(modifier = Modifier.nextCard()) {
                    WindowDropdownPreference(
                        items = MEDIUM_LABELS,
                        selectedIndex = indexOfMedium(other),
                        title = "其它请求",
                        summary = MEDIUM_LABELS[indexOfMedium(other)],
                        onSelectedIndexChange = { i ->
                            other = MEDIUM_VALUES[i]
                            RemoteConfig.putInt(Config.MEDIUM_OTHER, other)
                        },
                    )
                }
            }
        }
    }
}