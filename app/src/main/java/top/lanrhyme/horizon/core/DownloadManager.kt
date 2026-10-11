package top.lanrhyme.horizon.core

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import top.lanrhyme.horizon.model.DownloadItem
import top.lanrhyme.horizon.model.DownloadStatus
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object DownloadManager {

    private const val PREFS_NAME = "horizon_downloads"
    private const val KEY_DOWNLOADS = "saved_downloads"

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromStorage()
    }

    private fun loadFromStorage() {
        val sp = prefs ?: return
        val raw = sp.getString(KEY_DOWNLOADS, null) ?: return
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<DownloadItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DownloadItem(
                        id = obj.getString("id"),
                        url = obj.getString("url"),
                        fileName = obj.getString("fileName"),
                        contentLength = obj.optLong("contentLength", -1L),
                        downloadedBytes = obj.optLong("downloadedBytes", 0L),
                        progress = obj.optInt("progress", 0),
                        status = try {
                            DownloadStatus.valueOf(obj.getString("status"))
                        } catch (_: Exception) {
                            DownloadStatus.FAILED
                        },
                        filePath = if (obj.has("filePath")) obj.optString("filePath") else null,
                        mimeType = if (obj.has("mimeType")) obj.optString("mimeType") else null,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            _downloads.value = list
        } catch (_: Exception) {}
    }

    private fun persistDownloads() {
        val sp = prefs ?: return
        val array = JSONArray()
        _downloads.value.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("url", item.url)
                put("fileName", item.fileName)
                put("contentLength", item.contentLength)
                put("downloadedBytes", item.downloadedBytes)
                put("progress", item.progress)
                put("status", item.status.name)
                if (item.filePath != null) put("filePath", item.filePath)
                if (item.mimeType != null) put("mimeType", item.mimeType)
                put("createdAt", item.createdAt)
            }
            array.put(obj)
        }
        sp.edit().putString(KEY_DOWNLOADS, array.toString()).apply()
    }

    fun startDownload(
        url: String,
        suggestedFileName: String? = null,
        mimeType: String? = null,
        responseStream: InputStream? = null,
        contentLength: Long = -1L
    ) {
        val context = appContext ?: return
        val fileName = sanitizeFileName(suggestedFileName ?: extractFileNameFromUrl(url))
        val downloadItem = DownloadItem(
            url = url,
            fileName = fileName,
            contentLength = contentLength,
            mimeType = mimeType,
            status = DownloadStatus.DOWNLOADING
        )

        _downloads.update { listOf(downloadItem) + it }
        persistDownloads()

        Toast.makeText(context, "开始下载: $fileName", Toast.LENGTH_SHORT).show()

        scope.launch {
            try {
                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) 
                    ?: File(context.filesDir, "downloads").apply { mkdirs() }
                var targetFile = File(downloadDir, fileName)
                var counter = 1
                while (targetFile.exists()) {
                    val dotIndex = fileName.lastIndexOf('.')
                    val nameWithoutExt = if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
                    val ext = if (dotIndex > 0) fileName.substring(dotIndex) else ""
                    targetFile = File(downloadDir, "${nameWithoutExt}_$counter$ext")
                    counter++
                }

                if (responseStream != null) {
                    saveStreamToFile(responseStream, targetFile, downloadItem.id, contentLength)
                } else {
                    downloadViaHttp(url, targetFile, downloadItem.id)
                }

                _downloads.update { list ->
                    list.map {
                        if (it.id == downloadItem.id) {
                            it.copy(
                                status = DownloadStatus.COMPLETED,
                                progress = 100,
                                filePath = targetFile.absolutePath,
                                downloadedBytes = targetFile.length()
                            )
                        } else it
                    }
                }
                persistDownloads()

                launch(Dispatchers.Main) {
                    Toast.makeText(context, "下载完成: ${targetFile.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                _downloads.update { list ->
                    list.map {
                        if (it.id == downloadItem.id) {
                            it.copy(status = DownloadStatus.FAILED)
                        } else it
                    }
                }
                persistDownloads()

                launch(Dispatchers.Main) {
                    Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveStreamToFile(stream: InputStream, targetFile: File, itemId: String, totalLen: Long) {
        stream.use { input ->
            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                var lastUpdate = System.currentTimeMillis()

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 300) {
                        lastUpdate = now
                        val prog = if (totalLen > 0) ((totalRead * 100) / totalLen).toInt().coerceIn(0, 100) else 0
                        _downloads.update { list ->
                            list.map {
                                if (it.id == itemId) it.copy(downloadedBytes = totalRead, progress = prog) else it
                            }
                        }
                    }
                }
                output.flush()
            }
        }
    }

    private fun downloadViaHttp(urlStr: String, targetFile: File, itemId: String) {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.instanceFollowRedirects = true
        conn.connect()

        val totalLen = conn.contentLength.toLong()
        _downloads.update { list ->
            list.map {
                if (it.id == itemId) it.copy(contentLength = totalLen) else it
            }
        }

        conn.inputStream.use { input ->
            saveStreamToFile(input, targetFile, itemId, totalLen)
        }
    }

    fun openDownloadedFile(item: DownloadItem) {
        val context = appContext ?: return
        val path = item.filePath ?: return
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, "文件不存在或已被删除", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType ?: getMimeType(file.name))
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "无法打开此类型文件", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteDownload(item: DownloadItem) {
        if (!item.filePath.isNullOrBlank()) {
            try {
                File(item.filePath).delete()
            } catch (_: Exception) {}
        }
        _downloads.update { list -> list.filter { it.id != item.id } }
        persistDownloads()
    }

    private fun extractFileNameFromUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            val lastSegment = uri.lastPathSegment
            if (!lastSegment.isNullOrBlank()) lastSegment else "download_${System.currentTimeMillis()}"
        } catch (_: Exception) {
            "download_${System.currentTimeMillis()}"
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "apk" -> "application/vnd.android.package-archive"
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "zip" -> "application/zip"
            "txt" -> "text/plain"
            "html" -> "text/html"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            else -> "*/*"
        }
    }
}
