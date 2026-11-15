package com.ctimer2.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ctimer2.ui.theme.DragonCatBlue

/**
 * 几何画风秒表：顶部表冠 + 圆表盘 + 中心轴 + 两根指针。
 * 所有几何量按画布实际尺寸推算，"表冠 + 表盘"整体垂直居中。
 */
@Composable
fun StopwatchIcon(
    modifier: Modifier = Modifier,
    tint: Color = DragonCatBlue.PrimaryBlueLight
) {
    Canvas(modifier = modifier.size(28.dp)) {
        val stroke = 2.dp.toPx()
        val short = size.minDimension
        val cx = size.width / 2f

        // 表冠占 15% 高度，其余留给表盘
        val crownH = short * 0.15f
        val r = (short - crownH - stroke * 2f) / 2f
        val cy = crownH + stroke + r

        // 表盘
        drawCircle(
            color = tint,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = stroke)
        )

        // 表冠帽
        val capHalf = short * 0.09f
        drawLine(
            color = tint,
            start = Offset(cx - capHalf, stroke / 2f),
            end = Offset(cx + capHalf, stroke / 2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // 表冠杆：末端改用平头（Butt），避免圆帽在圆周接缝处多凸出一小点
        drawLine(
            color = tint,
            start = Offset(cx, stroke / 2f),
            end = Offset(cx, cy - r),
            strokeWidth = stroke,
            cap = StrokeCap.Butt
        )

        // 分针（12 点方向）
        drawLine(
            color = tint,
            start = Offset(cx, cy),
            end = Offset(cx, cy - r * 0.62f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // 秒针（约 4 点方向）
        drawLine(
            color = tint,
            start = Offset(cx, cy),
            end = Offset(cx + r * 0.44f, cy + r * 0.38f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // 中心轴点
        drawCircle(
            color = tint,
            radius = stroke * 0.9f,
            center = Offset(cx, cy)
        )
    }
}
