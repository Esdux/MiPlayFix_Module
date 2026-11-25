package com.xposed.miplayfix.ui.util

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * 点底栏翻页用的弹簧。取自 vcut_lower / HyperMusicCover 的 PagerNavigationSpring。
 *
 * 为什么不用 `pagerState.animateScrollToPage()`：它走的是 tween，跨两页以上就是一条匀速
 * 直线，手指离开底栏后页面像被绳子拖着走。弹簧给出的是"先快后慢、末端略微收住"的位移，
 * 与 HyperOS 系统分页、与 miuix 自己的转场是同一个手感。
 *
 * 参数含义（改之前先读）：
 * - dampingRatio 0.86：欠阻尼。1.0 会完全没有回弹，看着发木；低于 0.75 会明显来回荡。
 * - stiffness 380：偏硬。这是"跨页"而不是"打开面板"，行程长，太软会显得拖沓。
 * - visibilityThreshold 1f：1 像素以内就停，否则末尾会有肉眼可见的长时间微调。
 *
 * 注意配套用法：`pagerState.scroll(MutatePriority.UserInput) { ... scrollBy(...) }`，
 * 必须用 UserInput 优先级，否则手指同时按住时会被手势抢走。
 */
val PagerNavigationSpringSpec: SpringSpec<Float> = spring(
    dampingRatio = 0.86f,
    stiffness = 380f,
    visibilityThreshold = 1f,
)

/** 需要更"弹"的场合（如开关面板展开）用这个；不要用在跨页上 */
val PanelSpringSpec: SpringSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)
