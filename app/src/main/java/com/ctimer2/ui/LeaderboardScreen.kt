package com.ctimer2.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ctimer2.network.RemoteScore
import com.ctimer2.ui.components.FrostedGlass
import com.ctimer2.ui.components.IconCloud
import com.ctimer2.ui.components.IconTrash
import com.ctimer2.ui.theme.DragonCatBlue
import com.ctimer2.viewmodel.LeaderboardViewModel

/**
 * 云端排行榜。
 *
 * 版式迭代说明：原来是一行一条的列表（每行 40dp 高、横向排满），信息密度低、
 * 屏幕只能看到 7-8 条，且长条状不好看。改成**双列卡片**：
 * 每张卡 78dp 高，左上角排名、中间成绩大字、底部「列表名 · 用户」、右上角删除按钮，
 * 横屏一屏能看 10-12 条，卡片式也比长条更耐看。
 *
 * 动效：卡片增删用 LazyGrid 的 animateItem 平滑过渡；排名前三的染色会随名次
 * 变化做颜色动画；删除时该位置显示转圈。
 */
@Composable
fun LeaderboardScreen(
    defaultSession: String,
    userName: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LeaderboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var timeInput by remember { mutableStateOf("") }
    var sessionInput by remember { mutableStateOf(defaultSession) }
    var pendingDelete by remember { mutableStateOf<RemoteScore?>(null) }
    var userMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    FrostedGlass(
        modifier = modifier.fillMaxSize(),
        shape = RectangleShape,
        showBorder = false,
        tintAlphaShift = -0.14f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // ---- 标题栏 ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DragonCatBlue.CardSurface)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(17.dp)) {
                        val w = size.width
                        val h = size.height
                        val stroke = 2.dp.toPx()
                        drawLine(
                            DragonCatBlue.TextPrimary,
                            Offset(0f, h / 2f),
                            Offset(w, h / 2f),
                            stroke
                        )
                        val head = Path().apply {
                            moveTo(w * 0.42f, h * 0.12f)
                            lineTo(0f, h / 2f)
                            lineTo(w * 0.42f, h * 0.88f)
                        }
                        drawPath(head, DragonCatBlue.TextPrimary, style = Stroke(stroke))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(20.dp)) { IconCloud() }
                Spacer(Modifier.width(9.dp))
                Column {
                    Text(
                        "云端排行",
                        style = MaterialTheme.typography.titleLarge,
                        color = DragonCatBlue.TextPrimary
                    )
                    Text(
                        text = buildString {
                            append("Dramtwo/CTimer-List · ")
                            val sel = state.selectedUser
                            append(if (sel == null) "所有用户" else "仅看 $sel")
                            append(" · ${state.visibleScores.size} 条")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = DragonCatBlue.TextSecondary
                    )
                }

                Spacer(Modifier.weight(1f))

                // 用户筛选下拉
                Box {
                    Button(
                        onClick = { userMenuOpen = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DragonCatBlue.CardSurface),
                        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = state.selectedUser ?: "所有用户",
                            color = DragonCatBlue.TextPrimary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        Text("  ▾", color = DragonCatBlue.TextSecondary, fontSize = 11.sp)
                    }
                    DropdownMenu(
                        expanded = userMenuOpen,
                        onDismissRequest = { userMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("所有用户", fontSize = 13.sp) },
                            onClick = {
                                viewModel.selectUser(null)
                                userMenuOpen = false
                            }
                        )
                        state.availableUsers.forEach { u ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "$u（${state.scores.count { it.user.equals(u, true) }}）",
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    viewModel.selectUser(u)
                                    userMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.refresh() },
                    enabled = !state.loading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DragonCatBlue.PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("刷新", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            // ---- 上传区（压成一行，尽量把空间留给列表） ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = timeInput,
                    onValueChange = { timeInput = it },
                    label = { Text("成绩（秒）", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.28f)
                        .height(52.dp)
                )
                OutlinedTextField(
                    value = sessionInput,
                    onValueChange = { sessionInput = it },
                    label = { Text("列表名", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.44f)
                        .height(52.dp)
                )
                Button(
                    onClick = { viewModel.upload(timeInput, sessionInput, userName) },
                    enabled = !state.uploading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DragonCatBlue.PrimaryBlue),
                    modifier = Modifier
                        .weight(0.2f)
                        .height(52.dp)
                ) {
                    Text(
                        if (state.uploading) "上传中…" else "上传",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            val hint = state.error ?: state.message
            if (hint != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = hint,
                    fontSize = 11.sp,
                    color = if (state.error != null) DragonCatBlue.ResultWorse else DragonCatBlue.ResultBetter
                )
            }

            Spacer(Modifier.height(8.dp))

            // ---- 双列成绩卡片 ----
            Box(Modifier.weight(1f)) {
                if (state.loading && state.visibleScores.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DragonCatBlue.PrimaryBlue)
                    }
                } else if (state.visibleScores.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (state.loading) "正在拉取…"
                            else if (state.selectedUser != null) "该用户暂无云端成绩"
                            else "云端还没有成绩，上传一条试试",
                            fontSize = 12.sp,
                            color = DragonCatBlue.TextSecondary
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = state.visibleScores,
                            key = { _, s -> s.key }
                        ) { index, score ->
                            ScoreGridCard(
                                rank = index + 1,
                                score = score,
                                deleting = score.issueNumber in state.deleting,
                                onDelete = { pendingDelete = score },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(220),
                                    placementSpec = tween(260),
                                    fadeOutSpec = tween(160)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- 删除确认框 ----
    val target = pendingDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除云端成绩？", fontSize = 15.sp, color = DragonCatBlue.TextPrimary) },
            text = {
                Text(
                    "将从 GitHub 仓库永久删除 Issue #${target.issueNumber}\n" +
                        "${target.displayTime} · ${target.session}\n\n此操作不可撤销。",
                    fontSize = 13.sp,
                    color = DragonCatBlue.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteScore(target, userName)
                        pendingDelete = null
                    }
                ) {
                    Text("删除", color = DragonCatBlue.ResultWorse, fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消", color = DragonCatBlue.TextSecondary, fontSize = 14.sp)
                }
            }
        )
    }
}

/**
 * 一条云端成绩的卡片：78dp 高，双列排布。
 *
 * 排版说明：内容**整体居中**（原来左对齐又留了右侧删除位的 padding，看起来文字偏左）。
 * 删除按钮是绝对定位在右上角的小圆，不参与文字排版，所以居中不会被打扰。
 */
@Composable
private fun ScoreGridCard(
    rank: Int,
    score: RemoteScore,
    deleting: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val podium = rank <= 3
    val bg by animateColorAsState(
        targetValue = when (rank) {
            1 -> DragonCatBlue.PrimaryBlue.copy(alpha = 0.22f)
            2 -> DragonCatBlue.PrimaryBlue.copy(alpha = 0.15f)
            3 -> DragonCatBlue.PrimaryBlue.copy(alpha = 0.10f)
            else -> Color.White.copy(alpha = 0.50f)
        },
        animationSpec = tween(260),
        label = "cardBg"
    )
    val badgeColor by animateColorAsState(
        targetValue = if (podium) DragonCatBlue.PrimaryBlue else Color.White.copy(alpha = 0.62f),
        animationSpec = tween(260),
        label = "badgeColor"
    )
    // 入场：卡片依次上浮淡入（延迟按名次阶梯，最多吃 12 档，避免长列表末尾等太久）
    var appeared by remember(score.key) { mutableStateOf(false) }
    LaunchedEffect(score.key) {
        kotlinx.coroutines.delay((rank.coerceAtMost(12) - 1) * 32L)
        appeared = true
    }
    val enter by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(240),
        label = "cardEnter"
    )
    // 删除中：整卡轻微收缩，给一点「按下去」的反馈
    val pressScale by animateFloatAsState(
        targetValue = if (deleting) 0.955f else 1f,
        animationSpec = tween(150),
        label = "cardPress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                alpha = enter
                translationY = with(density) { ((1f - enter) * 14.dp.toPx()) }
            }
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(enabled = !deleting, onClick = onDelete)
    ) {
        // 内容区：水平、垂直都居中；左右各留 12dp 让位给删除钮
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$rank",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (podium) Color.White else DragonCatBlue.TextSecondary
                    )
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    text = score.displayTime,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DragonCatBlue.TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildString {
                    append(score.session)
                    val author = score.author
                    if (!author.isNullOrBlank()) {
                        append(" · ")
                        append(author)
                    }
                },
                fontSize = 10.sp,
                color = DragonCatBlue.TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 右上角删除按钮（绝对定位，不占文字排版）
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(5.dp)
                .size(21.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.52f))
                .clickable(enabled = !deleting, onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            if (deleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(11.dp),
                    color = DragonCatBlue.PrimaryBlue,
                    strokeWidth = 1.3.dp
                )
            } else {
                IconTrash(iconSize = 11.dp)
            }
        }
    }
}
