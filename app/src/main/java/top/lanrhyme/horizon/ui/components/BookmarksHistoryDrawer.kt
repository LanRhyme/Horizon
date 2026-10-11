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
    BOOKMARKS, HISTORY, DOWNLOADS
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
                .width(400.dp)
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
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .background(MorandiPalette.SurfaceSubtle, RoundedCornerShape(10.dp))
                            .padding(3.dp)
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                text = "下载 (${downloads.size})",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (selectedTab == LibraryTab.DOWNLOADS) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == LibraryTab.DOWNLOADS) MorandiPalette.AccentBlue else MorandiPalette.TextSecondary
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

                        LibraryTab.HISTORY -> {
                            if (history.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("暂无浏览历史", color = MorandiPalette.TextMuted, fontSize = 12.sp)
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
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
                                        items(history, key = { it.id }) { item ->
                                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.visitedAt))
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
                                                            text = "$timeStr · ${item.url}",
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
