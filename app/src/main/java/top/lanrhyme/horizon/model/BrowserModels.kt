package top.lanrhyme.horizon.model

import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val spaceId: String,
    val title: String = "新标签页",
    val url: String = "about:blank",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val isPinned: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false
)

data class BrowserSpace(
    val id: String,
    val name: String,
    val iconName: String = "Default",
    val accentColorHex: Long = 0xFF5B7083
)
