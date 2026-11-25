package com.xposed.miplayfix.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.xposed.miplayfix.ui.AppSettings
import com.xposed.miplayfix.ui.theme.MiuixAppTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.preference.ArrowPreference
import com.xposed.miplayfix.ui.util.PageScaffold
import com.xposed.miplayfix.ui.util.firstCard
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

/**
 * 二级页容器。按标题分派到具体设置界面，避免为每个功能建一个 Activity。
 */
class FeatureDetailActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_SUMMARY = "summary"

        fun intent(context: Context, title: String, summary: String): Intent =
            Intent(context, FeatureDetailActivity::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_SUMMARY, summary)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "功能"
        val summary = intent.getStringExtra(EXTRA_SUMMARY) ?: ""
        val settings = AppSettings.load(this)

        setContent {
            MiuixAppTheme(
                themeMode = runCatching { ColorSchemeMode.valueOf(settings.themeMode) }
                    .getOrDefault(ColorSchemeMode.System),
            ) {
                when (title) {
                    "音频延迟" -> AudioDelayScreen(isBlurEnabled = settings.isBlurEnabled)
                    "连接介质" -> MediumScreen(isBlurEnabled = settings.isBlurEnabled)
                    else -> PlaceholderPage(title, summary, settings.isBlurEnabled)
                }
            }
        }
    }
}

@Composable
private fun PlaceholderPage(title: String, summary: String, isBlurEnabled: Boolean) {
    PageScaffold(title = title, isBlurEnabled = isBlurEnabled) {
        item(key = "ph") {
            Column {
                Card(modifier = androidx.compose.ui.Modifier.firstCard()) {
                    ArrowPreference(
                        title = title,
                        summary = summary.ifBlank { "该功能尚未实现" },
                        onClick = {},
                    )
                }
            }
        }
    }
}
