package top.lanrhyme.horizon.ui.components

import androidx.compose.foundation.background
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
    onOpenSyncSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember(activeTab?.url) { mutableStateOf(activeTab?.url ?: "") }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(16.dp),
        color = MorandiPalette.SurfaceElevated,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 后退/前进/刷新按钮
                IconButton(
                    onClick = onBack,
                    enabled = activeTab?.canGoBack == true,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "后退",
                        tint = if (activeTab?.canGoBack == true) MorandiPalette.TextPrimary else MorandiPalette.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onForward,
                    enabled = activeTab?.canGoForward == true,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "前进",
                        tint = if (activeTab?.canGoForward == true) MorandiPalette.TextPrimary else MorandiPalette.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (activeTab?.isLoading == true) Icons.Default.Close else Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = MorandiPalette.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 地址栏主输入框
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MorandiPalette.Surface)
                        .padding(horizontal = 12.dp),
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
                            modifier = Modifier.size(14.dp)
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
                                fontSize = 13.sp
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
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (isFocused && textInput.isNotBlank()) {
                            IconButton(
                                onClick = { textInput = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "清空",
                                    tint = MorandiPalette.TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 固定当前标签按钮
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (activeTab?.isPinned == true) Icons.Default.PushPin else Icons.Default.PushPin,
                        contentDescription = "固定标签",
                        tint = if (activeTab?.isPinned == true) MorandiPalette.AccentBlue else MorandiPalette.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Split View 分屏切换按钮
                IconButton(
                    onClick = onToggleSplitView,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerticalSplit,
                        contentDescription = "同屏分屏",
                        tint = if (isSplitViewActive) MorandiPalette.AccentBlue else MorandiPalette.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Firefox 账号与同步入口按钮
                IconButton(
                    onClick = onOpenSyncSettings,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Firefox 同步",
                        tint = MorandiPalette.AccentRose,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 网页加载进度条
            if (activeTab?.isLoading == true) {
                LinearProgressIndicator(
                    progress = { activeTab.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MorandiPalette.AccentBlue,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
