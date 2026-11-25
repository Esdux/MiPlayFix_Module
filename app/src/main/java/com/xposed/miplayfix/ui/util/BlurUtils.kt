package com.xposed.miplayfix.ui.util

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.xposed.miplayfix.ui.theme.isInDarkTheme

/**
 * 顶栏渐进模糊的统一参数。
 *
 * 所有页面的顶栏模糊都从这里读，改一处全局生效 —— 这是「看着像 HyperOS」的关键参数组，
 * 不要在各页面里散着写。
 */
object TopBarBlurConfig {
    /** 模糊半径：模糊最强处的强度。15 是「能看出模糊但不糊成一片」的位置 */
    const val BlurRadius: Float = 15f

    /** 渐变曲线指数：1 = 线性；>1 让模糊更快衰减到清晰端。10 是实测值，别随手改 */
    const val GradientCurve: Float = 10f

    /** 顶栏 surface 混合透明度（0~1），越大栏越实。0.3 = 内容滚过去还能隐约看见 */
    const val SurfaceAlpha: Float = 0.3f

    /**
     * 滚动渐显距离（dp）：内容下滑该距离内，模糊从透明渐显到完整。
     * 0 = 顶栏常驻完整模糊（推荐，各页面顶部即可见模糊）。
     * 非 0 时只有滚动才出现模糊，静止时顶栏是透明的。
     */
    val ScrollFadeDistance: Dp = 0.dp
}

/**
 * 整屏唯一的一层 backdrop。
 *
 * **API 33 硬门槛**：RuntimeShader 从 T 开始才有。低于 33 或设备不支持时返回 null，
 * 所有 blurActive 自动为 false，界面退化成纯色 —— 这是设计好的降级，不是 bug。
 *
 * **整屏只允许一层**：多层 backdrop 各自采样会成倍消耗 GPU，且互相采样会出现递归模糊。
 * 底栏、顶栏都从这同一层采样。
 */
@Composable
fun rememberBlurBackdrop(): LayerBackdrop? {
    if (!isRuntimeShaderSupported()) return null
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

// isInDarkTheme() 只有一份，定义在 ui/theme/MiuixAppTheme.kt。
// 这里用 import 而不是再写一份：两份实现漂移会让同一页面出现两种深色判定，
// 状态栏图标、卡片底色、玻璃高光各挑各的档 —— 这正是「两份真相」的典型症状。

/**
 * 顶栏容器：在内容之上铺一层渐进模糊，然后把顶栏画在它上面。
 *
 * 为什么是 progressiveTextureBlur 而不是全屏 blur：只模糊顶栏那一块，并按渐变衰减到清晰，
 * 成本是整屏模糊的一小部分；全屏模糊会把整页每帧都过一遍 shader。
 */
@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurEnabled: Boolean,
    scrollBehavior: ScrollBehavior? = null,
    content: @Composable () -> Unit,
) {
    val blurActive = blurEnabled && backdrop != null
    val fadePx = with(LocalDensity.current) { TopBarBlurConfig.ScrollFadeDistance.toPx() }
    Box {
        if (blurActive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        // ScrollFadeDistance = 0 时常驻满强度；非 0 时按滚动偏移渐显
                        alpha = if (fadePx > 0f) {
                            scrollBehavior?.state
                                ?.let { (-it.contentOffset / fadePx).coerceIn(0f, 1f) }
                                ?: 1f
                        } else {
                            1f
                        }
                    }
                    .progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        // 渐变曲线是 ProgressiveBlur 自己的字段，不是 progressiveTextureBlur 的参数
                        gradient = ProgressiveBlur(
                            angle = 90f,
                            startFraction = 0f,
                            endFraction = 1f,
                            curve = TopBarBlurConfig.GradientCurve,
                        ),
                        blurRadius = TopBarBlurConfig.BlurRadius,
                        colors = BlurDefaults.blurColors(
                            blendColors = listOf(
                                BlendColorEntry(
                                    color = MiuixTheme.colorScheme.surface
                                        .copy(alpha = TopBarBlurConfig.SurfaceAlpha),
                                ),
                            ),
                        ),
                    ),
            )
        }
        content()
    }
}

/**
 * 是否该用分栏布局（平板 / 折叠屏展开 / 横向大窗）。
 *
 * 判据与 HyperMusicCover 一致：宽 ≥ 840dp，或 宽 ≥ 600dp 且宽高比 < 1.2（横向握持）。
 * 用它决定 DeviceType，进而决定背景动效的取景范围 —— 平板上一份手机比例的
 * 背景动效会被拉得很难看。
 */
@Composable
fun shouldShowSplitPane(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    return with(density) {
        val widthDp = windowInfo.containerSize.width.toDp()
        val heightDp = windowInfo.containerSize.height.toDp()
        val ratio = heightDp / widthDp
        widthDp >= 840.dp || (widthDp >= 600.dp && ratio < 1.2f)
    }
}

/** 底栏混合色：贴底用 surface，悬浮用 surfaceContainer，透明度按形态给 */
@Composable
fun barBlendColor(floating: Boolean): Color {
    val base = if (floating) MiuixTheme.colorScheme.surfaceContainer
    else MiuixTheme.colorScheme.surface
    val alpha = if (floating) 0.4f else 0.5f
    return remember(base, alpha) { base.copy(alpha = alpha) }
}
