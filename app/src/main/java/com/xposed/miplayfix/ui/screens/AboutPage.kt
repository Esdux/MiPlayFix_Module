package com.xposed.miplayfix.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xposed.miplayfix.R
import com.xposed.miplayfix.ui.util.PageScaffold
import com.xposed.miplayfix.ui.util.firstCard
import com.xposed.miplayfix.ui.util.nextCard
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 关于页 = 应用身份（图标 + 名称 + 版本）→ 链接 → 许可与致谢。
 *
 * 图标写法照搬 HyperMusicCover：一张 `ic_about_logo.xml`（带渐变底的 vector）+
 * `squircleClip(cornerRadius = 28.dp)`。squircle 是 HyperOS 的应用图标形状，
 * 比 RoundedCornerShape 的圆角更「方中带圆」，是这套界面辨识度的一部分。
 *
 * **不要用 `painterResource(android.R.drawable.sym_def_app_icon)`。** 那是系统的
 * adaptive-icon（xml），而 Compose 的 painterResource 只认 VectorDrawable 与位图，
 * 翻到本页时直接抛 IllegalArgumentException: Only VectorDrawables and rasterized
 * asset types are supported，进程退出。只在翻到末页才触发，最容易漏测。
 *
 * isCurrent 是 lambda 而不是 Boolean：关于页常有逐帧动画，传 Boolean 会让翻页时
 * 所有页一起重组；传 lambda 则只有本页在自己读它时才重组。
 */
@Composable
fun AboutPage(
    isBlurEnabled: Boolean,
    extraBottomPadding: Dp = 0.dp,
    isCurrent: () -> Boolean = { true },
    appName: String = "Example",
    versionName: String = "1.0",
    onOpenProject: () -> Unit = {},
    onOpenIssues: () -> Unit = {},
    onOpenLicense: () -> Unit = {},
) {
    PageScaffold(title = "关于", isBlurEnabled = isBlurEnabled, extraBottomPadding = extraBottomPadding) {
        item(key = "identity") {
            Column(
                modifier = Modifier.fillMaxWidth().firstCard(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 100dp 图标 + 28dp squircle 圆角：与 HyperMusicCover 关于页同尺寸
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        // 12dp：与块间距同值。图标上方留白不需要单独一套数。
                        .padding(top = 12.dp)
                        .size(100.dp)
                        .squircleClip(cornerRadius = 28.dp),
                ) {
                    Image(
                        modifier = Modifier.size(100.dp),
                        painter = painterResource(R.drawable.ic_about_logo),
                        contentDescription = null,
                    )
                }
                MiuixText(
                    // 4dp：5dp 是手调的，落在 4 的倍数之外，换字号时会对不齐。
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    text = appName,
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.SemiBold,
                )
                MiuixText(
                    modifier = Modifier.padding(bottom = 12.dp),
                    text = "v$versionName",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        }

        item(key = "links") {
            Card(modifier = Modifier.nextCard()) {
                ArrowPreference(title = "项目主页", summary = "GitHub", onClick = onOpenProject)
                ArrowPreference(title = "问题反馈", summary = "Issues", onClick = onOpenIssues)
            }
        }

        item(key = "license") {
            Column {
                SmallTitle(text = "开源许可")
                Card(modifier = Modifier.nextCard()) {
                    ArrowPreference(
                        title = "第三方许可",
                        summary = "Miuix 等组件使用的许可证",
                        onClick = onOpenLicense,
                    )
                }
            }
        }
    }
}
