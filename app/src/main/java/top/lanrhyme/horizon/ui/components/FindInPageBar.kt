package top.lanrhyme.horizon.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanrhyme.horizon.model.FindInPageState
import top.lanrhyme.horizon.ui.theme.MorandiPalette

@Composable
fun FindInPageBar(
    state: FindInPageState,
    onQueryChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.isActive) {
        if (state.isActive) {
            focusRequester.requestFocus()
        }
    }

    AnimatedVisibility(
        visible = state.isActive,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .padding(top = 8.dp, end = 16.dp)
                .width(360.dp),
            shape = RoundedCornerShape(12.dp),
            color = MorandiPalette.SurfaceElevated,
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, MorandiPalette.BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 搜索输入框
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MorandiPalette.SurfaceSubtle)
                        .border(0.5.dp, MorandiPalette.BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MorandiPalette.TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        BasicTextField(
                            value = state.query,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MorandiPalette.TextPrimary,
                                fontSize = 12.sp
                            ),
                            cursorBrush = SolidColor(MorandiPalette.AccentBlue),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onFindNext() }),
                            decorationBox = { innerTextField ->
                                if (state.query.isEmpty()) {
                                    Text(
                                        text = "在页面中查找...",
                                        color = MorandiPalette.TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 匹配计数
                if (state.query.isNotEmpty()) {
                    Text(
                        text = if (state.totalMatches > 0) "${state.currentMatch}/${state.totalMatches}" else "0/0",
                        fontSize = 11.sp,
                        color = if (state.totalMatches > 0) MorandiPalette.TextPrimary else MorandiPalette.AccentRose
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // 上一个
                IconButton(
                    onClick = onFindPrevious,
                    enabled = state.query.isNotEmpty() && state.totalMatches > 0,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "上一个",
                        tint = if (state.totalMatches > 0) MorandiPalette.TextPrimary else MorandiPalette.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 下一个
                IconButton(
                    onClick = onFindNext,
                    enabled = state.query.isNotEmpty() && state.totalMatches > 0,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "下一个",
                        tint = if (state.totalMatches > 0) MorandiPalette.TextPrimary else MorandiPalette.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 关闭按钮
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭查找",
                        tint = MorandiPalette.TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
