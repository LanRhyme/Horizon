package top.lanrhyme.horizon.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanrhyme.horizon.model.BrowserSpace
import top.lanrhyme.horizon.model.BrowserTab
import top.lanrhyme.horizon.ui.theme.MorandiPalette

@Composable
fun TabletSidebar(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    spaces: List<BrowserSpace>,
    currentSpaceId: String,
    onSelectSpace: (String) -> Unit,
    tabs: List<BrowserTab>,
    activeTabId: String?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onTogglePin: (String) -> Unit,
    onOpenExtensions: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sidebarWidth by animateDpAsState(
        targetValue = if (isExpanded) 260.dp else 68.dp,
        label = "sidebar_width"
    )

    Surface(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .padding(8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MorandiPalette.Surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶栏操作区：展开/折叠、新建标签
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = if (isExpanded) Arrangement.SpaceBetween else Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                        contentDescription = "切换侧边栏",
                        tint = MorandiPalette.TextPrimary
                    )
                }

                if (isExpanded) {
                    IconButton(
                        onClick = onNewTab,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MorandiPalette.SurfaceElevated, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新建标签页",
                            tint = MorandiPalette.AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 固定标签区 (Pinned Tabs)
            val pinnedTabs = tabs.filter { it.isPinned }
            if (pinnedTabs.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(pinnedTabs, key = { it.id }) { tab ->
                        TabRowItem(
                            tab = tab,
                            isSelected = tab.id == activeTabId,
                            isExpanded = isExpanded,
                            onSelect = { onSelectTab(tab.id) },
                            onClose = { onCloseTab(tab.id) },
                            onTogglePin = { onTogglePin(tab.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MorandiPalette.Border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 当前空间普通标签列表
            val regularTabs = tabs.filter { !it.isPinned }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(regularTabs, key = { it.id }) { tab ->
                    TabRowItem(
                        tab = tab,
                        isSelected = tab.id == activeTabId,
                        isExpanded = isExpanded,
                        onSelect = { onSelectTab(tab.id) },
                        onClose = { onCloseTab(tab.id) },
                        onTogglePin = { onTogglePin(tab.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 底部工作空间 Spaces 胶囊栏
            if (isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(MorandiPalette.SurfaceElevated, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    spaces.forEach { space ->
                        val isCurrent = space.id == currentSpaceId
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectSpace(space.id) },
                            color = if (isCurrent) Color(space.accentColorHex).copy(alpha = 0.2f) else Color.Transparent
                        ) {
                            Text(
                                text = space.name,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) Color(space.accentColorHex) else MorandiPalette.TextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 一体化底部系统功能栏：扩展、工具箱、设置
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(MorandiPalette.SurfaceElevated, RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenExtensions,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = "扩展中心",
                            tint = MorandiPalette.TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenTools,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Construction,
                            contentDescription = "工具箱",
                            tint = MorandiPalette.TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = MorandiPalette.TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            } else {
                // 折叠态：紧凑空间圆点切换与垂直功能图标
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    spaces.forEach { space ->
                        val isCurrent = space.id == currentSpaceId
                        Box(
                            modifier = Modifier
                                .size(if (isCurrent) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(Color(space.accentColorHex))
                                .clickable { onSelectSpace(space.id) }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    IconButton(onClick = onOpenExtensions, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Extension, contentDescription = "扩展", tint = MorandiPalette.TextMuted, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onOpenTools, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Construction, contentDescription = "工具箱", tint = MorandiPalette.TextMuted, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = MorandiPalette.TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TabRowItem(
    tab: BrowserTab,
    isSelected: Boolean,
    isExpanded: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onTogglePin: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() },
        color = if (isSelected) MorandiPalette.TabSelectedBg else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
        ) {
            // 加载环 / 图标
            if (tab.isLoading) {
                CircularProgressIndicator(
                    progress = { tab.progress / 100f },
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MorandiPalette.AccentBlue
                )
            } else {
                Icon(
                    imageVector = if (tab.isPinned) Icons.Default.PushPin else Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isSelected) MorandiPalette.TextPrimary else MorandiPalette.TextSecondary
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = tab.title.ifBlank { "新标签页" },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MorandiPalette.TextPrimary else MorandiPalette.TextSecondary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 关闭按钮
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        modifier = Modifier.size(14.dp),
                        tint = MorandiPalette.TextMuted
                    )
                }
            }
        }
    }
}
