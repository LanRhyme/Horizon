package top.lanrhyme.horizon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class MainActivity : ComponentActivity() {

    private var geckoSession: GeckoSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val runtime = (application as HorizonApp).geckoRuntime
        geckoSession = GeckoSession().apply {
            open(runtime)
            loadUri("https://www.google.com")
        }

        setContent {
            MaterialTheme {
                HorizonTabletLayout(session = geckoSession)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        geckoSession?.close()
    }
}

@Composable
fun HorizonTabletLayout(
    session: GeckoSession?,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf("https://www.google.com") }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F9))
            .systemBarsPadding()
    ) {
        // 侧边栏 (Zen/Arc 风格卡片式)
        Surface(
            modifier = Modifier
                .width(if (isExpanded) 240.dp else 64.dp)
                .fillMaxHeight()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFECEEF2),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
            ) {
                // 顶部控制折叠与新建
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isExpanded) Arrangement.SpaceBetween else Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.MenuOpen else Icons.Default.Menu,
                            contentDescription = "Toggle Sidebar"
                        )
                    }
                    if (isExpanded) {
                        IconButton(onClick = { session?.loadUri("https://duckduckgo.com") }) {
                            Icon(Icons.Default.Add, contentDescription = "New Tab")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 示例标签列表
                TabItem(
                    title = "Google Search",
                    icon = Icons.Default.Language,
                    isSelected = true,
                    isExpanded = isExpanded,
                    onClick = { session?.loadUri("https://www.google.com") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                TabItem(
                    title = "Mozilla Developer",
                    icon = Icons.Default.Code,
                    isSelected = false,
                    isExpanded = isExpanded,
                    onClick = { session?.loadUri("https://developer.mozilla.org") }
                )
            }
        }

        // 右侧网页主视口
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            // 顶栏轻量地址显示
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure",
                        tint = Color(0xFF6C7A89),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentUrl,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2C3E50)
                    )
                }
            }

            // GeckoView 网页容器
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    factory = { context ->
                        GeckoView(context).apply {
                            session?.let { setSession(it) }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TabItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFFD6DBE4) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) Color(0xFF1B263B) else Color(0xFF5C677D)
            )
            if (isExpanded) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) Color(0xFF1B263B) else Color(0xFF5C677D),
                    maxLines = 1
                )
            }
        }
    }
}
