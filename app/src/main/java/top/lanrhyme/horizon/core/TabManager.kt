package top.lanrhyme.horizon.core

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import top.lanrhyme.horizon.model.BrowserSpace
import top.lanrhyme.horizon.model.BrowserTab

class TabManager(
    private val context: Context,
    private val geckoRuntime: GeckoRuntime
) : ViewModel() {

    // 默认工作空间 Spaces
    val defaultSpaces = listOf(
        BrowserSpace(id = "space_general", name = "日常", accentColorHex = 0xFF5B7A9C),
        BrowserSpace(id = "space_work", name = "开发", accentColorHex = 0xFF6E8B7E),
        BrowserSpace(id = "space_reading", name = "阅读", accentColorHex = 0xFFA86F77)
    )

    private val _spaces = MutableStateFlow(defaultSpaces)
    val spaces: StateFlow<List<BrowserSpace>> = _spaces.asStateFlow()

    private val _currentSpaceId = MutableStateFlow("space_general")
    val currentSpaceId: StateFlow<String> = _currentSpaceId.asStateFlow()

    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    // 分屏右侧活动标签 ID (为 null 表示单屏全宽)
    private val _secondaryTabId = MutableStateFlow<String?>(null)
    val secondaryTabId: StateFlow<String?> = _secondaryTabId.asStateFlow()

    // 缓存 GeckoSession 实例，避免重复重建与销毁
    private val sessionMap = mutableMapOf<String, GeckoSession>()

    init {
        // 创建初始默认标签
        val initialTab = createNewTab(
            spaceId = "space_general",
            url = "https://cn.bing.com",
            title = "必应搜索"
        )
        _activeTabId.value = initialTab.id
    }

    fun getOrCreateSession(tabId: String): GeckoSession {
        return sessionMap.getOrPut(tabId) {
            GeckoSession().apply {
                val tab = _tabs.value.find { it.id == tabId }
                open(geckoRuntime)

                // 监听进度与加载状态
                progressDelegate = object : GeckoSession.ProgressDelegate {
                    override fun onPageStart(session: GeckoSession, url: String) {
                        updateTab(tabId) { it.copy(url = url, isLoading = true, progress = 10) }
                    }

                    override fun onPageStop(session: GeckoSession, success: Boolean) {
                        updateTab(tabId) { it.copy(isLoading = false, progress = 100) }
                    }

                    override fun onProgressChange(session: GeckoSession, progress: Int) {
                        updateTab(tabId) { it.copy(progress = progress) }
                    }
                }

                // 监听标题与导航状态
                navigationDelegate = object : GeckoSession.NavigationDelegate {
                    override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                        updateTab(tabId) { it.copy(canGoBack = canGoBack) }
                    }

                    override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                        updateTab(tabId) { it.copy(canGoForward = canGoForward) }
                    }
                }

                contentDelegate = object : GeckoSession.ContentDelegate {
                    override fun onTitleChange(session: GeckoSession, title: String?) {
                        if (!title.isNullOrBlank()) {
                            updateTab(tabId) { it.copy(title = title) }
                        }
                    }
                }

                if (tab != null && tab.url.isNotBlank() && tab.url != "about:blank") {
                    loadUri(tab.url)
                }
            }
        }
    }

    fun switchSpace(spaceId: String) {
        _currentSpaceId.value = spaceId
        val spaceTabs = _tabs.value.filter { it.spaceId == spaceId }
        if (spaceTabs.isNotEmpty() && (activeTab.value == null || activeTab.value?.spaceId != spaceId)) {
            _activeTabId.value = spaceTabs.first().id
        }
    }

    fun selectTab(tabId: String) {
        _activeTabId.value = tabId
    }

    fun createNewTab(
        spaceId: String = _currentSpaceId.value,
        url: String = "https://cn.bing.com",
        title: String = "新标签页"
    ): BrowserTab {
        val newTab = BrowserTab(
            spaceId = spaceId,
            url = url,
            title = title
        )
        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
        return newTab
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        val index = currentTabs.indexOfFirst { it.id == tabId }
        if (index == -1) return

        sessionMap.remove(tabId)?.close()

        val updatedTabs = currentTabs.filter { it.id != tabId }
        _tabs.value = updatedTabs

        if (_activeTabId.value == tabId) {
            val spaceTabs = updatedTabs.filter { it.spaceId == _currentSpaceId.value }
            _activeTabId.value = when {
                spaceTabs.isNotEmpty() -> spaceTabs.getOrNull(index.coerceAtMost(spaceTabs.size - 1))?.id
                updatedTabs.isNotEmpty() -> updatedTabs.first().id
                else -> {
                    val freshTab = createNewTab()
                    freshTab.id
                }
            }
        }

        if (_secondaryTabId.value == tabId) {
            _secondaryTabId.value = null
        }
    }

    fun togglePinTab(tabId: String) {
        updateTab(tabId) { it.copy(isPinned = !it.isPinned) }
    }

    fun toggleSplitView(tabId: String? = null) {
        if (_secondaryTabId.value != null) {
            _secondaryTabId.value = null
        } else {
            val candidate = tabId ?: _tabs.value.firstOrNull { it.id != _activeTabId.value && it.spaceId == _currentSpaceId.value }?.id
            _secondaryTabId.value = candidate
        }
    }

    fun loadUrl(tabId: String, rawInput: String) {
        val trimmed = rawInput.trim()
        val finalUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://cn.bing.com/search?q=" + java.net.URLEncoder.encode(trimmed, "UTF-8")
        }

        val session = getOrCreateSession(tabId)
        session.loadUri(finalUrl)
        updateTab(tabId) { it.copy(url = finalUrl, title = trimmed) }
    }

    fun goBack(tabId: String) {
        sessionMap[tabId]?.goBack()
    }

    fun goForward(tabId: String) {
        sessionMap[tabId]?.goForward()
    }

    fun reload(tabId: String) {
        sessionMap[tabId]?.reload()
    }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == tabId) transform(tab) else tab
            }
        }
    }

    val activeTab: StateFlow<BrowserTab?> = MutableStateFlow<BrowserTab?>(null).apply {
        viewModelScope.launch {
            _tabs.collect { tabsList ->
                val activeId = _activeTabId.value
                value = tabsList.find { it.id == activeId }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionMap.values.forEach { it.close() }
        sessionMap.clear()
    }
}
