package top.lanrhyme.horizon.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SyncAccountState(
    val isLoggedIn: Boolean = false,
    val userEmail: String? = null,
    val avatarUrl: String? = null,
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    val syncBookmarksEnabled: Boolean = true,
    val syncHistoryEnabled: Boolean = true,
    val syncTabsEnabled: Boolean = true,
    val syncPasswordsEnabled: Boolean = true
)

object FxaSyncManager {

    private val _accountState = MutableStateFlow(
        SyncAccountState(
            isLoggedIn = false,
            userEmail = null,
            lastSyncTime = null
        )
    )
    val accountState: StateFlow<SyncAccountState> = _accountState.asStateFlow()

    // 登录或关联 Firefox 账号
    fun login(email: String) {
        _accountState.value = _accountState.value.copy(
            isLoggedIn = true,
            userEmail = email,
            lastSyncTime = "刚刚"
        )
    }

    fun logout() {
        _accountState.value = SyncAccountState()
    }

    fun triggerSync() {
        if (!_accountState.value.isLoggedIn) return
        _accountState.value = _accountState.value.copy(isSyncing = true)
        // 模拟同步流程
        kotlin.concurrent.thread {
            Thread.sleep(1500)
            _accountState.value = _accountState.value.copy(
                isSyncing = false,
                lastSyncTime = "刚刚"
            )
        }
    }

    fun toggleSyncBookmarks(enabled: Boolean) {
        _accountState.value = _accountState.value.copy(syncBookmarksEnabled = enabled)
    }

    fun toggleSyncTabs(enabled: Boolean) {
        _accountState.value = _accountState.value.copy(syncTabsEnabled = enabled)
    }

    fun toggleSyncHistory(enabled: Boolean) {
        _accountState.value = _accountState.value.copy(syncHistoryEnabled = enabled)
    }
}
