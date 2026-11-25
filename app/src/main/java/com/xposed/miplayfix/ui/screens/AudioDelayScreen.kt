package com.xposed.miplayfix.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xposed.miplayfix.Config
import com.xposed.miplayfix.data.DeviceRepo
import com.xposed.miplayfix.ui.RemoteConfig
import com.xposed.miplayfix.ui.util.PageScaffold
import com.xposed.miplayfix.ui.util.firstCard
import com.xposed.miplayfix.ui.util.nextCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

/** 预设延迟（微秒）。250000 是原模块硬编码值；800000/1000000 是系统自身默认 */
private val PRESETS = listOf(
    100000, 150000, 200000, 250000, 300000, 400000, 500000, 800000, 1000000,
)
private val PRESET_LABELS = PRESETS.map { "${it / 1000} ms" }

/** 设备行的下拉：第 0 项是「跟随全局」，其余与全局同一套预设 */
private val DEVICE_LABELS = listOf("跟随全局") + PRESET_LABELS

private fun indexOfDelay(us: Int): Int = PRESETS.indexOf(us).let { if (it < 0) 3 else it }

@Composable
fun AudioDelayScreen(isBlurEnabled: Boolean, extraBottomPadding: Dp = 0.dp) {
    val ready by RemoteConfig.readyState
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var enabled by remember(ready) { mutableStateOf(RemoteConfig.getBool(Config.AUDIO_ENABLED, true)) }
    var global by remember {
        mutableStateOf(RemoteConfig.getInt(Config.AUDIO_DEFAULT_DELAY, Config.DEFAULT_AUDIO_DELAY))
    }
    var devices by remember { mutableStateOf<List<DeviceRepo.Device>>(emptyList()) }
    var overrides by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var loaded by remember { mutableStateOf(false) }

    PageScaffold(title = "音频延迟", isBlurEnabled = isBlurEnabled, extraBottomPadding = extraBottomPadding) {
        item(key = "audio") {
            Column {
                Card(modifier = Modifier.firstCard()) {
                    SwitchPreference(
                        title = "启用音频延迟调整",
                        summary = "关闭后这段 Hook 不加载，延迟交回系统默认",
                        checked = enabled,
                        onCheckedChange = { v ->
                            enabled = v
                            RemoteConfig.putBool(Config.AUDIO_ENABLED, v)
                        },
                    )
                }

                SmallTitle(text = "全局默认")
                Card(modifier = Modifier.nextCard()) {
                    WindowDropdownPreference(
                        items = PRESET_LABELS,
                        selectedIndex = indexOfDelay(global),
                        title = "默认延迟",
                        summary = PRESET_LABELS[indexOfDelay(global)],
                        onSelectedIndexChange = { i ->
                            global = PRESETS[i]
                            RemoteConfig.putInt(Config.AUDIO_DEFAULT_DELAY, global)
                        },
                    )
                }

                SmallTitle(text = "按设备覆盖")
                Card(modifier = Modifier.nextCard()) {
                    ArrowPreference(
                        title = if (!loaded) "读取设备缓存" else "重新读取设备",
                        summary = if (!loaded) {
                            "数据来自 miplay_database 的 CacheDevice，需要 root"
                        } else if (devices.isEmpty()) {
                            "未读到设备，先连接一次音频流转再回来"
                        } else {
                            "已读到 ${devices.size} 台"
                        },
                        onClick = {
                            scope.launch {
                                devices = withContext(Dispatchers.IO) { DeviceRepo.load(context) }
                                overrides = devices.associate { d ->
                                    d.mac to RemoteConfig.getInt(Config.deviceKey(d.mac), -1)
                                }
                                loaded = true
                            }
                        },
                    )
                }

                devices.forEach { dev ->
                    val cur = overrides[dev.mac] ?: -1
                    val sel = if (cur > 0) indexOfDelay(cur) + 1 else 0
                    Card(modifier = Modifier.nextCard()) {
                        WindowDropdownPreference(
                            items = DEVICE_LABELS,
                            selectedIndex = sel,
                            title = dev.name.ifBlank { dev.mac },
                            summary = DEVICE_LABELS[sel] + "　" + dev.mac,
                            onSelectedIndexChange = { i ->
                                if (i == 0) {
                                    RemoteConfig.remove(Config.deviceKey(dev.mac))
                                    overrides = overrides - dev.mac
                                } else {
                                    val v = PRESETS[i - 1]
                                    RemoteConfig.putInt(Config.deviceKey(dev.mac), v)
                                    overrides = overrides + (dev.mac to v)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}