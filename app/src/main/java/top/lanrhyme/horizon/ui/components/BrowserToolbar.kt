package top.lanrhyme.horizon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanrhyme.horizon.model.BrowserTab
import top.lanrhyme.horizon.ui.theme.MorandiPalette

@Composable
fun BrowserToolbar(
    activeTab: BrowserTab?,
    isSplitViewActive: Boolean,
    onNavigateUrl: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onToggleSplitView: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleReaderMode: () -> Unit,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onOpenBookmarksHistory: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayUrl = remember(activeTab?.url) {
        val u = activeTab?.url ?: ""
        if (u == "horizon://newtab" || u == "about:blank") "" else u
    }
    var textInput by remember(displayUrl) { mutableStateOf(displayUrl) }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MorandiPalette.SurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 后退/前进/刷新微按钮组
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = activeTab?.canGoBack == true,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "后退",
                        tint = if (activeTab?.canGoBack == true) MorandiPalette.TextPrimary else MorandiPalette.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onForward,
                    enabled = activeTab?.canGoForward == true,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "前进",
                        tint = if (activeTab?.canGoForward == true) MorandiPalette.TextPrimary else MorandiPalette.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (activeTab?.isLoading == true) Icons.Default.Close else Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = MorandiPalette.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 现代化沉浸地址栏输入框
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MorandiPalette.SurfaceSubtle)
                    .border(0.5.dp, if (isFocused) MorandiPalette.AccentBlue else MorandiPalette.BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (activeTab?.url?.startsWith("https://") == true) Icons.Default.Lock else Icons.Default.Search,
                        contentDescription = null,
                        tint = if (activeTab?.url?.startsWith("https://") == true) MorandiPalette.AccentSage else MorandiPalette.TextMuted,
                        modifier = Modifier.size(13.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { isFocused = it.isFocused },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MorandiPalette.TextPrimary,
                            fontSize = 12.sp
                        ),
                        cursorBrush = SolidColor(MorandiPalette.AccentBlue),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            onNavigateUrl(textInput)
                            focusManager.clearFocus()
                        }),
                        decorationBox = { innerTextField ->
                            if (textInput.isBlank() && !isFocused) {
                                Text(
                                    text = "搜索或输入网址",
                                    color = MorandiPalette.TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (isFocused && textInput.isNotBlank()) {
                        IconButton(
                            onClick = { textInput = "" },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "清空",
                                tint = MorandiPalette.TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 右侧功能图标栏：桌面模式、固定、分屏、工具箱、同步
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 桌面端视图切换
                IconButton(
                    onClick = onToggleDesktopMode,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DesktopWindows,
                        contentDescription = "桌面版",
                        tint = if (activeTab?.isDesktopMode == true) MorandiPalette.AccentBlue else MorandiPalette.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 文档免打扰阅读模式
                IconButton(
                    onClick = onToggleReaderMode,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "阅读模式",
                        tint = if (activeTab?.isReaderMode == true) MorandiPalette.AccentSage else MorandiPalette.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 收藏书签五角星
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "收藏书签",
                        tint = if (isBookmarked) MorandiPalette.AccentAmber else MorandiPalette.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 固定当前标签
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "固定标签",
                        tint = if (activeTab?.isPinned == true) MorandiPalette.AccentBlue else MorandiPalette.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Split View 分屏切换
                IconButton(
                    onClick = onToggleSplitView,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerticalSplit,
                        contentDescription = "同屏分屏",
                        tint = if (isSplitViewActive) MorandiPalette.AccentBlue else MorandiPalette.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 书签与历史面板
                IconButton(
                    onClick = onOpenBookmarksHistory,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmarks,
                        contentDescription = "书签与历史",
                        tint = MorandiPalette.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 工具箱
                IconButton(
                    onClick = onOpenTools,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = "工具箱",
                        tint = MorandiPalette.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Firefox 同步入口
                IconButton(
                    onClick = onOpenSyncSettings,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Firefox 同步",
                        tint = MorandiPalette.AccentRose,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 紧贴工具栏底部的加载进度指示条或微细底边线
        if (activeTab?.isLoading == true) {
            LinearProgressIndicator(
                progress = { activeTab.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp),
                color = MorandiPalette.AccentBlue,
                trackColor = Color.Transparent
            )
        } else {
            HorizontalDivider(
                color = MorandiPalette.BorderSubtle,
                thickness = 0.5.dp
            )
        }
    }
}
