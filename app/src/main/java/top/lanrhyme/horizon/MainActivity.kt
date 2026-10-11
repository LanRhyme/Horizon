package top.lanrhyme.horizon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.lanrhyme.horizon.core.TabManager
import top.lanrhyme.horizon.extension.ExtensionManager
import top.lanrhyme.horizon.ui.components.*
import top.lanrhyme.horizon.ui.theme.MorandiPalette

class MainActivity : ComponentActivity() {

    private lateinit var tabManager: TabManager
    private lateinit var extensionManager: ExtensionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val runtime = (application as HorizonApp).geckoRuntime
        tabManager = TabManager(this, runtime)
        extensionManager = ExtensionManager(this, runtime)

        setContent {
            MaterialTheme {
                HorizonBrowserScreen(
                    tabManager = tabManager,
                    extensionManager = extensionManager
                )
            }
        }
    }
}

@Composable
fun HorizonBrowserScreen(
    tabManager: TabManager,
    extensionManager: ExtensionManager,
    modifier: Modifier = Modifier
) {
    var isSidebarExpanded by remember { mutableStateOf(true) }
    var isToolsDrawerOpen by remember { mutableStateOf(false) }
    var isSettingsDrawerOpen by remember { mutableStateOf(false) }
    var settingsCategory by remember { mutableStateOf(SettingsCategory.EXTENSIONS) }

    val spaces by tabManager.spaces.collectAsState()
    val currentSpaceId by tabManager.currentSpaceId.collectAsState()
    val tabs by tabManager.tabs.collectAsState()
    val activeTabId by tabManager.activeTabId.collectAsState()
    val secondaryTabId by tabManager.secondaryTabId.collectAsState()
    val currentSearchEngine by tabManager.searchEngine.collectAsState()
    val popupState by extensionManager.popupState.collectAsState()

    // 过滤出属于当前空间的标签列表
    val spaceTabs = remember(tabs, currentSpaceId) {
        tabs.filter { it.spaceId == currentSpaceId }
    }

    val activeTab = remember(tabs, activeTabId) {
        tabs.find { it.id == activeTabId }
    }

    val secondaryTab = remember(tabs, secondaryTabId) {
        tabs.find { it.id == secondaryTabId }
    }

    val primarySession = remember(activeTabId) {
        activeTabId?.let { tabManager.getOrCreateSession(it) }
    }

    val secondarySession = remember(secondaryTabId) {
        secondaryTabId?.let { tabManager.getOrCreateSession(it) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MorandiPalette.ScaffoldBg)
            .systemBarsPadding()
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 左侧：Arc / Zen 风格一体化侧边栏
            TabletSidebar(
                isExpanded = isSidebarExpanded,
                onToggleExpand = { isSidebarExpanded = !isSidebarExpanded },
                spaces = spaces,
                currentSpaceId = currentSpaceId,
                onSelectSpace = { tabManager.switchSpace(it) },
                tabs = spaceTabs,
                activeTabId = activeTabId,
                onSelectTab = { tabManager.selectTab(it) },
                onCloseTab = { tabManager.closeTab(it) },
                onNewTab = { tabManager.createNewTab() },
                onTogglePin = { tabManager.togglePinTab(it) },
                onOpenExtensions = {
                    settingsCategory = SettingsCategory.EXTENSIONS
                    isSettingsDrawerOpen = true
                    isToolsDrawerOpen = false
                },
                onOpenTools = {
                    isToolsDrawerOpen = !isToolsDrawerOpen
                    isSettingsDrawerOpen = false
                },
                onOpenSettings = {
                    settingsCategory = SettingsCategory.APPEARANCE
                    isSettingsDrawerOpen = true
                    isToolsDrawerOpen = false
                }
            )

            // 右侧：一体化主浏览器窗口卡片 (Window Canvas)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 2.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MorandiPalette.Border),
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    // 融入窗口顶部的工具栏
                    BrowserToolbar(
                        activeTab = activeTab,
                        isSplitViewActive = secondaryTabId != null,
                        onNavigateUrl = { url -> activeTabId?.let { tabManager.loadUrl(it, url) } },
                        onBack = { activeTabId?.let { tabManager.goBack(it) } },
                        onForward = { activeTabId?.let { tabManager.goForward(it) } },
                        onRefresh = { activeTabId?.let { tabManager.reload(it) } },
                        onToggleSplitView = { tabManager.toggleSplitView() },
                        onTogglePin = { activeTabId?.let { tabManager.togglePinTab(it) } },
                        onToggleDesktopMode = { activeTabId?.let { tabManager.toggleDesktopMode(it) } },
                        onOpenTools = {
                            isToolsDrawerOpen = !isToolsDrawerOpen
                            isSettingsDrawerOpen = false
                        },
                        onOpenSyncSettings = {
                            settingsCategory = SettingsCategory.SYNC_ABOUT
                            isSettingsDrawerOpen = true
                            isToolsDrawerOpen = false
                        }
                    )

                    // 网页视口 (单屏 / Split View 双开分屏)
                    WebViewport(
                        primarySession = primarySession,
                        primaryTab = activeTab,
                        secondarySession = secondarySession,
                        secondaryTab = secondaryTab,
                        isSplitView = secondaryTabId != null,
                        onCloseSecondary = { tabManager.toggleSplitView() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 抽屉展开时的半透明 Scrim 遮罩
        val isAnyDrawerOpen = isToolsDrawerOpen || isSettingsDrawerOpen
        AnimatedVisibility(
            visible = isAnyDrawerOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable {
                        isToolsDrawerOpen = false
                        isSettingsDrawerOpen = false
                    }
            )
        }

        // 工具箱右侧抽屉面板
        ToolsDrawer(
            isOpen = isToolsDrawerOpen,
            onClose = { isToolsDrawerOpen = false },
            activeTab = activeTab,
            onToggleDesktopMode = { activeTabId?.let { tabManager.toggleDesktopMode(it) } },
            onViewSource = { activeTabId?.let { tabManager.viewSource(it) } },
            onClearData = { tabManager.clearBrowsingData() },
            onNewTab = { tabManager.createNewTab() },
            onOpenExtensions = {
                settingsCategory = SettingsCategory.EXTENSIONS
                isSettingsDrawerOpen = true
                isToolsDrawerOpen = false
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // 设置中心抽屉面板 (外观/搜索隐私/扩展/同步与关于)
        SettingsDrawer(
            isOpen = isSettingsDrawerOpen,
            onClose = { isSettingsDrawerOpen = false },
            currentEngine = currentSearchEngine,
            onSelectEngine = { tabManager.setSearchEngine(it) },
            extensionManager = extensionManager,
            initialCategory = settingsCategory,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // WebExtension 扩展 Popup 交互弹窗
        ExtensionPopupDialog(
            popupState = popupState,
            onDismiss = { extensionManager.closePopup() }
        )
    }
}
