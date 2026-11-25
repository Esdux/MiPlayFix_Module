package com.xposed.miplayfix.ui.screens

import android.widget.Toast
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

/**
 * 主页只放「一眼看状态 + 一个当场能做的动作」。
 * 具体配置的入口统一在「功能」页 —— 同一配置两处入口会让其中一处永远停在旧版。
 */
@Composable
fun HomePage(isBlurEnabled: Boolean, extraBottomPadding: Dp = 0.dp) {
    val ready by RemoteConfig.readyState
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var master by remember(ready) { mutableStateOf(RemoteConfig.getBool(Config.MASTER, true)) }
    var devices by remember { mutableStateOf<List<DeviceRepo.Device>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    PageScaffold(title = "MiPlayFix", isBlurEnabled = isBlurEnabled, extraBottomPadding = extraBottomPadding) {
        item(key = "master") {
            Column {
                Card(modifier = Modifier.firstCard()) {
                    SwitchPreference(
                        title = "功能总开关",
                        summary = if (master) {
                            "已开启，各功能按自己的开关生效"
                        } else {
                            "已关闭，目标进程不加载任何 Hook"
                        },
                        checked = master,
                        onCheckedChange = { v ->
                            master = v
                            RemoteConfig.putBool(Config.MASTER, v)
                        },
                    )
                }

                SmallTitle(text = "连接")
                Card(modifier = Modifier.nextCard()) {
                    ArrowPreference(
                        title = "重新连接电脑",
                        summary = "断开并重新拉起互联服务，让手机重新发起连接",
                        onClick = {
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { DeviceRepo.reconnect() }
                                Toast.makeText(
                                    context,
                                    if (ok) "已重新连接" else "需要 root 权限",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                }

                SmallTitle(text = "设备缓存")
                Card(modifier = Modifier.nextCard()) {
                    ArrowPreference(
                        title = if (loading) "读取中" else "读取妙播设备缓存",
                        summary = when {
                            loading -> "正在读 miplay_database"
                            devices.isEmpty() -> "CacheDevice 表，用于按设备区分延迟"
                            else -> "已发现 ${devices.size} 台电脑"
                        },
                        onClick = {
                            loading = true
                            scope.launch {
                                devices = withContext(Dispatchers.IO) { DeviceRepo.load(context) }
                                loading = false
                                if (devices.isEmpty()) {
                                    Toast.makeText(context, "未读到缓存，需要 root", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}