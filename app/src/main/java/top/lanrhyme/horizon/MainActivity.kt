package top.lanrhyme.horizon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.lanrhyme.horizon.core.TabManager
import top.lanrhyme.horizon.ui.components.BrowserToolbar
import top.lanrhyme.horizon.ui.components.SyncSettingsDialog
import top.lanrhyme.horizon.ui.components.TabletSidebar
import top.lanrhyme.horizon.ui.components.WebViewport
import top.lanrhyme.horizon.ui.theme.MorandiPalette

class MainActivity : ComponentActivity() {

    private lateinit var tabManager: TabManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val runtime = (application as HorizonApp).geckoRuntime
        tabManager = TabManager(this, runtime)

        setContent {
            MaterialTheme {
                HorizonBrowserScreen(tabManager = tabManager)
            }
        }
    }
}

@Composable
fun HorizonBrowserScreen(
    tabManager: TabManager,
    modifier: Modifier = Modifier
) {
    var isSidebarExpanded by remember { mutableStateOf(true) }
    var showSyncDialog by remember { mutableStateOf(false) }

    val spaces by tabManager.spaces.collectAsState()
    val currentSpaceId by tabManager.currentSpaceId.collectAsState()
    val tabs by tabManager.tabs.collectAsState()
    val activeTabId by tabManager.activeTabId.collectAsState()
    val secondaryTabId by tabManager.secondaryTabId.collectAsState()

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

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MorandiPalette.Background)
            .systemBarsPadding()
    ) {
        // Zen / Arc 风格折叠侧边栏
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
            onTogglePin = { tabManager.togglePinTab(it) }
        )

        // 右侧主体：顶栏 + 网页视口
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            BrowserToolbar(
                activeTab = activeTab,
                isSplitViewActive = secondaryTabId != null,
                onNavigateUrl = { url -> activeTabId?.let { tabManager.loadUrl(it, url) } },
                onBack = { activeTabId?.let { tabManager.goBack(it) } },
                onForward = { activeTabId?.let { tabManager.goForward(it) } },
                onRefresh = { activeTabId?.let { tabManager.reload(it) } },
                onToggleSplitView = { tabManager.toggleSplitView() },
                onTogglePin = { activeTabId?.let { tabManager.togglePinTab(it) } },
                onOpenSyncSettings = { showSyncDialog = true }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 网页视口 (支持单开 / Split View 双开分屏)
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

    // Firefox 同步配置弹窗
    if (showSyncDialog) {
        SyncSettingsDialog(onDismiss = { showSyncDialog = false })
    }
}
