package top.lanrhyme.horizon.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanrhyme.horizon.model.BrowserTab
import top.lanrhyme.horizon.ui.theme.MorandiPalette

@Composable
fun ToolsDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    activeTab: BrowserTab?,
    onToggleDesktopMode: () -> Unit,
    onViewSource: () -> Unit,
    onClearData: () -> Unit,
    onNewTab: () -> Unit,
    onOpenExtensions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .padding(top = 8.dp, bottom = 8.dp, end = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MorandiPalette.SurfaceElevated,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 面板顶栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MorandiPalette.AccentBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Construction,
                                contentDescription = null,
                                tint = MorandiPalette.AccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "工具箱",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MorandiPalette.TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MorandiPalette.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 工具列表
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 桌面端视图切换
                    ToolCardItem(
                        icon = Icons.Default.DesktopWindows,
                        title = "桌面版网站",
                        description = if (activeTab?.isDesktopMode == true) "已启用桌面端 UA 呈现" else "当前为平板/移动版布局",
                        action = {
                            Switch(
                                checked = activeTab?.isDesktopMode == true,
                                onCheckedChange = { onToggleDesktopMode() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MorandiPalette.AccentBlue
                                )
                            )
                        },
                        onClick = onToggleDesktopMode
                    )

                    // 查看网页源代码
                    ToolCardItem(
                        icon = Icons.Default.Code,
                        title = "查看网页源代码",
                        description = "在新标签页高亮解析 HTML 结构",
                        onClick = {
                            onViewSource()
                            onClose()
                        }
                    )

                    // 扩展插件快速直达
                    ToolCardItem(
                        icon = Icons.Default.Extension,
                        title = "扩展管理与商店",
                        description = "管理已安装扩展或安装新脚本",
                        onClick = {
                            onOpenExtensions()
                            onClose()
                        }
                    )

                    // 清除浏览器缓存与 Cookie
                    ToolCardItem(
                        icon = Icons.Default.DeleteOutline,
                        title = "清除缓存与 Cookie",
                        description = "一键清理本地临时存储与会话数据",
                        onClick = { showClearConfirm = true },
                        accentColor = MorandiPalette.AccentRose
                    )

                    // 新建标签页
                    ToolCardItem(
                        icon = Icons.Default.AddBox,
                        title = "新建空白标签页",
                        description = "在当前工作空间快速开启新窗口",
                        onClick = {
                            onNewTab()
                            onClose()
                        }
                    )
                }

                // 底部状态提示
                Text(
                    text = "Horizon Engine Powered by Mozilla GeckoView",
                    style = MaterialTheme.typography.bodySmall,
                    color = MorandiPalette.TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清除浏览数据") },
            text = { Text("将清除全部缓存、Cookie 及本地存储数据，此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearData()
                        showClearConfirm = false
                    }
                ) {
                    Text("立即清除", color = MorandiPalette.AccentRose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消", color = MorandiPalette.TextSecondary)
                }
            },
            containerColor = MorandiPalette.SurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ToolCardItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MorandiPalette.AccentBlue,
    action: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        color = MorandiPalette.Surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MorandiPalette.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MorandiPalette.TextSecondary,
                    fontSize = 11.sp
                )
            }

            if (action != null) {
                action()
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MorandiPalette.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
