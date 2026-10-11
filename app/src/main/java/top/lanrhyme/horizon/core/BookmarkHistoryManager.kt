package top.lanrhyme.horizon.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import top.lanrhyme.horizon.model.BookmarkItem
import top.lanrhyme.horizon.model.HistoryItem

object BookmarkHistoryManager {

    // 默认内置的常用技术与文档书签
    private val defaultBookmarks = listOf(
        BookmarkItem(
            title = "MDN Web 文档",
            url = "https://developer.mozilla.org",
            faviconUrl = "https://developer.mozilla.org/favicon-48x48.png"
        ),
        BookmarkItem(
            title = "GitHub",
            url = "https://github.com",
            faviconUrl = "https://github.githubassets.com/favicons/favicon.svg"
        ),
        BookmarkItem(
            title = "arXiv 预印本学术文献",
            url = "https://arxiv.org",
            faviconUrl = "https://arxiv.org/static/browse/0.3.4/images/arxiv-logo-one-color.svg"
        ),
        BookmarkItem(
            title = "维基百科 (Wikipedia)",
            url = "https://zh.wikipedia.org",
            faviconUrl = "https://zh.wikipedia.org/static/favicon/wikipedia.ico"
        ),
        BookmarkItem(
            title = "V2EX 开发者社区",
            url = "https://v2ex.com",
            faviconUrl = "https://v2ex.com/static/favicon.ico"
        )
    )

    private val _bookmarks = MutableStateFlow<List<BookmarkItem>>(defaultBookmarks)
    val bookmarks: StateFlow<List<BookmarkItem>> = _bookmarks.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    fun isBookmarked(url: String): Boolean {
        if (url.isBlank() || url == "horizon://newtab" || url == "about:blank") return false
        return _bookmarks.value.any { it.url == url }
    }

    fun toggleBookmark(title: String, url: String, faviconUrl: String? = null) {
        if (url.isBlank() || url == "horizon://newtab" || url == "about:blank") return
        if (isBookmarked(url)) {
            _bookmarks.update { list -> list.filter { it.url != url } }
        } else {
            val item = BookmarkItem(title = title.ifBlank { url }, url = url, faviconUrl = faviconUrl)
            _bookmarks.update { listOf(item) + it }
        }
    }

    fun removeBookmark(id: String) {
        _bookmarks.update { list -> list.filter { it.id != id } }
    }

    fun recordHistory(title: String, url: String, faviconUrl: String? = null) {
        if (url.isBlank() || url == "horizon://newtab" || url == "about:blank" || url.startsWith("view-source:")) return
        val newItem = HistoryItem(
            title = title.ifBlank { url },
            url = url,
            faviconUrl = faviconUrl
        )
        // 去重并将最新记录置顶
        _history.update { current ->
            listOf(newItem) + current.filter { it.url != url }.take(99)
        }
    }

    fun removeHistoryItem(id: String) {
        _history.update { list -> list.filter { it.id != id } }
    }

    fun clearHistory() {
        _history.value = emptyList()
    }
}
