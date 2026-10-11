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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import org.mozilla.geckoview.GeckoView
import top.lanrhyme.horizon.extension.ExtensionPopupState
import top.lanrhyme.horizon.ui.theme.MorandiPalette

@Composable
fun ExtensionPopupDialog(
    popupState: ExtensionPopupState?,
    onDismiss: () -> Unit
) {
    if (popupState == null) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(420.dp)
                .height(540.dp),
            shape = RoundedCornerShape(18.dp),
            color = MorandiPalette.SurfaceElevated,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(MorandiPalette.Surface)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = popupState.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MorandiPalette.TextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MorandiPalette.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // 嵌入 GeckoView 显示扩展 Popup 页面
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)),
                    factory = { context ->
                        GeckoView(context).apply {
                            setSession(popupState.session)
                        }
                    },
                    update = { geckoView ->
                        geckoView.setSession(popupState.session)
                    }
                )
            }
        }
    }
}
