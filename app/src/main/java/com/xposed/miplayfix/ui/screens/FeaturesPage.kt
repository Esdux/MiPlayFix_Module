package com.xposed.miplayfix.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xposed.miplayfix.data.DeviceRepo
import com.xposed.miplayfix.ui.util.PageScaffold
import com.xposed.miplayfix.ui.util.firstCard
import com.xposed.miplayfix.ui.util.nextCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference

/**
 * 功能页是各功能的**唯一入口**。主页与设置页不再重复放同名入口 ——
 * 两处入口必有一处停在旧版，用户改完回到另一处会以为没保存。
 */
@Composable
fun FeaturesPage(isBlurEnabled: Boolean, extraBottomPadding: Dp = 0.dp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    PageScaffold(title = "功能", isBlurEnabled = isBlurEnabled, extraBottomPadding = extraBottomPadding) {
        item(key = "features") {
            Column {
                Card(modifier = Modifier.firstCard()) {
                    ArrowPreference(
                        title = "音频延迟",
                        summary = "全局默认与按设备覆盖，改完立刻写入配置",
                        onClick = {
                            context.startActivity(
                                FeatureDetailActivity.intent(
                                    context,
                                    "音频延迟",
                                    "全局默认与按设备覆盖",
                                ),
                            )
                        },
                    )
                }

                Card(modifier = Modifier.nextCard()) {
                    ArrowPreference(
                        title = "连接介质",
                        summary = "把 P2P 请求改走局域网，绕开网卡不支持承载网络的问题",
                        onClick = {
                            context.startActivity(
                                FeatureDetailActivity.intent(
                                    context,
                                    "连接介质",
                                    "投屏、互传、其它各自的通道",
                                ),
                            )
                        },
                    )
                }

                SmallTitle(text = "应急")
                Card(modifier = Modifier.nextCard()) {
                    ArrowPreference(
                        title = "立即重启互联服务",
                        summary = "断开当前所有会话并重新拉起，连接卡住时用",
                        onClick = {
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { DeviceRepo.reconnect() }
                                Toast.makeText(
                                    context,
                                    if (ok) "已重启" else "需要 root 权限",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                }
            }
        }
    }
}
