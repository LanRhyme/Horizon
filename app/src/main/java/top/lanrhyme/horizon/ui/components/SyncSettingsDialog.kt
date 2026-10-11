package top.lanrhyme.horizon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import top.lanrhyme.horizon.sync.FxaSyncManager
import top.lanrhyme.horizon.sync.SyncAccountState
import top.lanrhyme.horizon.ui.theme.MorandiPalette

@Composable
fun SyncSettingsDialog(
    onDismiss: () -> Unit
) {
    val syncState by FxaSyncManager.accountState.collectAsState()
    var emailInput by remember { mutableStateOf("user@mozilla.org") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(420.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MorandiPalette.SurfaceElevated,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 弹窗头部
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MorandiPalette.AccentBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Firefox 账户与同步",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MorandiPalette.TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MorandiPalette.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!syncState.isLoggedIn) {
                    // 未登录状态
                    Text(
                        text = "连接你的 Firefox 账户，在平板与桌面端之间无缝同步书签、历史记录和打开的标签页。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MorandiPalette.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Firefox 账户邮箱") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { FxaSyncManager.login(emailInput) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MorandiPalette.AccentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("登录 Firefox 账户并同步")
                    }
                } else {
                    // 已登录状态
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MorandiPalette.Surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MorandiPalette.AccentBlue
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = syncState.userEmail ?: "Firefox 用户",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MorandiPalette.TextPrimary
                                )
                                Text(
                                    text = "上次同步: ${syncState.lastSyncTime ?: "未同步"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MorandiPalette.TextMuted
                                )
                            }
                            if (syncState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MorandiPalette.AccentBlue
                                )
                            } else {
                                IconButton(onClick = { FxaSyncManager.triggerSync() }) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "即刻同步",
                                        tint = MorandiPalette.AccentBlue
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 同步开关项
                    SyncSwitchRow(
                        title = "同步已打开的标签页 (Synced Tabs)",
                        checked = syncState.syncTabsEnabled,
                        onCheckedChange = { FxaSyncManager.toggleSyncTabs(it) }
                    )
                    SyncSwitchRow(
                        title = "同步书签 (Bookmarks)",
                        checked = syncState.syncBookmarksEnabled,
                        onCheckedChange = { FxaSyncManager.toggleSyncBookmarks(it) }
                    )
                    SyncSwitchRow(
                        title = "同步历史记录 (History)",
                        checked = syncState.syncHistoryEnabled,
                        onCheckedChange = { FxaSyncManager.toggleSyncHistory(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { FxaSyncManager.logout() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MorandiPalette.AccentRose)
                    ) {
                        Text("退出账户")
                    }
                }
            }
        }
    }
}

@Composable
fun SyncSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MorandiPalette.TextPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MorandiPalette.SurfaceElevated,
                checkedTrackColor = MorandiPalette.AccentBlue
            )
        )
    }
}
