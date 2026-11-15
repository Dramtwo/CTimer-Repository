package com.ctimer2.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import com.ctimer2.ui.theme.DragonCatBlue

private val ICON_SIZE = 22.dp

/** 成绩总评：三根高低不同的柱 */
@Composable
fun IconSummary(tint: Color = DragonCatBlue.PrimaryBlue, iconSize: androidx.compose.ui.unit.Dp = ICON_SIZE) {
    Canvas(Modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val barW = w * 0.22f
        val gap = w * 0.16f
        val heights = listOf(0.55f, 0.95f, 0.75f)
        heights.forEachIndexed { i, hf ->
            val left = i * (barW + gap)
            drawRect(
                color = tint,
                topLeft = androidx.compose.ui.geometry.Offset(left, h * (1f - hf)),
                size = androidx.compose.ui.geometry.Size(barW, h * hf)
            )
        }
    }
}

/** 按时间：表盘 + 两根指针 */
@Composable
fun IconTime(tint: Color = DragonCatBlue.PrimaryBlue, iconSize: androidx.compose.ui.unit.Dp = ICON_SIZE) {
    Canvas(Modifier.size(iconSize)) {
        val r = size.minDimension / 2f
        val c = androidx.compose.ui.geometry.Offset(size.width / 2f, r)
        drawCircle(color = tint, radius = r - 1.dp.toPx(), center = c, style = Stroke(2.dp.toPx()))
        drawLine(tint, c, androidx.compose.ui.geometry.Offset(c.x, c.y - r * 0.55f), 2.dp.toPx())
        drawLine(tint, c, androidx.compose.ui.geometry.Offset(c.x + r * 0.45f, c.y), 2.dp.toPx())
    }
}

/** 按成绩：领奖台三阶 */
@Composable
fun IconScore(tint: Color = DragonCatBlue.PrimaryBlue, iconSize: androidx.compose.ui.unit.Dp = ICON_SIZE) {
    Canvas(Modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val stepW = w / 3f
        val heights = listOf(0.55f, 1.0f, 0.75f)
        heights.forEachIndexed { i, hf ->
            drawRect(
                color = tint,
                topLeft = androidx.compose.ui.geometry.Offset(i * stepW, h * (1f - hf)),
                size = androidx.compose.ui.geometry.Size(stepW - 1.dp.toPx(), h * hf)
            )
        }
    }
}

/** 列表：堆叠的三个圆角方块 */
@Composable
fun IconList(tint: Color = DragonCatBlue.PrimaryBlue, iconSize: androidx.compose.ui.unit.Dp = ICON_SIZE) {
    Canvas(Modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val lineH = h / 5f
        repeat(3) { i ->
            drawRect(
                color = tint,
                topLeft = androidx.compose.ui.geometry.Offset(0f, i * lineH * 2f),
                size = androidx.compose.ui.geometry.Size(w, lineH)
            )
        }
    }
}

/** 公式速记：一页公式纸（外框 + 三条公式行） */
@Composable
fun IconFormula(tint: Color = DragonCatBlue.PrimaryBlue, iconSize: androidx.compose.ui.unit.Dp = ICON_SIZE) {
    Canvas(Modifier.size(iconSize)) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2f, stroke / 2f),
            size = androidx.compose.ui.geometry.Size(w - stroke, h - stroke),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            style = Stroke(stroke)
        )
        val lineX1 = w * 0.22f
        val lineX2 = w * 0.78f
        listOf(0.32f, 0.52f, 0.72f).forEach { fy ->
            drawLine(
                color = tint,
                start = androidx.compose.ui.geometry.Offset(lineX1, h * fy),
                end = androidx.compose.ui.geometry.Offset(lineX2, h * fy),
                strokeWidth = stroke
            )
        }
    }
}

/** 导入：向下箭头 + 承接托盘 */
@Composable
fun IconImport(tint: Color = DragonCatBlue.TextSecondary, iconSize: androidx.compose.ui.unit.Dp = ICON_SIZE) {
    Canvas(Modifier.size(iconSize)) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // 竖直主干
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(cx, h * 0.10f),
            end = androidx.compose.ui.geometry.Offset(cx, h * 0.58f),
            strokeWidth = stroke
        )
        // 箭头两侧
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(cx, h * 0.58f),
            end = androidx.compose.ui.geometry.Offset(cx - w * 0.17f, h * 0.40f),
            strokeWidth = stroke
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(cx, h * 0.58f),
            end = androidx.compose.ui.geometry.Offset(cx + w * 0.17f, h * 0.40f),
            strokeWidth = stroke
        )
        // 底部托盘：两段折线
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.76f),
            end = androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.76f),
            strokeWidth = stroke
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.76f),
            end = androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.88f),
            strokeWidth = stroke
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.76f),
            end = androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.88f),
            strokeWidth = stroke
        )
    }
}

/** 设置：三根滑杆（每根一个圆点） */
@Composable
fun IconSettings(tint: Color = DragonCatBlue.TextSecondary, iconSize: androidx.compose.ui.unit.Dp = ICON_SIZE) {
    Canvas(Modifier.size(iconSize)) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height
        val rows = listOf(0.28f, 0.5f, 0.72f)
        val knobX = listOf(0.36f, 0.64f, 0.42f)

        rows.forEachIndexed { i, fy ->
            val y = h * fy
            drawLine(
                color = tint,
                start = androidx.compose.ui.geometry.Offset(w * 0.12f, y),
                end = androidx.compose.ui.geometry.Offset(w * 0.88f, y),
                strokeWidth = stroke
            )
            drawCircle(
                color = tint,
                radius = stroke * 1.6f,
                center = androidx.compose.ui.geometry.Offset(w * knobX[i], y)
            )
        }
    }
}

/** 重置卡片位置：环形箭头（箭头精确落在圆环端口上，方向沿切线） */
@Composable
fun IconReset(tint: Color = DragonCatBlue.TextSecondary, iconSize: androidx.compose.ui.unit.Dp = ICON_SIZE) {
    Canvas(Modifier.size(iconSize)) {
        val stroke = 2.dp.toPx()
        val r = size.minDimension / 2f - stroke
        val c = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val startAngle = -50f
        val sweep = 300f

        drawArc(
            color = tint,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(c.x - r, c.y - r),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = Stroke(stroke)
        )

        // 圆环起点（弧的起始端口）
        val rad = Math.toRadians(startAngle.toDouble())
        val sx = c.x + r * kotlin.math.cos(rad).toFloat()
        val sy = c.y + r * kotlin.math.sin(rad).toFloat()

        // 箭头方向：沿圆环切线（起点角度 + 90°）
        val tangent = Math.toRadians((startAngle + 90f).toDouble())
        val dx = kotlin.math.cos(tangent).toFloat()
        val dy = kotlin.math.sin(tangent).toFloat()
        val px = -dy
        val py = dx

        val len = r * 0.62f
        val halfW = r * 0.34f

        val tip = androidx.compose.ui.geometry.Offset(sx + dx * len, sy + dy * len)
        val base1 = androidx.compose.ui.geometry.Offset(sx + px * halfW, sy + py * halfW)
        val base2 = androidx.compose.ui.geometry.Offset(sx - px * halfW, sy - py * halfW)

        val arrow = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(base1.x, base1.y)
            lineTo(base2.x, base2.y)
            close()
        }
        drawPath(arrow, tint)
    }
}

/** 云端排行：一朵云 + 下降箭头（代表云端数据落地） */
@Composable
fun IconCloud(tint: Color = DragonCatBlue.PrimaryBlue, iconSize: androidx.compose.ui.unit.Dp = ICON_SIZE) {
    Canvas(Modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        // 云体：三段圆弧拼成的轮廓
        val cx = w * 0.5f
        val cy = h * 0.38f
        val r1 = w * 0.20f
        val r2 = w * 0.15f
        val r3 = w * 0.13f
        drawCircle(tint, r1, androidx.compose.ui.geometry.Offset(w * 0.38f, cy), style = Stroke(stroke))
        drawCircle(tint, r2, androidx.compose.ui.geometry.Offset(w * 0.63f, cy + h * 0.02f), style = Stroke(stroke))
        drawCircle(tint, r3, androidx.compose.ui.geometry.Offset(w * 0.24f, cy + h * 0.06f), style = Stroke(stroke))
        // 云底横线
        drawLine(
            tint,
            androidx.compose.ui.geometry.Offset(w * 0.20f, cy + h * 0.14f),
            androidx.compose.ui.geometry.Offset(w * 0.80f, cy + h * 0.14f),
            stroke
        )
        // 下方下载箭头
        drawLine(
            tint,
            androidx.compose.ui.geometry.Offset(cx, h * 0.55f),
            androidx.compose.ui.geometry.Offset(cx, h * 0.84f),
            stroke
        )
        val arrow = Path().apply {
            moveTo(cx, h * 0.94f)
            lineTo(cx - w * 0.13f, h * 0.76f)
            lineTo(cx + w * 0.13f, h * 0.76f)
            close()
        }
        drawPath(arrow, tint)
    }
}

/** 用户系统：几何人形（头 + 肩） */
@Composable
fun IconUser(tint: Color = DragonCatBlue.PrimaryBlue, iconSize: androidx.compose.ui.unit.Dp = ICON_SIZE) {
    Canvas(Modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        // 头
        drawCircle(
            color = tint,
            radius = w * 0.19f,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.28f),
            style = Stroke(stroke)
        )
        // 肩：半圆弧
        drawArc(
            color = tint,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.56f),
            size = androidx.compose.ui.geometry.Size(w * 0.68f, h * 0.52f),
            style = Stroke(stroke)
        )
    }
}

/** 删除：几何垃圾桶（云端成绩行尾的删除按钮） */
@Composable
fun IconTrash(
    tint: Color = DragonCatBlue.TextSecondary,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp
) {
    Canvas(Modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        // 桶盖横线
        drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.27f),
            androidx.compose.ui.geometry.Offset(w * 0.84f, h * 0.27f), stroke)
        // 提手
        drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.27f),
            androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.12f), stroke)
        drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.12f),
            androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.12f), stroke)
        drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.12f),
            androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.27f), stroke)
        // 桶身（上宽下窄）
        val body = Path().apply {
            moveTo(w * 0.24f, h * 0.30f)
            lineTo(w * 0.29f, h * 0.88f)
            lineTo(w * 0.71f, h * 0.88f)
            lineTo(w * 0.76f, h * 0.30f)
        }
        drawPath(body, tint, style = Stroke(stroke))
        // 两道纹路
        drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.41f, h * 0.42f),
            androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.74f), stroke * 0.75f)
        drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.59f, h * 0.42f),
            androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.74f), stroke * 0.75f)
    }
}
