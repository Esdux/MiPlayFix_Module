package com.xposed.miplayfix.ui.util

import androidx.compose.ui.unit.dp

/**
 * 全工程的间距与尺寸阶梯 —— **唯一来源**。
 *
 * 为什么要有这个文件：
 * 早先每个页面自己写数字，同一类东西出现了五种值 —— 对话框按钮间距 20dp、
 * 页尾留白 24dp、关于页图标上方 8dp、标题与副标题之间 5dp、状态卡内部 1dp。
 * 每一处单看都说得通，放在一起就是「这个 App 每个页面的手感都不一样」。
 * 用户说不出哪里不对，但会觉得「不精致」。
 *
 * 阶梯只保留 4 的倍数，外加一个 12 作为栅格基准：
 *
 * | 值 | 用途 |
 * |---|---|
 * | 2dp  | 纯视觉分离（标题与紧邻的副标题），不承担间距语义 |
 * | 4dp  | 组件内部微调（chip 内边距、图标与文字的贴合） |
 * | 8dp  | 组件内部呼吸（图标与标题之间、SmallTitle 自带值） |
 * | 12dp | **块间距**：卡片之间、卡片与屏幕边缘、对话框按钮之间 |
 * | 16dp | 卡内边距（miuix 的 BasicComponentDefaults.InsideMargin 就是这个值） |
 * | 28dp | SmallTitle 的水平内边距（miuix 自带，跟系统文字缩进对齐） |
 *
 * 规则：**能用 [CardSpacing] 的 firstCard/nextCard 就不要手写 12dp**；
 * 只有这三类地方允许出现字面量 —— 图标尺寸、卡内边距、以及本文件里的常量。
 * 别的数字出现在 padding 里，就是有人又手调了一次。
 */
object DshSpacing {
    /** 纯视觉分离，不表达「这是一组」。 */
    val Hairline = 2.dp

    /** 组件内部微调：chip、徽标、图标贴边。 */
    val Tight = 4.dp

    /** 组件内部呼吸：图标与标题之间、行内元素之间。 */
    val Compact = 8.dp

    /** 块间距：卡片之间、卡片与屏幕边缘。与 [CardSpacing] 同值。 */
    val Block = 12.dp

    /** 卡内边距：与 miuix BasicComponentDefaults.InsideMargin 一致。 */
    val CardInside = 16.dp
}

/**
 * 图标尺寸阶梯。
 *
 * 四个值都有明确出处，不要临时加第五个：
 * - [ListIcon] 58dp：列表行左侧图标（Cirno 的 AppItemCompose 钉死这个数）
 * - [DetailIcon] 72dp：二级页身份区图标（比列表大一号，表示「进到里面了」）
 * - [HeroIcon] 100dp：关于页应用图标（HyperMusicCover 关于页同尺寸）
 * - [WatermarkIcon] 110dp：状态卡底纹图标（故意出血，只露一角）
 */
object DshIconSize {
    val ListIcon = 58.dp
    val DetailIcon = 72.dp
    val HeroIcon = 100.dp
    val WatermarkIcon = 110.dp
}
