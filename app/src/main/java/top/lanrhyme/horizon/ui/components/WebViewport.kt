package top.lanrhyme.horizon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import top.lanrhyme.horizon.model.BrowserTab
import top.lanrhyme.horizon.ui.theme.MorandiPalette

@Composable
fun WebViewport(
    primarySession: GeckoSession?,
    primaryTab: BrowserTab?,
    secondarySession: GeckoSession?,
    secondaryTab: BrowserTab?,
    isSplitView: Boolean,
    onCloseSecondary: () -> Unit,
    onNavigate: (String) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenExtensions: () -> Unit,
    currentEngine: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Start
    ) {
        // 主视口
        Box(
            modifier = Modifier
                .weight(if (isSplitView) 0.5f else 1f)
                .fillMaxHeight()
                .background(Color.White)
        ) {
            if (primaryTab?.url == "horizon://newtab") {
                NewTabDashboard(
                    onNavigate = onNavigate,
                    onOpenBookmarks = onOpenBookmarks,
                    onOpenHistory = onOpenHistory,
                    onOpenExtensions = onOpenExtensions,
                    currentEngine = currentEngine
                )
            } else if (primarySession != null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        GeckoView(context).apply {
                            setSession(primarySession)
                        }
                    },
                    update = { geckoView ->
                        geckoView.setSession(primarySession)
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("无活动标签页", color = MorandiPalette.TextMuted, fontSize = 13.sp)
                }
            }
        }

        // 分屏次视口 (Split View)
        if (isSplitView && secondarySession != null) {
            // 中间垂直分割线
            VerticalDivider(
                color = MorandiPalette.BorderSubtle,
                thickness = 1.dp,
                modifier = Modifier.fillMaxHeight()
            )

            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .background(Color.White)
            ) {
                // 分屏顶栏小标题与关闭按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(MorandiPalette.SurfaceSubtle)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = secondaryTab?.title ?: "并排分屏",
                        style = MaterialTheme.typography.bodySmall,
                        color = MorandiPalette.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onCloseSecondary,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭分屏",
                            modifier = Modifier.size(12.dp),
                            tint = MorandiPalette.TextMuted
                        )
                    }
                }

                if (secondaryTab?.url == "horizon://newtab") {
                    NewTabDashboard(
                        onNavigate = onNavigate,
                        onOpenBookmarks = onOpenBookmarks,
                        onOpenHistory = onOpenHistory,
                        onOpenExtensions = onOpenExtensions,
                        currentEngine = currentEngine
                    )
                } else {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        factory = { context ->
                            GeckoView(context).apply {
                                setSession(secondarySession)
                            }
                        },
                        update = { geckoView ->
                            geckoView.setSession(secondarySession)
                        }
                    )
                }
            }
        }
    }
}
