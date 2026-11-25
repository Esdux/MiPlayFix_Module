package com.xposed.miplayfix.ui

import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xposed.miplayfix.ui.screens.AboutPage
import com.xposed.miplayfix.ui.screens.FeaturesPage
import com.xposed.miplayfix.ui.screens.HomePage
import com.xposed.miplayfix.ui.screens.SettingsPage
import com.xposed.miplayfix.ui.theme.MiuixAppTheme
import com.xposed.miplayfix.ui.theme.isInDarkTheme
import com.xposed.miplayfix.ui.util.PagerNavigationSpringSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 一个标签的定义。
 *
 * 把标签做成数据而不是散在 when(page) 里，是因为标签数量与顺序会随产品变
 * （工具型四标签、内容型三标签、媒体型三标签）。散着写时改一次标签要动五处，
 * 而且很容易漏掉底栏图标与返回键的对应关系。
 *
 * content 收 (isBlurEnabled, bottomPadding)：前者透传给 PageScaffold 决定顶栏要不要模糊，
 * 后者让每个页面自己决定塞进 extraBottomPadding 还是忽略（末页常不需要）。
 */
data class AppTab(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val content: @Composable (isBlurEnabled: Boolean, bottomPadding: Dp) -> Unit,
)

/**
 * 默认四标签：主页 → 功能 → 设置 → 关于。
 *
 * 顺序是硬规则（使用频率 × 首次理解成本）：主页永远第一，设置倒数第二，关于永远最后。
 * 判定依据见 references/information-architecture.md。换产品形态时只改这个列表，
 * 不要改 AppShellScreen 里的逻辑。
 */
@Composable
fun defaultTabs(): List<AppTab> = listOf(
    AppTab("home", "主页", MiuixIcons.Home) { blur, pad -> HomePage(isBlurEnabled = blur, extraBottomPadding = pad) },
    AppTab("features", "功能", MiuixIcons.Tune) { blur, pad -> FeaturesPage(isBlurEnabled = blur, extraBottomPadding = pad) },
    AppTab("settings", "设置", MiuixIcons.Settings) { blur, pad -> SettingsPage(isBlurEnabled = blur, extraBottomPadding = pad) },
    AppTab("about", "关于", MiuixIcons.Info) { blur, pad -> AboutPage(isBlurEnabled = blur, extraBottomPadding = pad) },
)

/**
 * 四标签应用壳 —— 从 vcut_lower / HyperMusicCover 的 MainActivity 提炼。
 *
 * 三件事必须照抄，它们是踩过坑的结论：
 *
 * 1. **主题在 onStop 应用，不在设置改变时应用。** 切换深浅色是一次 configuration change，
 *    Activity 会被重建；在设置页里当场应用，用户会看到开关跳一下、页面闪一下。
 *    挪到 onStop 做，结果一样（下次冷启动的启动图颜色是对的），但没人在看。
 *
 * 2. **onResume 让 settingsEpoch 自增。** 主题页是独立 Activity，它在
 *    本 Activity stopped 期间保存自己的改动；而本页持有的设置是 onCreate 里读的。
 *    从主题页回来不重读，界面就停在你离开时的主题上，直到下次冷启动。
 *
 * 3. **点底栏用弹簧滚过去，不用 animateScrollToPage。** 后者是线性缓动，跨多页时像被
 *    拖着走；PagerNavigationSpringSpec 让页面像被弹过去，与 HyperOS 系统的分页一致。
 *
 * 底栏两种形态（navBarMode）由设置决定：0 = 贴底实心、1 = 悬浮。
 * 两种形态共用应用壳这一份 backdrop，它只注册在 pager 区域、不包底栏 ——
 * 注册层不能包含采样者，否则自采样递归。页面各有一份自己的 backdrop 给顶栏用，
 * 两层互不相干，见 references/performance-and-caching.md。
 *
 * **曾有第三种「液态玻璃」，已删除。** 它不是实现不了，而是那个开关是假的：
 * 打开后只是模糊半径 25 → 32 加一道描边，用户看不出区别，却要多背一个设置项、
 * 一条 FoldUnder 折叠逻辑、一份 539 行的自建底栏组件。拨了跟没拨一样的开关，
 * 比没有这个开关更糟 —— 它会让人怀疑其它开关也是假的。
 */
class AppShellActivity : ComponentActivity() {

    /** 每次回到前台自增，作为 setContent 里 remember 的 key，让设置跟着重读 */
    private var settingsEpoch by mutableStateOf(0)

    override fun onResume() {
        super.onResume()
        settingsEpoch++
    }

    override fun onStop() {
        super.onStop()
        // 只在这里应用夜间模式：它会重建 Activity，别让用户看着它发生
        AppSettings.applyNightMode(this)
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        // 配置通道必须在这里建立：少了这一步页面读到的永远是默认值
        RemoteConfig.attach(this)

        val saved = AppSettings.load(this)
        // 冷启动白闪三件套之一：先用上次绘制到的那层颜色涂窗口。
        // surfaceArgb 需要 Context，且首次启动返回 null —— 那时还没画过任何一帧。
        AppSettings.surfaceArgb(this)?.let { window.setBackgroundDrawable(ColorDrawable(it)) }
        AppSettings.applyNightMode(this)

        setContent {
            val settings = remember(settingsEpoch) { AppSettings.load(this@AppShellActivity) }
            val themeMode = remember(settings) {
                runCatching { ColorSchemeMode.valueOf(settings.themeMode) }
                    .getOrDefault(ColorSchemeMode.System)
            }
            MiuixAppTheme(themeMode = themeMode) {
                val surface = MiuixTheme.colorScheme.surface
                LaunchedEffect(surface, themeMode) {
                    AppSettings.rememberSurface(this@AppShellActivity, themeMode.name, surface.toArgb())
                }
                AppShellScreen(
                    tabs = defaultTabs(),
                    isFloatingNavbar = settings.isFloatingNavbar,
                    isBlurEnabled = settings.isBlurEnabled,
                )
            }
        }
    }
}

/**
 * 应用壳本体。tabs 由调用方给，壳只负责翻页、底栏、返回键与 backdrop。
 *
 * 硬约束：
 * - tabs.size 必须是 3–5；超出说明层级没设计好，应该拆二级页。
 * - tabs[0] 必须是首页；返回键契约假设「不在首页就回首页」。
 */
@Composable
fun AppShellScreen(
    tabs: List<AppTab>,
    isFloatingNavbar: Boolean,
    isBlurEnabled: Boolean,
) {
    require(tabs.size in 3..5) { "标签页数量必须在 3-5 之间" }

    val scope = rememberCoroutineScope()
    val pageCount = tabs.size
    val pagerState = rememberPagerState(pageCount = { pageCount })
    var selectedIndex by remember { mutableIntStateOf(0) }
    var isNavigating by remember { mutableStateOf(false) }
    var navJob by remember { mutableStateOf<Job?>(null) }

    // 末页常有逐帧动画：把「是不是当前页」做成 lambda 传下去，而不是把 currentPage
    // 传下去，否则翻页时所有页一起重组
    val onLastPage = remember(pageCount) {
        derivedStateOf { pagerState.currentPage == pageCount - 1 }
    }
    val isLastPageCurrent: () -> Boolean = remember(onLastPage) { { onLastPage.value } }

    // 手指拖动改变页码时同步底栏选中态；程序滚动期间不回写，避免与 goToPage 抢
    LaunchedEffect(pagerState.currentPage) {
        if (!isNavigating && selectedIndex != pagerState.currentPage) {
            selectedIndex = pagerState.currentPage
        }
    }

    val surfaceColor = MiuixTheme.colorScheme.surface
    // 整屏唯一的一层 backdrop：底栏的模糊都从这里采样
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val navBarMode = if (isFloatingNavbar) 1 else 0

    fun goToPage(index: Int) {
        if (index == selectedIndex) return
        navJob?.cancel()
        selectedIndex = index
        isNavigating = true
        navJob = scope.launch {
            val myJob = coroutineContext.job
            try {
                pagerState.scroll(MutatePriority.UserInput) {
                    val info = pagerState.layoutInfo
                    val pageSize = info.pageSize + info.pageSpacing
                    val distanceInPages =
                        index - pagerState.currentPage - pagerState.currentPageOffsetFraction
                    val scrollPixels = distanceInPages * pageSize
                    var previous = 0f
                    animate(
                        initialValue = 0f,
                        targetValue = scrollPixels,
                        animationSpec = PagerNavigationSpringSpec,
                    ) { value, _ ->
                        previous += scrollBy(value - previous)
                    }
                }
                if (pagerState.currentPage != index) pagerState.scrollToPage(index)
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != index) selectedIndex = pagerState.currentPage
                }
            }
        }
    }

    // 返回键契约：不在首页就回首页，首页交给系统（退出）
    BackHandler(enabled = selectedIndex != 0) { goToPage(0) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                mode = navBarMode,
                tabs = tabs,
                selectedIndex = selectedIndex,
                backdrop = backdrop,
                blurActive = isBlurEnabled,
                onItemSelected = { goToPage(it) },
            )
        },
    ) { globalPadding ->
        val navBarHeight = globalPadding.calculateBottomPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isBlurEnabled) Modifier.layerBackdrop(backdrop) else Modifier)
                .background(surfaceColor),
        ) {
            HorizontalPager(
                state = pagerState,
                // 1 = 只保留左右各一页，翻页手感与内存的折中；不要用默认 0（翻页时白屏）
                beyondViewportPageCount = 1,
                // 必须关掉：miuix 的阻尼回弹自己接管，两者叠加会在两端打架
                overscrollEffect = null,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                // 每页只拿到「要不要模糊」和底栏高度。页面内部的顶栏采样源由
                // PageScaffold 自己建 —— 采样源必须与被采样内容在同一层里，
                // 从外面传进来就跨层了。
                tabs[page].content(isBlurEnabled, navBarHeight)
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    mode: Int,
    tabs: List<AppTab>,
    selectedIndex: Int,
    backdrop: LayerBackdrop?,
    blurActive: Boolean,
    onItemSelected: (Int) -> Unit,
) {
    when (mode) {
        // 悬浮：FloatingNavigationBar + textureBlur
        1 -> {
            val barColor =
                if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer
            val shape = RoundedCornerShape(FloatingToolbarDefaults.CornerRadius)
            val isDark = isInDarkTheme()
            val highlight = remember(isDark) {
                if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
            }
            FloatingNavigationBar(
                modifier = if (blurActive && backdrop != null) {
                    Modifier.textureBlur(
                        backdrop = backdrop,
                        shape = shape,
                        blurRadius = 25f,
                        colors = BlurDefaults.blurColors(
                            blendColors = listOf(
                                BlendColorEntry(
                                    color = MiuixTheme.colorScheme.surfaceContainer.copy(0.4f),
                                ),
                            ),
                        ),
                        highlight = highlight,
                    )
                } else {
                    Modifier
                },
                color = barColor,
            ) {
                tabs.forEachIndexed { index, tab ->
                    FloatingNavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { onItemSelected(index) },
                        icon = tab.icon,
                        label = tab.label,
                        enabled = true,
                    )
                }
            }
        }

        // 贴底实心（默认）：模糊时条本身透明，模糊由 textureBlur 提供
        else -> {
            val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
            Box(
                modifier = Modifier
                    .then(
                        if (blurActive && backdrop != null) {
                            Modifier.textureBlur(
                                backdrop = backdrop,
                                shape = RectangleShape,
                                blurRadius = 25f,
                                colors = BlurDefaults.blurColors(
                                    blendColors = listOf(
                                        BlendColorEntry(
                                            color = MiuixTheme.colorScheme.surface.copy(0.5f),
                                        ),
                                    ),
                                ),
                            )
                        } else {
                            Modifier
                        }
                    )
                    .background(barColor),
            ) {
                NavigationBar(color = barColor) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = { onItemSelected(index) },
                            icon = tab.icon,
                            label = tab.label,
                            enabled = true,
                        )
                    }
                }
            }
        }
    }
}