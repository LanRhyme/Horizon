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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 主视口
        Surface(
            modifier = Modifier
                .weight(if (isSplitView) 0.5f else 1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 1.dp
        ) {
            if (primarySession != null) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
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
                    Text("无活动标签页", color = MorandiPalette.TextMuted)
                }
            }
        }

        // 分屏次视口 (Split View)
        if (isSplitView && secondarySession != null) {
            Surface(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 分屏顶栏小标题与关闭按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(MorandiPalette.Surface)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = secondaryTab?.title ?: "并排分屏",
                            style = MaterialTheme.typography.bodySmall,
                            color = MorandiPalette.TextSecondary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onCloseSecondary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭分屏",
                                modifier = Modifier.size(14.dp),
                                tint = MorandiPalette.TextMuted
                            )
                        }
                    }

                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
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
