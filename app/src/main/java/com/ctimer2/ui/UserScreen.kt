package com.ctimer2.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ctimer2.ui.components.FrostedGlass
import com.ctimer2.ui.components.IconUser
import com.ctimer2.ui.theme.DragonCatBlue

/**
 * 用户中心：只要用户名，不需要密码。
 * 登录后，上传的成绩标题会带 [用户名] 前缀，拉取云端成绩也只拉自己的。
 */
@Composable
fun UserScreen(
    userName: String?,
    syncing: Boolean,
    cloudStatus: String?,
    onLogin: (String) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember(userName) { mutableStateOf(userName ?: "") }

    FrostedGlass(
        modifier = modifier.fillMaxSize(),
        shape = RectangleShape,
        showBorder = false,
        tintAlphaShift = -0.16f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // 标题栏
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
                Box(Modifier.size(22.dp)) { IconUser() }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "用户系统",
                        style = MaterialTheme.typography.titleLarge,
                        color = DragonCatBlue.TextPrimary
                    )
                    Text(
                        "用户名即身份，无需密码",
                        style = MaterialTheme.typography.labelSmall,
                        color = DragonCatBlue.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 当前身份
            FrostedGlass(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                tintAlphaShift = 0.04f,
                backdrop = false
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("当前用户", fontSize = 11.sp, color = DragonCatBlue.TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = userName ?: "未登录（成绩上传不带用户名）",
                        fontSize = 18.sp,
                        color = if (userName == null) DragonCatBlue.TextSecondary else DragonCatBlue.TextPrimary
                    )
                    if (syncing) {
                        Spacer(Modifier.height(6.dp))
                        Text("正在同步云端…", fontSize = 12.sp, color = DragonCatBlue.TextSecondary)
                    } else if (!cloudStatus.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(cloudStatus!!, fontSize = 12.sp, color = DragonCatBlue.TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 登录 / 改名
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("用户名", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onLogin(input) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DragonCatBlue.PrimaryBlue),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text(
                        if (userName == null) "登录" else "保存用户名",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                if (userName != null) {
                    Button(
                        onClick = onLogout,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DragonCatBlue.ResultWorse.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("退出登录", color = DragonCatBlue.ResultWorse, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "登录后：\n" +
                    "· 每次上传的成绩标题会带 [用户名] 前缀，例如 [CTimer2] [Boziwan] 12.34 · 默认列表\n" +
                    "· 从仓库拉取时只拉这个用户名的成绩，别人的不会混进来\n" +
                    "· 用户名保存在本机，重启后仍保持登录",
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = DragonCatBlue.TextSecondary
            )
        }
    }
}
