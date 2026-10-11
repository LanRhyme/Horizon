package top.lanrhyme.horizon.model

import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val spaceId: String,
    val title: String = "新标签页",
    val url: String = "horizon://newtab",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val isPinned: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isDesktopMode: Boolean = false,
    val faviconUrl: String? = null,
    val isReaderMode: Boolean = false
)

data class BrowserSpace(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconName: String = "Default",
    val accentColorHex: Long = 0xFF5B7083
)

data class BookmarkItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val faviconUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val faviconUrl: String? = null,
    val visitedAt: Long = System.currentTimeMillis()
)

data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val fileName: String,
    val contentLength: Long = -1L,
    val downloadedBytes: Long = 0L,
    val progress: Int = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val filePath: String? = null,
    val mimeType: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class FindInPageState(
    val isActive: Boolean = false,
    val query: String = "",
    val currentMatch: Int = 0,
    val totalMatches: Int = 0,
    val wrapped: Boolean = false
)
