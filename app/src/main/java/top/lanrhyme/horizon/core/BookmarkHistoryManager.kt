package top.lanrhyme.horizon.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import top.lanrhyme.horizon.model.BookmarkItem
import top.lanrhyme.horizon.model.HistoryItem

object BookmarkHistoryManager {

    private const val PREFS_NAME = "horizon_browser_data"
    private const val KEY_BOOKMARKS = "saved_bookmarks"
    private const val KEY_HISTORY = "saved_history"

    private var prefs: SharedPreferences? = null

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

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromStorage()
    }

    private fun loadFromStorage() {
        val sp = prefs ?: return

        // 读取书签
        val bookmarksJson = sp.getString(KEY_BOOKMARKS, null)
        if (!bookmarksJson.isNullOrBlank()) {
            try {
                val array = JSONArray(bookmarksJson)
                val list = mutableListOf<BookmarkItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        BookmarkItem(
                            id = obj.optString("id"),
                            title = obj.optString("title"),
                            url = obj.optString("url"),
                            faviconUrl = if (obj.has("faviconUrl")) obj.optString("faviconUrl") else null,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    _bookmarks.value = list
                }
            } catch (_: Exception) {}
        }

        // 读取历史
        val historyJson = sp.getString(KEY_HISTORY, null)
        if (!historyJson.isNullOrBlank()) {
            try {
                val array = JSONArray(historyJson)
                val list = mutableListOf<HistoryItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        HistoryItem(
                            id = obj.optString("id"),
                            title = obj.optString("title"),
                            url = obj.optString("url"),
                            faviconUrl = if (obj.has("faviconUrl")) obj.optString("faviconUrl") else null,
                            visitedAt = obj.optLong("visitedAt", System.currentTimeMillis())
                        )
                    )
                }
                _history.value = list
            } catch (_: Exception) {}
        }
    }

    private fun saveBookmarks() {
        val sp = prefs ?: return
        val array = JSONArray()
        _bookmarks.value.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("url", item.url)
                if (item.faviconUrl != null) put("faviconUrl", item.faviconUrl)
                put("createdAt", item.createdAt)
            }
            array.put(obj)
        }
        sp.edit().putString(KEY_BOOKMARKS, array.toString()).apply()
    }

    private fun saveHistory() {
        val sp = prefs ?: return
        val array = JSONArray()
        _history.value.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("url", item.url)
                if (item.faviconUrl != null) put("faviconUrl", item.faviconUrl)
                put("visitedAt", item.visitedAt)
            }
            array.put(obj)
        }
        sp.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

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
        saveBookmarks()
    }

    fun removeBookmark(id: String) {
        _bookmarks.update { list -> list.filter { it.id != id } }
        saveBookmarks()
    }

    fun recordHistory(title: String, url: String, faviconUrl: String? = null) {
        if (url.isBlank() || url == "horizon://newtab" || url == "about:blank" || url.startsWith("view-source:")) return
        val newItem = HistoryItem(
            title = title.ifBlank { url },
            url = url,
            faviconUrl = faviconUrl
        )
        // 去重并将最新记录置顶，保留最近 200 条
        _history.update { current ->
            listOf(newItem) + current.filter { it.url != url }.take(199)
        }
        saveHistory()
    }

    fun removeHistoryItem(id: String) {
        _history.update { list -> list.filter { it.id != id } }
        saveHistory()
    }

    fun clearHistory() {
        _history.value = emptyList()
        saveHistory()
    }
}
