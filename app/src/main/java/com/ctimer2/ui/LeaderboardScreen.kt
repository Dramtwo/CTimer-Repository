package com.ctimer2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ctimer2.network.RemoteScore
import com.ctimer2.ui.components.FrostedGlass
import com.ctimer2.ui.components.IconCloud
import com.ctimer2.ui.theme.DragonCatBlue
import com.ctimer2.viewmodel.LeaderboardViewModel

/**
 * 云端排行榜：数据来自 GitHub Issues（Dramtwo/CTimer-List）。
 * 整页毛玻璃，与其他页面同一材质。
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

    // 进入页面或切换用户后刷新（只拉当前用户名的成绩）
    androidx.compose.runtime.LaunchedEffect(userName) {
        viewModel.refresh(userName)
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
                .padding(20.dp)
        ) {
            // ---- 标题栏 ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DragonCatBlue.CardSurface)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(18.dp)) {
                        val w = size.width
                        val h = size.height
                        val stroke = 2.dp.toPx()
                        drawLine(
                            DragonCatBlue.TextPrimary,
                            androidx.compose.ui.geometry.Offset(0f, h / 2f),
                            androidx.compose.ui.geometry.Offset(w, h / 2f),
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
                Spacer(Modifier.width(14.dp))
                Box(Modifier.size(22.dp)) { IconCloud() }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "云端排行",
                        style = MaterialTheme.typography.titleLarge,
                        color = DragonCatBlue.TextPrimary
                    )
                    Text(
                        text = if (userName.isNullOrBlank())
                            "GitHub Issues · Dramtwo/CTimer-List · 全部用户"
                        else "GitHub Issues · 仅显示 $userName",
                        style = MaterialTheme.typography.labelSmall,
                        color = DragonCatBlue.TextSecondary
                    )
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { viewModel.refresh(userName) },
                    enabled = !state.loading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DragonCatBlue.PrimaryBlue
                    )
                ) {
                    Text("刷新", color = Color.White, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(14.dp))

            // ---- 上传区 ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = timeInput,
                    onValueChange = { timeInput = it },
                    label = { Text("成绩（秒）", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(0.32f)
                )
                OutlinedTextField(
                    value = sessionInput,
                    onValueChange = { sessionInput = it },
                    label = { Text("列表名", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(0.48f)
                )
                Button(
                    onClick = { viewModel.upload(timeInput, sessionInput, userName) },
                    enabled = !state.uploading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DragonCatBlue.PrimaryBlue
                    ),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(
                        if (state.uploading) "上传中…" else "上传",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }

            // ---- 状态提示 ----
            val hint = state.error ?: state.message
            if (hint != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = hint,
                    fontSize = 12.sp,
                    color = if (state.error != null) DragonCatBlue.ResultWorse else DragonCatBlue.ResultBetter
                )
            }

            Spacer(Modifier.height(12.dp))

            // ---- 列表 ----
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                Text("#", fontSize = 11.sp, color = DragonCatBlue.TextSecondary, modifier = Modifier.width(34.dp))
                Text("成绩", fontSize = 11.sp, color = DragonCatBlue.TextSecondary, modifier = Modifier.width(86.dp))
                Text("列表", fontSize = 11.sp, color = DragonCatBlue.TextSecondary, modifier = Modifier.weight(1f))
                Text("来自", fontSize = 11.sp, color = DragonCatBlue.TextSecondary)
            }
            Spacer(Modifier.height(6.dp))

            Box(Modifier.weight(1f), contentAlignment = Alignment.TopStart) {
                if (state.loading && state.scores.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DragonCatBlue.PrimaryBlue)
                    }
                } else if (state.scores.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (state.loading) "正在拉取…" else "云端还没有成绩，上传一条试试",
                            fontSize = 13.sp,
                            color = DragonCatBlue.TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(state.scores, key = { _, s -> s.key }) { index, score ->
                            ScoreLeaderRow(rank = index + 1, score = score)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreLeaderRow(rank: Int, score: RemoteScore) {
    val podium = rank <= 3
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (podium) DragonCatBlue.PrimaryBlue.copy(alpha = 0.14f)
                else Color.White.copy(alpha = 0.45f)
            )
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(34.dp), contentAlignment = Alignment.CenterStart) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> DragonCatBlue.PrimaryBlue
                            2 -> DragonCatBlue.PrimaryBlueLight
                            3 -> DragonCatBlue.BadgeGlassEnd
                            else -> Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    fontSize = if (podium) 11.sp else 12.sp,
                    color = if (podium) Color.White else DragonCatBlue.TextSecondary,
                    fontWeight = if (podium) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
        Text(
            text = score.displayTime,
            fontSize = 15.sp,
            color = DragonCatBlue.TextPrimary,
            fontWeight = if (podium) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.width(86.dp)
        )
        Text(
            text = score.session,
            fontSize = 12.sp,
            color = DragonCatBlue.TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = score.author ?: "—",
            fontSize = 11.sp,
            color = DragonCatBlue.TextSecondary
        )
    }
}
