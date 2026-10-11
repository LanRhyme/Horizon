package top.lanrhyme.horizon.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import top.lanrhyme.horizon.ui.theme.MorandiPalette
import java.text.SimpleDateFormat
import java.util.*

data class QuickSite(
    val title: String,
    val url: String,
    val iconUrl: String,
    val fallbackLetter: String,
    val accentColor: Color
)

@Composable
fun NewTabDashboard(
    onNavigate: (String) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenExtensions: () -> Unit,
    currentEngine: String,
    modifier: Modifier = Modifier
) {
    var searchInput by remember { mutableStateOf("") }

    val currentTime = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11 -> "早上好"
            in 12..17 -> "下午好"
            in 18..22 -> "晚上好"
            else -> "夜深了"
        }
        val dateStr = SimpleDateFormat("M月d日 EEEE", Locale.CHINESE).format(Date())
        Pair(greeting, dateStr)
    }

    val quickSites = remember {
        listOf(
            QuickSite("GitHub", "https://github.com", "https://github.githubassets.com/favicons/favicon.svg", "G", MorandiPalette.TextPrimary),
            QuickSite("MDN 文档", "https://developer.mozilla.org", "https://developer.mozilla.org/favicon-48x48.png", "M", MorandiPalette.AccentBlue),
            QuickSite("arXiv 文献", "https://arxiv.org", "https://arxiv.org/static/browse/0.3.4/images/arxiv-logo-one-color.svg", "A", MorandiPalette.AccentRose),
            QuickSite("维基百科", "https://zh.wikipedia.org", "https://zh.wikipedia.org/static/favicon/wikipedia.ico", "W", MorandiPalette.AccentSage),
            QuickSite("少数派", "https://sspai.com", "https://sspai.com/favicon.ico", "S", MorandiPalette.AccentAmber),
            QuickSite("知乎", "https://www.zhihu.com", "https://static.zhihu.com/heifetz/favicon.ico", "Z", MorandiPalette.AccentBlue),
            QuickSite("哔哩哔哩", "https://www.bilibili.com", "https://www.bilibili.com/favicon.ico", "B", MorandiPalette.AccentRose),
            QuickSite("V2EX", "https://v2ex.com", "https://v2ex.com/static/favicon.ico", "V", MorandiPalette.AccentSage)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MorandiPalette.SurfaceSubtle),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxHeight()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 问候语与日期
            Text(
                text = currentTime.first,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MorandiPalette.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${currentTime.second}  ·  Horizon 专注沉浸式平板文档阅读",
                style = MaterialTheme.typography.bodySmall,
                color = MorandiPalette.TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 居中主搜索胶囊
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MorandiPalette.Border),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = MorandiPalette.AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    BasicTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MorandiPalette.TextPrimary,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(MorandiPalette.AccentBlue),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (searchInput.isNotBlank()) {
                                onNavigate(searchInput)
                            }
                        }),
                        decorationBox = { innerTextField ->
                            if (searchInput.isBlank()) {
                                Text(
                                    text = "在 $currentEngine 搜索或输入网址...",
                                    color = MorandiPalette.TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchInput.isNotBlank()) {
                        IconButton(
                            onClick = { searchInput = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "清除",
                                tint = MorandiPalette.TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 快捷站点网格 (Speed Dial)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickSites) { site ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onNavigate(site.url) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(0.8.dp, MorandiPalette.BorderSubtle),
                        shadowElevation = 1.5.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(site.accentColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                var isImageError by remember { mutableStateOf(false) }
                                if (!isImageError) {
                                    AsyncImage(
                                        model = site.iconUrl,
                                        contentDescription = site.title,
                                        modifier = Modifier.size(22.dp),
                                        onError = { isImageError = true }
                                    )
                                } else {
                                    Text(
                                        text = site.fallbackLetter,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = site.accentColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = site.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MorandiPalette.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 底部快捷操作胶囊 (书签 / 历史 / 扩展)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DashboardActionPill(
                    icon = Icons.Default.Bookmarks,
                    title = "书签库",
                    onClick = onOpenBookmarks
                )
                DashboardActionPill(
                    icon = Icons.Default.History,
                    title = "历史记录",
                    onClick = onOpenHistory
                )
                DashboardActionPill(
                    icon = Icons.Default.Extension,
                    title = "扩展商店",
                    onClick = onOpenExtensions
                )
            }
        }
    }
}

@Composable
private fun DashboardActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(0.8.dp, MorandiPalette.BorderSubtle),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MorandiPalette.AccentBlue,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MorandiPalette.TextPrimary
            )
        }
    }
}
