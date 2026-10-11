package top.lanrhyme.horizon.core

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import top.lanrhyme.horizon.model.BrowserSpace
import top.lanrhyme.horizon.model.BrowserTab

class TabManager(
    private val context: Context,
    private val geckoRuntime: GeckoRuntime
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("horizon_tabs_spaces", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SPACES = "saved_spaces"
        private const val KEY_TABS = "saved_tabs"
        private const val KEY_SEARCH_ENGINE = "saved_search_engine"
        private const val KEY_ACTIVE_TAB_ID = "saved_active_tab_id"
        private const val KEY_CURRENT_SPACE_ID = "saved_current_space_id"
        private const val KEY_TRACKING_PROTECTION = "saved_tracking_protection"
    }

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

    // 搜索引擎管理 (默认 Bing)
    private val _searchEngine = MutableStateFlow("Bing")
    val searchEngine: StateFlow<String> = _searchEngine.asStateFlow()

    // 强化跟踪保护开关 (Enhanced Tracking Protection)
    private val _trackingProtection = MutableStateFlow(true)
    val trackingProtection: StateFlow<Boolean> = _trackingProtection.asStateFlow()

    // 页内查找状态 (Find in Page)
    private val _findInPageState = MutableStateFlow(top.lanrhyme.horizon.model.FindInPageState())
    val findInPageState: StateFlow<top.lanrhyme.horizon.model.FindInPageState> = _findInPageState.asStateFlow()

    // 缓存 GeckoSession 实例，避免重复重建与销毁
    private val sessionMap = mutableMapOf<String, GeckoSession>()

    init {
        loadPersistedState()
    }

    private fun loadPersistedState() {
        // 读取搜索引擎
        val savedEngine = prefs.getString(KEY_SEARCH_ENGINE, "Bing") ?: "Bing"
        _searchEngine.value = savedEngine

        // 读取防跟踪开关
        val savedTracking = prefs.getBoolean(KEY_TRACKING_PROTECTION, true)
        _trackingProtection.value = savedTracking

        // 读取 Spaces
        val spacesJson = prefs.getString(KEY_SPACES, null)
        if (!spacesJson.isNullOrBlank()) {
            try {
                val array = JSONArray(spacesJson)
                val list = mutableListOf<BrowserSpace>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        BrowserSpace(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            iconName = obj.optString("iconName", "Default"),
                            accentColorHex = obj.optLong("accentColorHex", 0xFF5B7083)
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    _spaces.value = list
                }
            } catch (_: Exception) {}
        }

        val savedSpaceId = prefs.getString(KEY_CURRENT_SPACE_ID, "space_general") ?: "space_general"
        if (_spaces.value.any { it.id == savedSpaceId }) {
            _currentSpaceId.value = savedSpaceId
        }

        // 读取 Tabs
        val tabsJson = prefs.getString(KEY_TABS, null)
        val loadedTabs = mutableListOf<BrowserTab>()
        if (!tabsJson.isNullOrBlank()) {
            try {
                val array = JSONArray(tabsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    loadedTabs.add(
                        BrowserTab(
                            id = obj.getString("id"),
                            spaceId = obj.getString("spaceId"),
                            title = obj.optString("title", "新标签页"),
                            url = obj.optString("url", "horizon://newtab"),
                            isPinned = obj.optBoolean("isPinned", false),
                            isDesktopMode = obj.optBoolean("isDesktopMode", false),
                            faviconUrl = if (obj.has("faviconUrl")) obj.optString("faviconUrl") else null,
                            isReaderMode = obj.optBoolean("isReaderMode", false)
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        if (loadedTabs.isNotEmpty()) {
            _tabs.value = loadedTabs
            val savedActiveId = prefs.getString(KEY_ACTIVE_TAB_ID, null)
            _activeTabId.value = if (loadedTabs.any { it.id == savedActiveId }) savedActiveId else loadedTabs.first().id
        } else {
            // 没有已恢复的标签，创建初始标签
            val initialTab = createNewTab(
                spaceId = _currentSpaceId.value,
                url = "horizon://newtab",
                title = "新标签页"
            )
            _activeTabId.value = initialTab.id
        }
    }

    private fun persistTabs() {
        val array = JSONArray()
        _tabs.value.forEach { tab ->
            val obj = JSONObject().apply {
                put("id", tab.id)
                put("spaceId", tab.spaceId)
                put("title", tab.title)
                put("url", tab.url)
                put("isPinned", tab.isPinned)
                put("isDesktopMode", tab.isDesktopMode)
                if (tab.faviconUrl != null) put("faviconUrl", tab.faviconUrl)
                put("isReaderMode", tab.isReaderMode)
            }
            array.put(obj)
        }
        prefs.edit()
            .putString(KEY_TABS, array.toString())
            .putString(KEY_ACTIVE_TAB_ID, _activeTabId.value)
            .apply()
    }

    private fun persistSpaces() {
        val array = JSONArray()
        _spaces.value.forEach { space ->
            val obj = JSONObject().apply {
                put("id", space.id)
                put("name", space.name)
                put("iconName", space.iconName)
                put("accentColorHex", space.accentColorHex)
            }
            array.put(obj)
        }
        prefs.edit()
            .putString(KEY_SPACES, array.toString())
            .putString(KEY_CURRENT_SPACE_ID, _currentSpaceId.value)
            .apply()
    }

    private fun extractFaviconUrl(rawUrl: String): String? {
        return try {
            val uri = android.net.Uri.parse(rawUrl)
            val host = uri.host
            if (!host.isNullOrBlank()) {
                "https://www.google.com/s2/favicons?sz=64&domain_url=https://$host"
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun getOrCreateSession(tabId: String): GeckoSession {
        return sessionMap.getOrPut(tabId) {
            GeckoSession().apply {
                val tab = _tabs.value.find { it.id == tabId }
                settings.useTrackingProtection = _trackingProtection.value
                open(geckoRuntime)

                // 监听进度与加载状态
                progressDelegate = object : GeckoSession.ProgressDelegate {
                    override fun onPageStart(session: GeckoSession, url: String) {
                        val currentTab = _tabs.value.find { it.id == tabId }
                        // 当标签处于原生起始仪表盘 (horizon://newtab) 时，忽略 GeckoSession 内部初始化的 about:blank
                        if (currentTab?.url == "horizon://newtab" && url == "about:blank") {
                            return
                        }
                        val fav = extractFaviconUrl(url)
                        updateTab(tabId) { it.copy(url = url, isLoading = true, progress = 10, faviconUrl = fav) }
                    }

                    override fun onPageStop(session: GeckoSession, success: Boolean) {
                        val currentTab = _tabs.value.find { it.id == tabId }
                        val fav = currentTab?.url?.let { extractFaviconUrl(it) }
                        updateTab(tabId) { it.copy(isLoading = false, progress = 100, faviconUrl = fav) }

                        // 记录到浏览历史
                        if (currentTab != null && currentTab.url.isNotBlank() && currentTab.url != "horizon://newtab") {
                            BookmarkHistoryManager.recordHistory(
                                title = currentTab.title,
                                url = currentTab.url,
                                faviconUrl = fav
                            )
                        }
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

                    override fun onExternalResponse(session: GeckoSession, response: org.mozilla.geckoview.WebResponse) {
                        val disposition = response.headers["content-disposition"]
                        val filename = if (!disposition.isNullOrBlank() && disposition.contains("filename=")) {
                            disposition.substringAfter("filename=").trim('"', ' ', ';')
                        } else null
                        DownloadManager.startDownload(
                            url = response.uri,
                            suggestedFileName = filename,
                            mimeType = response.headers["content-type"],
                            responseStream = response.body
                        )
                    }
                }

                if (tab != null && tab.url.isNotBlank() && tab.url != "about:blank" && tab.url != "horizon://newtab") {
                    loadUri(tab.url)
                }
            }
        }
    }

    fun switchSpace(spaceId: String) {
        _currentSpaceId.value = spaceId
        prefs.edit().putString(KEY_CURRENT_SPACE_ID, spaceId).apply()
        val spaceTabs = _tabs.value.filter { it.spaceId == spaceId }
        if (spaceTabs.isNotEmpty() && (activeTab.value == null || activeTab.value?.spaceId != spaceId)) {
            _activeTabId.value = spaceTabs.first().id
            prefs.edit().putString(KEY_ACTIVE_TAB_ID, spaceTabs.first().id).apply()
        }
    }

    fun addCustomSpace(name: String, colorHex: Long) {
        val newSpace = BrowserSpace(name = name, accentColorHex = colorHex)
        _spaces.update { it + newSpace }
        _currentSpaceId.value = newSpace.id
        persistSpaces()
    }

    fun moveTabToSpace(tabId: String, newSpaceId: String) {
        updateTab(tabId) { it.copy(spaceId = newSpaceId) }
    }

    fun closeOtherTabs(tabId: String) {
        val currentSpaceTabs = _tabs.value.filter { it.spaceId == _currentSpaceId.value }
        currentSpaceTabs.forEach {
            if (it.id != tabId && !it.isPinned) {
                closeTab(it.id)
            }
        }
    }

    fun toggleReaderMode(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return
        val newReaderState = !tab.isReaderMode
        val session = sessionMap[tabId]
        if (session != null) {
            val script = """
                javascript:(function(){
                    var id = 'horizon-reader-style';
                    var el = document.getElementById(id);
                    if (el) {
                        el.remove();
                    } else {
                        var style = document.createElement('style');
                        style.id = id;
                        style.textContent = `
                            body {
                                max-width: 820px !important;
                                margin: 0 auto !important;
                                padding: 40px 24px !important;
                                font-size: 19px !important;
                                line-height: 1.85 !important;
                                background: #FAF9F6 !important;
                                color: #2B2D42 !important;
                            }
                            aside, [class*="sidebar"], [class*="ad-"], [class*="advertisement"], [id*="ad-"], header:not(article header), footer {
                                display: none !important;
                            }
                            img { max-width: 100% !important; border-radius: 8px !important; }
                        `;
                        document.head.appendChild(style);
                    }
                })();
            """.trimIndent().replace("\n", "")
            session.loadUri(script)
        }
        updateTab(tabId) { it.copy(isReaderMode = newReaderState) }
    }

    fun selectTab(tabId: String) {
        _activeTabId.value = tabId
        prefs.edit().putString(KEY_ACTIVE_TAB_ID, tabId).apply()
    }

    fun createNewTab(
        spaceId: String = _currentSpaceId.value,
        url: String = "horizon://newtab",
        title: String = "新标签页"
    ): BrowserTab {
        val newTab = BrowserTab(
            spaceId = spaceId,
            url = url,
            title = title
        )
        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
        persistTabs()
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
        persistTabs()
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

    fun setSearchEngine(engine: String) {
        _searchEngine.value = engine
        prefs.edit().putString(KEY_SEARCH_ENGINE, engine).apply()
    }

    fun setTrackingProtection(enabled: Boolean) {
        _trackingProtection.value = enabled
        prefs.edit().putBoolean(KEY_TRACKING_PROTECTION, enabled).apply()
        // 动态更新已打开会话的防跟踪策略
        sessionMap.values.forEach { session ->
            session.settings.useTrackingProtection = enabled
        }
    }

    fun toggleDesktopMode(tabId: String) {
        val session = sessionMap[tabId] ?: return
        val isDesktop = session.settings.userAgentMode == org.mozilla.geckoview.GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        val newMode = if (isDesktop) {
            org.mozilla.geckoview.GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        } else {
            org.mozilla.geckoview.GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        }
        val newVp = if (isDesktop) {
            org.mozilla.geckoview.GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        } else {
            org.mozilla.geckoview.GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
        }
        session.settings.userAgentMode = newMode
        session.settings.viewportMode = newVp
        session.reload()
        updateTab(tabId) { it.copy(isDesktopMode = !isDesktop) }
    }

    fun viewSource(tabId: String) {
        val currentTab = _tabs.value.find { it.id == tabId } ?: return
        val currentUrl = currentTab.url
        if (currentUrl.isNotBlank() && !currentUrl.startsWith("view-source:")) {
            createNewTab(
                spaceId = currentTab.spaceId,
                url = "view-source:$currentUrl",
                title = "源码: ${currentTab.title}"
            )
        }
    }

    fun clearBrowsingData(onCompleted: () -> Unit = {}) {
        geckoRuntime.storageController.clearData(
            org.mozilla.geckoview.StorageController.ClearFlags.ALL
        ).accept({
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onCompleted()
            }
        }, {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onCompleted()
            }
        })
    }

    fun loadUrl(tabId: String, rawInput: String) {
        val trimmed = rawInput.trim()
        val finalUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("view-source:") || trimmed.startsWith("about:") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> {
                val query = java.net.URLEncoder.encode(trimmed, "UTF-8")
                when (_searchEngine.value) {
                    "Google" -> "https://www.google.com/search?q=$query"
                    "DuckDuckGo" -> "https://duckduckgo.com/?q=$query"
                    "Baidu" -> "https://www.baidu.com/s?wd=$query"
                    else -> "https://cn.bing.com/search?q=$query"
                }
            }
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
        persistTabs()
    }

    val activeTab: StateFlow<BrowserTab?> = MutableStateFlow<BrowserTab?>(null).apply {
        viewModelScope.launch {
            _tabs.collect { tabsList ->
                val activeId = _activeTabId.value
                value = tabsList.find { it.id == activeId }
            }
        }
    }

    // 页内查找控制
    fun openFindInPage() {
        _findInPageState.update { it.copy(isActive = true) }
    }

    fun closeFindInPage() {
        val activeId = _activeTabId.value
        if (activeId != null) {
            sessionMap[activeId]?.finder?.clear()
        }
        _findInPageState.value = top.lanrhyme.horizon.model.FindInPageState(isActive = false)
    }

    fun findText(query: String, forward: Boolean = true) {
        val activeId = _activeTabId.value ?: return
        val session = sessionMap[activeId] ?: return
        _findInPageState.update { it.copy(query = query) }

        if (query.isBlank()) {
            session.finder.clear()
            _findInPageState.update { it.copy(currentMatch = 0, totalMatches = 0) }
            return
        }

        val flags = if (forward) {
            GeckoSession.FINDER_FIND_FORWARD or GeckoSession.FINDER_DISPLAY_HIGHLIGHT_ALL
        } else {
            GeckoSession.FINDER_FIND_BACKWARDS or GeckoSession.FINDER_DISPLAY_HIGHLIGHT_ALL
        }

        session.finder.find(query, flags).then({ result ->
            if (result != null) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    _findInPageState.update {
                        it.copy(
                            currentMatch = result.current,
                            totalMatches = result.total,
                            wrapped = result.wrapped
                        )
                    }
                }
            }
            org.mozilla.geckoview.GeckoResult.fromValue(result)
        }, { error ->
            org.mozilla.geckoview.GeckoResult.fromException<GeckoSession.FinderResult>(error)
        })
    }

    fun findNext() {
        val q = _findInPageState.value.query
        if (q.isNotBlank()) {
            findText(q, forward = true)
        }
    }

    fun findPrevious() {
        val q = _findInPageState.value.query
        if (q.isNotBlank()) {
            findText(q, forward = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionMap.values.forEach { it.close() }
        sessionMap.clear()
    }
}
