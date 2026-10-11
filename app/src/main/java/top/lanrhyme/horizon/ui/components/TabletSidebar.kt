package top.lanrhyme.horizon.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import top.lanrhyme.horizon.model.BrowserSpace
import top.lanrhyme.horizon.model.BrowserTab
import top.lanrhyme.horizon.ui.theme.MorandiPalette

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabletSidebar(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    spaces: List<BrowserSpace>,
    currentSpaceId: String,
    onSelectSpace: (String) -> Unit,
    onAddSpace: (String, Long) -> Unit,
    tabs: List<BrowserTab>,
    activeTabId: String?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onTogglePin: (String) -> Unit,
    onMoveTabToSpace: (String, String) -> Unit,
    onCloseOtherTabs: (String) -> Unit,
    onOpenExtensions: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sidebarWidth by animateDpAsState(
        targetValue = if (isExpanded) 250.dp else 64.dp,
        label = "sidebar_width"
    )

    var showAddSpaceDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight(),
        color = MorandiPalette.SidebarBg,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶栏：品牌标识与新建/折叠控制
            if (isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MorandiPalette.AccentBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "H",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HORIZON",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = MorandiPalette.TextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 新建标签小按钮
                        IconButton(
                            onClick = onNewTab,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "新建标签",
                                tint = MorandiPalette.AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // 折叠侧边栏
                        IconButton(
                            onClick = onToggleExpand,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuOpen,
                                contentDescription = "折叠",
                                tint = MorandiPalette.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                // 折叠态：简洁展开按钮
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MorandiPalette.SurfaceSubtle)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "展开侧边栏",
                        tint = MorandiPalette.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 固定标签区 (Pinned Tabs)
            val pinnedTabs = tabs.filter { it.isPinned }
            if (pinnedTabs.isNotEmpty()) {
                if (isExpanded) {
                    Text(
                        text = "已固定",
                        style = MaterialTheme.typography.labelSmall,
                        color = MorandiPalette.TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, bottom = 4.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(pinnedTabs, key = { it.id }) { tab ->
                        TabRowItem(
                            tab = tab,
                            spaces = spaces,
                            isSelected = tab.id == activeTabId,
                            isExpanded = isExpanded,
                            onSelect = { onSelectTab(tab.id) },
                            onClose = { onCloseTab(tab.id) },
                            onTogglePin = { onTogglePin(tab.id) },
                            onMoveToSpace = { onMoveTabToSpace(tab.id, it) },
                            onCloseOthers = { onCloseOtherTabs(tab.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = MorandiPalette.BorderSubtle, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(6.dp))
            }

            // 当前空间活动标签列表
            val regularTabs = tabs.filter { !it.isPinned }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(regularTabs, key = { it.id }) { tab ->
                    TabRowItem(
                        tab = tab,
                        spaces = spaces,
                        isSelected = tab.id == activeTabId,
                        isExpanded = isExpanded,
                        onSelect = { onSelectTab(tab.id) },
                        onClose = { onCloseTab(tab.id) },
                        onTogglePin = { onTogglePin(tab.id) },
                        onMoveToSpace = { onMoveTabToSpace(tab.id, it) },
                        onCloseOthers = { onCloseOtherTabs(tab.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 底部一体化 Dock 容器
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MorandiPalette.SurfaceSubtle,
                border = BorderStroke(0.5.dp, MorandiPalette.BorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isExpanded) {
                        // Spaces 切换胶囊 + 新建空间
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            spaces.forEach { space ->
                                val isCurrent = space.id == currentSpaceId
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .combinedClickable(
                                            onClick = { onSelectSpace(space.id) }
                                        ),
                                    color = if (isCurrent) Color(space.accentColorHex).copy(alpha = 0.15f) else Color.Transparent,
                                    border = if (isCurrent) BorderStroke(0.5.dp, Color(space.accentColorHex).copy(alpha = 0.4f)) else null,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(space.accentColorHex))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = space.name,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isCurrent) Color(space.accentColorHex) else MorandiPalette.TextSecondary
                                            )
                                        )
                                    }
                                }
                            }

                            // 新增空间按钮
                            IconButton(
                                onClick = { showAddSpaceDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "新增空间",
                                    tint = MorandiPalette.TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = MorandiPalette.BorderSubtle, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(4.dp))

                        // 扩展、工具、设置快捷栏
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onOpenExtensions, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Extension, contentDescription = "扩展", tint = MorandiPalette.TextSecondary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onOpenTools, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Construction, contentDescription = "工具箱", tint = MorandiPalette.TextSecondary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onOpenSettings, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Settings, contentDescription = "设置", tint = MorandiPalette.TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        // 折叠态垂直紧凑点与小图标
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            spaces.forEach { space ->
                                val isCurrent = space.id == currentSpaceId
                                Box(
                                    modifier = Modifier
                                        .size(if (isCurrent) 10.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(Color(space.accentColorHex))
                                        .clickable { onSelectSpace(space.id) }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            IconButton(onClick = onOpenExtensions, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Extension, contentDescription = "扩展", tint = MorandiPalette.TextMuted, modifier = Modifier.size(14.dp))
                            }
                            IconButton(onClick = onOpenTools, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Construction, contentDescription = "工具箱", tint = MorandiPalette.TextMuted, modifier = Modifier.size(14.dp))
                            }
                            IconButton(onClick = onOpenSettings, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Settings, contentDescription = "设置", tint = MorandiPalette.TextMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSpaceDialog) {
        var newSpaceName by remember { mutableStateOf("") }
        val colors = listOf(0xFF5B7A9C, 0xFF6E8B7E, 0xFFA86F77, 0xFFB58E62, 0xFF7D6B9C)
        var selectedColor by remember { mutableStateOf(colors[0]) }

        AlertDialog(
            onDismissRequest = { showAddSpaceDialog = false },
            title = { Text("新建工作空间") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newSpaceName,
                        onValueChange = { newSpaceName = it },
                        label = { Text("空间名称（如：论文、科研）") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(
                                        width = if (selectedColor == c) 2.dp else 0.dp,
                                        color = if (selectedColor == c) MorandiPalette.TextPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = c }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newSpaceName.isNotBlank()) {
                            onAddSpace(newSpaceName, selectedColor)
                            showAddSpaceDialog = false
                        }
                    }
                ) {
                    Text("创建", color = MorandiPalette.AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSpaceDialog = false }) {
                    Text("取消", color = MorandiPalette.TextSecondary)
                }
            },
            containerColor = MorandiPalette.SurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabRowItem(
    tab: BrowserTab,
    spaces: List<BrowserSpace>,
    isSelected: Boolean,
    isExpanded: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onTogglePin: () -> Unit,
    onMoveToSpace: (String) -> Unit,
    onCloseOthers: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(
                    onClick = onSelect,
                    onLongClick = { showMenu = true }
                ),
            color = if (isSelected) MorandiPalette.TabSelectedBg else Color.Transparent,
            shape = RoundedCornerShape(10.dp),
            shadowElevation = if (isSelected) 1.dp else 0.dp,
            border = if (isSelected) BorderStroke(0.5.dp, MorandiPalette.BorderSubtle) else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
            ) {
                // 选中态微型彩色竖标
                if (isSelected && isExpanded) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MorandiPalette.AccentBlue)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // 真实 Favicon 图标 / 加载圈
                if (tab.isLoading) {
                    CircularProgressIndicator(
                        progress = { tab.progress / 100f },
                        modifier = Modifier.size(15.dp),
                        strokeWidth = 2.dp,
                        color = MorandiPalette.AccentBlue
                    )
                } else {
                    if (tab.url == "horizon://newtab") {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isSelected) MorandiPalette.AccentBlue else MorandiPalette.TextSecondary
                        )
                    } else if (!tab.faviconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = tab.faviconUrl,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            onError = {}
                        )
                    } else {
                        Icon(
                            imageVector = if (tab.isPinned) Icons.Default.PushPin else Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isSelected) MorandiPalette.AccentBlue else MorandiPalette.TextSecondary
                        )
                    }
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = tab.title.ifBlank { "新标签页" },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MorandiPalette.TextPrimary else MorandiPalette.TextSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 关闭按钮
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(12.dp),
                            tint = MorandiPalette.TextMuted
                        )
                    }
                }
            }
        }

        // 长按上下文菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (tab.isPinned) "取消固定" else "固定标签页") },
                onClick = {
                    onTogglePin()
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )

            DropdownMenuItem(
                text = { Text("复制网址") },
                onClick = {
                    clipboardManager.setText(AnnotatedString(tab.url))
                    Toast.makeText(context, "已复制网址", Toast.LENGTH_SHORT).show()
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )

            // 移动到其他空间子菜单
            val otherSpaces = spaces.filter { it.id != tab.spaceId }
            if (otherSpaces.isNotEmpty()) {
                otherSpaces.forEach { targetSpace ->
                    DropdownMenuItem(
                        text = { Text("移至「${targetSpace.name}」") },
                        onClick = {
                            onMoveToSpace(targetSpace.id)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text("关闭其他标签页") },
                onClick = {
                    onCloseOthers()
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )

            DropdownMenuItem(
                text = { Text("关闭标签页", color = MorandiPalette.AccentRose) },
                onClick = {
                    onClose()
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = MorandiPalette.AccentRose, modifier = Modifier.size(16.dp)) }
            )
        }
    }
}
