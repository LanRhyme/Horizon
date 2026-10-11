package top.lanrhyme.horizon.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import top.lanrhyme.horizon.core.BookmarkHistoryManager
import top.lanrhyme.horizon.core.DownloadManager
import top.lanrhyme.horizon.model.DownloadItem
import top.lanrhyme.horizon.model.DownloadStatus
import top.lanrhyme.horizon.ui.theme.MorandiPalette
import java.text.SimpleDateFormat
import java.util.*

enum class LibraryTab {
    BOOKMARKS, HISTORY, DOWNLOADS, SYNCED_TABS
}

@Composable
fun BookmarksHistoryDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
    initialTab: LibraryTab = LibraryTab.BOOKMARKS,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    val bookmarks by BookmarkHistoryManager.bookmarks.collectAsState()
    val history by BookmarkHistoryManager.history.collectAsState()
    val downloads by DownloadManager.downloads.collectAsState()
    val syncState by top.lanrhyme.horizon.sync.FxaSyncManager.accountState.collectAsState()

    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .width(420.dp)
                .fillMaxHeight()
                .padding(top = 8.dp, bottom = 8.dp, end = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MorandiPalette.SurfaceElevated,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶栏切换
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .background(MorandiPalette.SurfaceSubtle, RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = LibraryTab.BOOKMARKS },
                            color = if (selectedTab == LibraryTab.BOOKMARKS) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == LibraryTab.BOOKMARKS) 1.dp else 0.dp
                        ) {
                            Text(
                                text = "书签 (${bookmarks.size})",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (selectedTab == LibraryTab.BOOKMARKS) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == LibraryTab.BOOKMARKS) MorandiPalette.AccentBlue else MorandiPalette.TextSecondary
                                )
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = LibraryTab.HISTORY },
                            color = if (selectedTab == LibraryTab.HISTORY) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == LibraryTab.HISTORY) 1.dp else 0.dp
                        ) {
                            Text(
                                text = "历史",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (selectedTab == LibraryTab.HISTORY) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == LibraryTab.HISTORY) MorandiPalette.AccentBlue else MorandiPalette.TextSecondary
                                )
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = LibraryTab.DOWNLOADS },
                            color = if (selectedTab == LibraryTab.DOWNLOADS) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == LibraryTab.DOWNLOADS) 1.dp else 0.dp
                        ) {
                            Text(
                                text = "下载",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (selectedTab == LibraryTab.DOWNLOADS) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == LibraryTab.DOWNLOADS) MorandiPalette.AccentBlue else MorandiPalette.TextSecondary
                                )
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = LibraryTab.SYNCED_TABS },
                            color = if (selectedTab == LibraryTab.SYNCED_TABS) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == LibraryTab.SYNCED_TABS) 1.dp else 0.dp
                        ) {
                            Text(
                                text = "多端标签",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (selectedTab == LibraryTab.SYNCED_TABS) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == LibraryTab.SYNCED_TABS) MorandiPalette.AccentBlue else MorandiPalette.TextSecondary
                                )
                            )
                        }
                    }

                    IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = MorandiPalette.TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                HorizontalDivider(color = MorandiPalette.BorderSubtle, thickness = 0.5.dp)

                // 列表内容
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    when (selectedTab) {
                        LibraryTab.BOOKMARKS -> {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            var showImportDialog by remember { mutableStateOf(false) }
                            var importInputText by remember { mutableStateOf("") }

                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "共 ${bookmarks.size} 项书签",
                                        fontSize = 11.sp,
                                        color = MorandiPalette.TextMuted
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = { showImportDialog = true },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(13.dp), tint = MorandiPalette.AccentBlue)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("导入", color = MorandiPalette.AccentBlue, fontSize = 11.sp)
                                        }
                                        TextButton(
                                            onClick = {
                                                val json = BookmarkHistoryManager.exportBookmarksJson()
                                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("Horizon Bookmarks", json)
                                                clipboard?.setPrimaryClip(clip)
                                                android.widget.Toast.makeText(context, "书签已复制到剪贴板 (JSON)", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(13.dp), tint = MorandiPalette.TextSecondary)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("导出", color = MorandiPalette.TextSecondary, fontSize = 11.sp)
                                        }
                                    }
                                }

                                if (showImportDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showImportDialog = false },
                                        title = { Text("导入书签 (JSON)") },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("粘贴导出的书签 JSON 数据：", fontSize = 12.sp, color = MorandiPalette.TextSecondary)
                                                OutlinedTextField(
                                                    value = importInputText,
                                                    onValueChange = { importInputText = it },
                                                    modifier = Modifier.fillMaxWidth().height(140.dp),
                                                    placeholder = { Text("[{\"title\":\"...\",\"url\":\"...\"}]", fontSize = 11.sp) }
                                                )
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                if (importInputText.isNotBlank()) {
                                                    val count = BookmarkHistoryManager.importBookmarksJson(importInputText)
                                                    if (count >= 0) {
                                                        android.widget.Toast.makeText(context, "成功导入 $count 项新书签", android.widget.Toast.LENGTH_SHORT).show()
                                                        showImportDialog = false
                                                        importInputText = ""
                                                    } else {
                                                        android.widget.Toast.makeText(context, "解析失败，请检查 JSON 格式", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }) {
                                                Text("确认导入", color = MorandiPalette.AccentBlue)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showImportDialog = false }) {
                                                Text("取消", color = MorandiPalette.TextSecondary)
                                            }
                                        },
                                        containerColor = MorandiPalette.SurfaceElevated,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }

                                if (bookmarks.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("尚未添加任何书签", color = MorandiPalette.TextMuted, fontSize = 12.sp)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(bookmarks, key = { it.id }) { item ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        onNavigate(item.url)
                                                        onClose()
                                                    },
                                                shape = RoundedCornerShape(10.dp),
                                                color = MorandiPalette.SurfaceSubtle,
                                                border = BorderStroke(0.5.dp, MorandiPalette.BorderSubtle)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(MorandiPalette.AccentBlue.copy(alpha = 0.1f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        AsyncImage(
                                                            model = item.faviconUrl ?: "https://www.google.com/s2/favicons?domain_url=${item.url}",
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(10.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = item.title,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Medium
                                                            ),
                                                            color = MorandiPalette.TextPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = item.url,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                            color = MorandiPalette.TextMuted,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { BookmarkHistoryManager.toggleBookmark(item.title, item.url) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteOutline,
                                                            contentDescription = "删除书签",
                                                            tint = MorandiPalette.TextMuted,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        LibraryTab.HISTORY -> {
                            if (history.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("暂无浏览历史", color = MorandiPalette.TextMuted, fontSize = 12.sp)
                                }
                            } else {
                                val todayStart = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                val yesterdayStart = todayStart - 86400000L

                                val groupedHistory = remember(history) {
                                    val map = linkedMapOf<String, MutableList<top.lanrhyme.horizon.model.HistoryItem>>()
                                    history.forEach { item ->
                                        val label = when {
                                            item.visitedAt >= todayStart -> "今天"
                                            item.visitedAt >= yesterdayStart -> "昨天"
                                            else -> "更早"
                                        }
                                        map.getOrPut(label) { mutableListOf() }.add(item)
                                    }
                                    map
                                }

                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "共 ${history.size} 条历史",
                                            fontSize = 11.sp,
                                            color = MorandiPalette.TextMuted
                                        )
                                        TextButton(
                                            onClick = { BookmarkHistoryManager.clearHistory() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("清空历史", color = MorandiPalette.AccentRose, fontSize = 11.sp)
                                        }
                                    }

                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        groupedHistory.forEach { (groupLabel, itemsInGroup) ->
                                            item(key = "header_$groupLabel") {
                                                Text(
                                                    text = groupLabel,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MorandiPalette.AccentBlue,
                                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                                                )
                                            }

                                            items(itemsInGroup, key = { it.id }) { item ->
                                                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.visitedAt))
                                                val isToday = item.visitedAt >= todayStart
                                                val dateStr = if (isToday) timeStr else SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(item.visitedAt))

                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            onNavigate(item.url)
                                                            onClose()
                                                        },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MorandiPalette.SurfaceSubtle,
                                                    border = BorderStroke(0.5.dp, MorandiPalette.BorderSubtle)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        AsyncImage(
                                                            model = item.faviconUrl ?: "https://www.google.com/s2/favicons?domain_url=${item.url}",
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )

                                                        Spacer(modifier = Modifier.width(10.dp))

                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = item.title,
                                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                ),
                                                                color = MorandiPalette.TextPrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "$dateStr · ${item.url}",
                                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                                color = MorandiPalette.TextMuted,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = { BookmarkHistoryManager.removeHistoryItem(item.id) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "移除",
                                                                tint = MorandiPalette.TextMuted,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        LibraryTab.DOWNLOADS -> {
                            if (downloads.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("暂无下载任务", color = MorandiPalette.TextMuted, fontSize = 12.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(downloads, key = { it.id }) { item ->
                                        DownloadRowItem(
                                            item = item,
                                            onOpen = { DownloadManager.openDownloadedFile(item) },
                                            onDelete = { DownloadManager.deleteDownload(item) }
                                        )
                                    }
                                }
                            }
                        }

                        LibraryTab.SYNCED_TABS -> {
                            if (!syncState.isLoggedIn) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SyncProblem,
                                            contentDescription = null,
                                            tint = MorandiPalette.TextMuted,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text("尚未登录 Firefox 账户", color = MorandiPalette.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("在设置 > 同步与关于中关联账号，即可浏览 PC 或手机上的实时标签页。", color = MorandiPalette.TextMuted, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            } else if (syncState.remoteDevices.isEmpty() || syncState.remoteDevices.all { it.tabs.isEmpty() }) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("暂无其他设备同步的标签页", color = MorandiPalette.TextMuted, fontSize = 12.sp)
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "已连接 ${syncState.remoteDevices.size} 台设备",
                                            fontSize = 11.sp,
                                            color = MorandiPalette.TextMuted
                                        )
                                        TextButton(
                                            onClick = { top.lanrhyme.horizon.sync.FxaSyncManager.triggerSync() },
                                            enabled = !syncState.isSyncing,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp), tint = MorandiPalette.AccentBlue)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(if (syncState.isSyncing) "同步中..." else "刷新", color = MorandiPalette.AccentBlue, fontSize = 11.sp)
                                        }
                                    }

                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        syncState.remoteDevices.forEach { device ->
                                            item(key = "device_${device.id}") {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 4.dp, bottom = 2.dp, start = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = when (device.deviceType) {
                                                            "mobile" -> Icons.Default.Smartphone
                                                            "tablet" -> Icons.Default.Tablet
                                                            else -> Icons.Default.Laptop
                                                        },
                                                        contentDescription = null,
                                                        tint = MorandiPalette.AccentBlue,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "${device.name} · ${device.lastSyncTime}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MorandiPalette.TextSecondary
                                                    )
                                                }
                                            }

                                            items(device.tabs, key = { "${device.id}_${it.id}" }) { tab ->
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            onNavigate(tab.url)
                                                            onClose()
                                                        },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MorandiPalette.SurfaceSubtle,
                                                    border = BorderStroke(0.5.dp, MorandiPalette.BorderSubtle)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        AsyncImage(
                                                            model = tab.faviconUrl ?: "https://www.google.com/s2/favicons?domain_url=${tab.url}",
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )

                                                        Spacer(modifier = Modifier.width(10.dp))

                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = tab.title,
                                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                ),
                                                                color = MorandiPalette.TextPrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = tab.url,
                                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                                color = MorandiPalette.TextMuted,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                onNavigate(tab.url)
                                                                onClose()
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.OpenInNew,
                                                                contentDescription = "打开",
                                                                tint = MorandiPalette.AccentBlue,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadRowItem(
    item: DownloadItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = item.status == DownloadStatus.COMPLETED) { onOpen() },
        shape = RoundedCornerShape(10.dp),
        color = MorandiPalette.SurfaceSubtle,
        border = BorderStroke(0.5.dp, MorandiPalette.BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when (item.status) {
                                DownloadStatus.COMPLETED -> MorandiPalette.AccentSage.copy(alpha = 0.15f)
                                DownloadStatus.DOWNLOADING -> MorandiPalette.AccentBlue.copy(alpha = 0.15f)
                                else -> MorandiPalette.AccentRose.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.status) {
                            DownloadStatus.COMPLETED -> Icons.Default.FileDownloadDone
                            DownloadStatus.DOWNLOADING -> Icons.Default.Download
                            else -> Icons.Default.ErrorOutline
                        },
                        contentDescription = null,
                        tint = when (item.status) {
                            DownloadStatus.COMPLETED -> MorandiPalette.AccentSage
                            DownloadStatus.DOWNLOADING -> MorandiPalette.AccentBlue
                            else -> MorandiPalette.AccentRose
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MorandiPalette.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val statusDesc = when (item.status) {
                        DownloadStatus.COMPLETED -> "下载完成 · ${formatFileSize(item.downloadedBytes)}"
                        DownloadStatus.DOWNLOADING -> "正在下载... ${item.progress}%"
                        DownloadStatus.FAILED -> "下载失败"
                        DownloadStatus.CANCELLED -> "已取消"
                        DownloadStatus.PENDING -> "等待中"
                    }
                    Text(
                        text = statusDesc,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MorandiPalette.TextMuted
                    )
                }

                if (item.status == DownloadStatus.COMPLETED) {
                    IconButton(onClick = onOpen, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "打开", tint = MorandiPalette.AccentBlue, modifier = Modifier.size(14.dp))
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "删除", tint = MorandiPalette.TextMuted, modifier = Modifier.size(14.dp))
                }
            }

            if (item.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { item.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp)),
                    color = MorandiPalette.AccentBlue,
                    trackColor = MorandiPalette.BorderSubtle
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups >= units.size) digitGroups = units.size - 1
    val df = java.text.DecimalFormat("#,##0.#")
    return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}
