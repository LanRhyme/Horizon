package top.lanrhyme.horizon.ui.components

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanrhyme.horizon.extension.CuratedExtension
import top.lanrhyme.horizon.extension.ExtensionManager
import top.lanrhyme.horizon.extension.InstalledExtensionItem
import top.lanrhyme.horizon.sync.FxaSyncManager
import top.lanrhyme.horizon.ui.theme.MorandiPalette

enum class SettingsCategory(val title: String, val icon: ImageVector) {
    APPEARANCE("外观", Icons.Default.Palette),
    SEARCH_PRIVACY("搜索与隐私", Icons.Default.Shield),
    EXTENSIONS("扩展插件", Icons.Default.Extension),
    SYNC_ABOUT("同步与关于", Icons.Default.Sync)
}

@Composable
fun SettingsDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    currentEngine: String,
    onSelectEngine: (String) -> Unit,
    extensionManager: ExtensionManager,
    modifier: Modifier = Modifier,
    initialCategory: SettingsCategory = SettingsCategory.EXTENSIONS
) {
    var selectedCategory by remember { mutableStateOf(initialCategory) }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .width(440.dp)
                .fillMaxHeight()
                .padding(top = 8.dp, bottom = 8.dp, end = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MorandiPalette.SurfaceElevated,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 面板头部
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MorandiPalette.AccentSage.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MorandiPalette.AccentSage,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "浏览器设置",
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

                // 顶部四大分类 Tab 切换
                ScrollableTabRow(
                    selectedTabIndex = selectedCategory.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = MorandiPalette.AccentBlue,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    SettingsCategory.entries.forEach { category ->
                        Tab(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            text = {
                                Text(
                                    text = category.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedCategory == category) MorandiPalette.AccentBlue else MorandiPalette.TextSecondary
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedCategory == category) MorandiPalette.AccentBlue else MorandiPalette.TextMuted
                                )
                            }
                        )
                    }
                }

                HorizontalDivider(color = MorandiPalette.Border, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                // 内容区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    when (selectedCategory) {
                        SettingsCategory.APPEARANCE -> AppearanceSettingsSection()
                        SettingsCategory.SEARCH_PRIVACY -> SearchPrivacySection(
                            currentEngine = currentEngine,
                            onSelectEngine = onSelectEngine
                        )
                        SettingsCategory.EXTENSIONS -> ExtensionsSection(extensionManager = extensionManager)
                        SettingsCategory.SYNC_ABOUT -> SyncAboutSection()
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceSettingsSection() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("主题与色彩规范", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MorandiPalette.TextPrimary)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MorandiPalette.Surface
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("当前激活：全局莫兰迪调色盘 (Morandi Warm/Cool)", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MorandiPalette.AccentBlue))
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MorandiPalette.AccentSage))
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MorandiPalette.AccentRose))
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MorandiPalette.AccentAmber))
                }
            }
        }

        Text("平板优化", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MorandiPalette.TextPrimary)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MorandiPalette.Surface
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• 垂直标签侧边栏支持 260dp ↔ 68dp 平滑折叠动画", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.TextSecondary)
                Text("• 原生支持 1:1 Split View 同屏并排双视口", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.TextSecondary)
                Text("• 地址栏与工具栏采用悬浮超椭圆微卡片设计", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.TextSecondary)
            }
        }
    }
}

@Composable
private fun SearchPrivacySection(
    currentEngine: String,
    onSelectEngine: (String) -> Unit
) {
    val engines = listOf("Bing", "Google", "DuckDuckGo", "Baidu")
    var trackingProtectionEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("默认搜索引擎", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MorandiPalette.TextPrimary)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            engines.forEach { engine ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectEngine(engine) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (currentEngine == engine) MorandiPalette.AccentBlue.copy(alpha = 0.12f) else MorandiPalette.Surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = engine,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (currentEngine == engine) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentEngine == engine) MorandiPalette.AccentBlue else MorandiPalette.TextPrimary
                        )
                        RadioButton(
                            selected = currentEngine == engine,
                            onClick = { onSelectEngine(engine) },
                            colors = RadioButtonDefaults.colors(selectedColor = MorandiPalette.AccentBlue)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("隐私与安全", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MorandiPalette.TextPrimary)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MorandiPalette.Surface
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("智能反跟踪保护", style = MaterialTheme.typography.bodyMedium, color = MorandiPalette.TextPrimary)
                    Text("阻断已知社交跟踪器与跨站指纹追踪", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = trackingProtectionEnabled,
                    onCheckedChange = { trackingProtectionEnabled = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = MorandiPalette.AccentSage)
                )
            }
        }
    }
}

@Composable
private fun ExtensionsSection(
    extensionManager: ExtensionManager
) {
    val installed by extensionManager.installedExtensions.collectAsState()
    val isInstalling by extensionManager.isInstalling.collectAsState()
    val installMessage by extensionManager.installMessage.collectAsState()
    var customUrlInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // 安装状态提示
            if (installMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MorandiPalette.AccentBlue.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = installMessage ?: "", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.AccentBlue)
                        IconButton(onClick = { extensionManager.clearMessage() }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = MorandiPalette.AccentBlue, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // 自定义 XPI / URL 安装
        item {
            Text("安装新扩展", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MorandiPalette.TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customUrlInput,
                    onValueChange = { customUrlInput = it },
                    placeholder = { Text("输入 .xpi 文件直链或本地 file:// 路径", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (customUrlInput.isNotBlank()) {
                            extensionManager.installExtension(customUrlInput)
                            customUrlInput = ""
                        }
                    },
                    enabled = !isInstalling && customUrlInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MorandiPalette.AccentBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("安装", fontSize = 12.sp)
                }
            }
        }

        // 精选推荐扩展
        item {
            Text("精选推荐扩展", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MorandiPalette.TextPrimary)
        }

        items(extensionManager.curatedExtensions) { curated ->
            val isAlreadyInstalled = installed.any { it.id == curated.id }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MorandiPalette.Surface
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MorandiPalette.AccentSage.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = MorandiPalette.AccentSage,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = curated.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MorandiPalette.TextPrimary)
                        Text(text = curated.description, style = MaterialTheme.typography.bodySmall, color = MorandiPalette.TextSecondary, fontSize = 11.sp, maxLines = 2)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isAlreadyInstalled) {
                        Text("已安装", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.TextMuted, fontSize = 12.sp)
                    } else {
                        FilledTonalButton(
                            onClick = { extensionManager.installExtension(curated.downloadUrl) },
                            enabled = !isInstalling,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("一键安装", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 已安装扩展列表
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("已安装扩展 (${installed.size})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MorandiPalette.TextPrimary)
        }

        if (installed.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MorandiPalette.Surface
                ) {
                    Text(
                        text = "暂无已安装的 WebExtension 插件",
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MorandiPalette.TextMuted
                    )
                }
            }
        } else {
            items(installed) { ext ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MorandiPalette.Surface
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = ext.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MorandiPalette.TextPrimary)
                            Text(text = "版本 ${ext.version}", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.TextSecondary, fontSize = 11.sp)
                        }

                        Switch(
                            checked = ext.isEnabled,
                            onCheckedChange = { extensionManager.toggleExtension(ext) },
                            colors = SwitchDefaults.colors(checkedTrackColor = MorandiPalette.AccentBlue)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(onClick = { extensionManager.uninstallExtension(ext) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "卸载", tint = MorandiPalette.AccentRose, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncAboutSection() {
    val state by FxaSyncManager.accountState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Firefox 跨端同步", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MorandiPalette.TextPrimary)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MorandiPalette.Surface
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("当前账号", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.TextSecondary)
                        Text(
                            text = state.userEmail ?: "未绑定 Firefox 账号",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MorandiPalette.TextPrimary
                        )
                    }

                    if (state.isLoggedIn) {
                        OutlinedButton(
                            onClick = { FxaSyncManager.logout() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("退出", fontSize = 11.sp, color = MorandiPalette.AccentRose)
                        }
                    } else {
                        var showLoginDialog by remember { mutableStateOf(false) }
                        var emailInput by remember { mutableStateOf("user@mozilla.org") }

                        Button(
                            onClick = { showLoginDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MorandiPalette.AccentBlue),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("连接账号", fontSize = 11.sp)
                        }

                        if (showLoginDialog) {
                            AlertDialog(
                                onDismissRequest = { showLoginDialog = false },
                                title = { Text("连接 Firefox 账户") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("关联你的 Firefox Sync 账号以同步跨端书签与标签页：", fontSize = 12.sp, color = MorandiPalette.TextSecondary)
                                        OutlinedTextField(
                                            value = emailInput,
                                            onValueChange = { emailInput = it },
                                            label = { Text("Firefox 邮箱") },
                                            singleLine = true
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        if (emailInput.isNotBlank()) {
                                            FxaSyncManager.login(emailInput)
                                            showLoginDialog = false
                                        }
                                    }) {
                                        Text("确定连接", color = MorandiPalette.AccentBlue)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showLoginDialog = false }) {
                                        Text("取消", color = MorandiPalette.TextSecondary)
                                    }
                                },
                                containerColor = MorandiPalette.SurfaceElevated,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                if (state.isLoggedIn) {
                    HorizontalDivider(color = MorandiPalette.BorderSubtle, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("上次同步", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.TextSecondary, fontSize = 11.sp)
                            Text(text = state.lastSyncTime ?: "正在同步...", fontSize = 12.sp, color = MorandiPalette.TextPrimary)
                        }
                        FilledTonalButton(
                            onClick = { FxaSyncManager.triggerSync() },
                            enabled = !state.isSyncing,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(if (state.isSyncing) "同步中..." else "立即同步", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // 同步项目微调
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("同步书签库", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.TextPrimary)
                        Switch(
                            checked = state.syncBookmarksEnabled,
                            onCheckedChange = { FxaSyncManager.toggleSyncBookmarks(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = MorandiPalette.AccentBlue)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("同步标签页 (Tabs)", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.TextPrimary)
                        Switch(
                            checked = state.syncTabsEnabled,
                            onCheckedChange = { FxaSyncManager.toggleSyncTabs(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = MorandiPalette.AccentBlue)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("同步历史记录", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.TextPrimary)
                        Switch(
                            checked = state.syncHistoryEnabled,
                            onCheckedChange = { FxaSyncManager.toggleSyncHistory(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = MorandiPalette.AccentBlue)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("关于 Horizon", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MorandiPalette.TextPrimary)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MorandiPalette.Surface
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Horizon 平板浏览器", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MorandiPalette.TextPrimary)
                Text("版本: v1.0.0 (Preview Build)", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.TextSecondary)
                Text("引擎核心: Mozilla GeckoView 157.0 Omni Engine", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.TextSecondary)
                Text("开源主页: https://github.com/LanRhyme/Horizon", style = MaterialTheme.typography.bodySmall, color = MorandiPalette.AccentBlue)
            }
        }
    }
}
