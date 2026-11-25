package com.xposed.miplayfix.ui.util

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 全工程唯一的间距来源 —— 12dp 栅格。取自 vcut_lower / HyperMusicCover 的 CardSpacing.kt。
 *
 * 规则（miuix 官方示例 SettingsPage 的写法）：
 * - 页面第一个块：四周 12dp；
 * - 之后每个块：左右 12dp + 下 12dp；
 * - 块上方的 SmallTitle 自带间距（PaddingValues(28.dp, 8.dp)），不要再额外加；
 * - 页面结尾不加额外间距，最后一块自己的 12dp 就是收尾。
 *
 * 为什么必须收敛到一个文件（原注释）：这两个函数出现之前，这段间距在五个页面里有五种写法
 * （只加 top、只加 vertical、只加 bottom……），结果卡与卡之间的缝每页都不一样。
 *
 * 用法：
 *   Card(modifier = Modifier.firstCard()) { ... }   // 页面第一张
 *   Card(modifier = Modifier.nextCard())  { ... }   // 之后每一张
 */
internal fun Modifier.firstCard(): Modifier = padding(12.dp)

internal fun Modifier.nextCard(): Modifier = padding(horizontal = 12.dp).padding(bottom = 12.dp)
