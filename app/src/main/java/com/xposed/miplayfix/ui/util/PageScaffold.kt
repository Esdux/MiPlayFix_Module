package com.xposed.miplayfix.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import com.xposed.miplayfix.ui.util.BlurredBar

/**
 * 每页统一骨架：顶栏（渐进模糊）+ 正确的 insets + 一个 LazyColumn。
 *
 * **backdrop 归本函数所有，不由调用方传。** 这里有个反直觉的点，踩过才知道：
 * backdrop 是「采样源」，`layerBackdrop` 是「把某层内容注册进这个采样源」。
 * 注册层里**绝不能包含**用它做模糊的组件 —— 否则那个组件在自己要采样的画面里，
 * 采样源指向自己，渲染树无限递归，libhwui 递归 500+ 层后 SIGSEGV（native crash，
 * 栈顶是 SkiaDisplayList::prepareListAndChildren）。
 *
 * 所以本函数只把 `layerBackdrop` 注册在包住列表的 Box 上，**不包 topBar**：
 * 顶栏在采样源之外，才能采到下面滚动的内容。同理，应用壳那份 backdrop
 * 只注册在 pager 区域，不包底栏（底栏在 Scaffold 的 bottomBar 里，天然在外面）。
 *
 * 两层 backdrop 各采各的，是原版 vcut_lower / HyperMusicCover 的结构；
 * 合成一层反而必然递归。
 *
 * 原注释（vcut_lower）：这段代码在成为函数之前被复制过六次，副本开始漂移 ——
 * 有一页把顶部 inset 放到了列表自己的 modifier 上，列表被压到栏下，
 * backdrop 采不到内容，那页的顶栏看起来就跟别的页不一样。
 */
@Composable
fun PageScaffold(
    title: String,
    isBlurEnabled: Boolean,
    extraBottomPadding: Dp = 0.dp,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    // 本页自己的一份采样源：只覆盖列表区域，不含顶栏
    val backdrop = rememberBlurBackdrop()
    val blurActive = isBlurEnabled && backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    Scaffold(
        popupHost = { },
        floatingActionButton = floatingActionButton,
        topBar = {
            BlurredBar(backdrop = backdrop, blurEnabled = blurActive, scrollBehavior = scrollBehavior) {
                TopAppBar(
                    title = title,
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = { if (onBack != null) PageBackButton(onBack) },
                    actions = { actions() },
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        // 只注册列表这一层，且必须排除 topBar —— 顶栏要采样它，不能在它里面。
        // 见函数头注释：注册层包含采样者 = 自采样递归 = native SIGSEGV。
        Box(modifier = if (blurActive) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                overscrollEffect = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + extraBottomPadding,
                ),
                content = content,
            )
        }
    }
}


@Composable
private fun PageBackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        val layoutDirection = LocalLayoutDirection.current
        Icon(
            modifier = Modifier.graphicsLayer {
                if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
            },
            imageVector = MiuixIcons.Back,
            contentDescription = "返回",
            tint = MiuixTheme.colorScheme.onBackground,
        )
    }
}

/**
 * 与 [PageScaffold] 同一副骨架，但内容是一个 `Column` 而不是 `LazyListScope`。
 *
 * 什么时候用哪个：
 * - 内容是「一串卡片、整页一起滚」→ [PageScaffold]（LazyColumn）。
 * - 内容是「自己会滚的东西」——带搜索栏的列表、双栏、地图、相机预览 → 本函数。
 *   这类页面在 LazyColumn 里放一个 `weight(1f)` 的列表，外层滚不动、内层也滚不动。
 *
 * 除了内容槽位，其余（insets、模糊、backdrop 归属、顶栏采样关系）**逐字相同**，
 * 因为它们解决的是同一批坑：insets 算错列表会被压到栏下，backdrop 包住顶栏会递归崩溃。
 * 复制一份改，就一定会漏掉其中一条。
 */
@Composable
fun PageScaffoldColumn(
    title: String,
    isBlurEnabled: Boolean,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = isBlurEnabled && backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    Scaffold(
        popupHost = { },
        topBar = {
            BlurredBar(backdrop = backdrop, blurEnabled = blurActive, scrollBehavior = scrollBehavior) {
                TopAppBar(
                    title = title,
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = { if (onBack != null) PageBackButton(onBack) },
                    actions = { actions() },
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        // 同样只注册内容层，不包 topBar —— 见 [PageScaffold] 的函数头注释
        Box(modifier = if (blurActive) Modifier.layerBackdrop(backdrop) else Modifier) {
            content(innerPadding)
        }
    }
}
