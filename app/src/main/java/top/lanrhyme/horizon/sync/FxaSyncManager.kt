package top.lanrhyme.horizon.sync

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import top.lanrhyme.horizon.core.BookmarkHistoryManager
import top.lanrhyme.horizon.model.BookmarkItem
import top.lanrhyme.horizon.model.HistoryItem

data class RemoteTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val faviconUrl: String? = null,
    val lastActiveTime: Long = System.currentTimeMillis()
)

data class RemoteDevice(
    val id: String,
    val name: String,
    val deviceType: String = "desktop", // desktop, mobile, tablet
    val lastSyncTime: String,
    val tabs: List<RemoteTab> = emptyList()
)

data class SyncAccountState(
    val isLoggedIn: Boolean = false,
    val userEmail: String? = null,
    val avatarUrl: String? = null,
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    val syncBookmarksEnabled: Boolean = true,
    val syncHistoryEnabled: Boolean = true,
    val syncTabsEnabled: Boolean = true,
    val syncPasswordsEnabled: Boolean = true,
    val autoSyncEnabled: Boolean = true,
    val remoteDevices: List<RemoteDevice> = emptyList()
)

object FxaSyncManager {

    private const val PREFS_NAME = "horizon_fxa_sync"
    private const val KEY_SYNC_DATA = "fxa_account_data"
    private var prefs: SharedPreferences? = null

    private val defaultRemoteDevices = listOf(
        RemoteDevice(
            id = "device_pc",
            name = "Arch Linux Desktop",
            deviceType = "desktop",
            lastSyncTime = "10 分钟前",
            tabs = listOf(
                RemoteTab(
                    title = "ArchWiki - Niri Compositor Documentation",
                    url = "https://wiki.archlinux.org/title/Niri",
                    faviconUrl = "https://wiki.archlinux.org/favicon.ico"
                ),
                RemoteTab(
                    title = "GeckoView Javadoc API Reference",
                    url = "https://mozilla.github.io/geckoview/javadoc/mozilla-central/",
                    faviconUrl = "https://mozilla.org/favicon.ico"
                ),
                RemoteTab(
                    title = "Android NDK Guides & Toolchains",
                    url = "https://developer.android.com/ndk/guides",
                    faviconUrl = "https://developer.android.com/favicon.ico"
                )
            )
        ),
        RemoteDevice(
            id = "device_phone",
            name = "Redmi K60 (ColorOS 16)",
            deviceType = "mobile",
            lastSyncTime = "25 分钟前",
            tabs = listOf(
                RemoteTab(
                    title = "GitHub - Trending Repositories",
                    url = "https://github.com/trending",
                    faviconUrl = "https://github.githubassets.com/favicons/favicon.svg"
                ),
                RemoteTab(
                    title = "Hacker News: Newest Tech Submissions",
                    url = "https://news.ycombinator.com",
                    faviconUrl = "https://news.ycombinator.com/favicon.ico"
                )
            )
        )
    )

    private val _accountState = MutableStateFlow(
        SyncAccountState(
            isLoggedIn = false,
            userEmail = null,
            lastSyncTime = null
        )
    )
    val accountState: StateFlow<SyncAccountState> = _accountState.asStateFlow()

    private var autoSyncJob: kotlinx.coroutines.Job? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob())

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadAccountState()
        startAutoSyncSchedule()
    }

    private fun startAutoSyncSchedule() {
        autoSyncJob?.cancel()
        autoSyncJob = scope.launch {
            while (isActive) {
                // 每隔 15 分钟轻量轮询同步 (如已登录且开启自动同步)
                if (_accountState.value.isLoggedIn && _accountState.value.autoSyncEnabled) {
                    triggerSync()
                }
                kotlinx.coroutines.delay(15 * 60 * 1000L)
            }
        }
    }

    private fun loadAccountState() {
        val sp = prefs ?: return
        val raw = sp.getString(KEY_SYNC_DATA, null) ?: return
        try {
            val json = JSONObject(raw)
            val isLogged = json.optBoolean("isLoggedIn", false)
            _accountState.value = SyncAccountState(
                isLoggedIn = isLogged,
                userEmail = if (json.has("userEmail")) json.optString("userEmail") else null,
                avatarUrl = if (json.has("avatarUrl")) json.optString("avatarUrl") else null,
                lastSyncTime = if (json.has("lastSyncTime")) json.optString("lastSyncTime") else null,
                syncBookmarksEnabled = json.optBoolean("syncBookmarksEnabled", true),
                syncHistoryEnabled = json.optBoolean("syncHistoryEnabled", true),
                syncTabsEnabled = json.optBoolean("syncTabsEnabled", true),
                syncPasswordsEnabled = json.optBoolean("syncPasswordsEnabled", true),
                autoSyncEnabled = json.optBoolean("autoSyncEnabled", true),
                remoteDevices = if (isLogged) defaultRemoteDevices else emptyList()
            )
        } catch (_: Exception) {}
    }

    private fun saveAccountState() {
        val sp = prefs ?: return
        val state = _accountState.value
        val json = JSONObject().apply {
            put("isLoggedIn", state.isLoggedIn)
            if (state.userEmail != null) put("userEmail", state.userEmail)
            if (state.avatarUrl != null) put("avatarUrl", state.avatarUrl)
            if (state.lastSyncTime != null) put("lastSyncTime", state.lastSyncTime)
            put("syncBookmarksEnabled", state.syncBookmarksEnabled)
            put("syncHistoryEnabled", state.syncHistoryEnabled)
            put("syncTabsEnabled", state.syncTabsEnabled)
            put("syncPasswordsEnabled", state.syncPasswordsEnabled)
            put("autoSyncEnabled", state.autoSyncEnabled)
        }
        sp.edit().putString(KEY_SYNC_DATA, json.toString()).apply()
    }

    // 登录或关联 Firefox 账号
    fun login(email: String) {
        val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        _accountState.value = _accountState.value.copy(
            isLoggedIn = true,
            userEmail = email,
            lastSyncTime = now,
            remoteDevices = defaultRemoteDevices
        )
        saveAccountState()
        triggerSync()
    }

    fun logout() {
        _accountState.value = SyncAccountState()
        saveAccountState()
    }

    fun triggerSync() {
        if (!_accountState.value.isLoggedIn) return
        _accountState.value = _accountState.value.copy(isSyncing = true)

        kotlin.concurrent.thread {
            try {
                // 模拟与 Firefox Sync 云端协议数据合并
                Thread.sleep(1000)

                // 如果启用了书签同步，合并云端精选文档书签
                if (_accountState.value.syncBookmarksEnabled) {
                    val syncedBookmarks = listOf(
                        BookmarkItem(
                            title = "Rust 官方语言参考",
                            url = "https://doc.rust-lang.org/reference/",
                            faviconUrl = "https://www.rust-lang.org/static/images/favicon.svg"
                        ),
                        BookmarkItem(
                            title = "Kotlin 官方中文文档",
                            url = "https://kotlinlang.org/docs/home.html",
                            faviconUrl = "https://kotlinlang.org/assets/images/favicon.svg"
                        ),
                        BookmarkItem(
                            title = "Jetpack Compose 平板设计规范",
                            url = "https://developer.android.com/design/ui/mobile/guides/foundations/large-screens",
                            faviconUrl = "https://developer.android.com/favicon.ico"
                        )
                    )
                    syncedBookmarks.forEach { item ->
                        if (!BookmarkHistoryManager.isBookmarked(item.url)) {
                            BookmarkHistoryManager.toggleBookmark(item.title, item.url, item.faviconUrl)
                        }
                    }
                }

                val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                _accountState.value = _accountState.value.copy(
                    isSyncing = false,
                    lastSyncTime = now,
                    remoteDevices = defaultRemoteDevices
                )
                saveAccountState()
            } catch (_: Exception) {
                _accountState.value = _accountState.value.copy(isSyncing = false)
            }
        }
    }

    fun toggleSyncBookmarks(enabled: Boolean) {
        _accountState.value = _accountState.value.copy(syncBookmarksEnabled = enabled)
        saveAccountState()
    }

    fun toggleSyncHistory(enabled: Boolean) {
        _accountState.value = _accountState.value.copy(syncHistoryEnabled = enabled)
        saveAccountState()
    }

    fun toggleSyncTabs(enabled: Boolean) {
        _accountState.value = _accountState.value.copy(syncTabsEnabled = enabled)
        saveAccountState()
    }

    fun toggleSyncPasswords(enabled: Boolean) {
        _accountState.value = _accountState.value.copy(syncPasswordsEnabled = enabled)
        saveAccountState()
    }

    fun toggleAutoSync(enabled: Boolean) {
        _accountState.value = _accountState.value.copy(autoSyncEnabled = enabled)
        saveAccountState()
    }
}
