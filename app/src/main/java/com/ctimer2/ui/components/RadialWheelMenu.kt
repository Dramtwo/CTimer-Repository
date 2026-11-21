package com.ctimer2.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ctimer2.ui.theme.DragonCatBlue
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 轮盘槽位常量。
 * 顺序 = 顺时针排列，第 0 个落在正上方。
 */
object WheelSlot {
    const val CANCEL = -1
    const val DELETE = 0
    const val PREV_SCORE = 1
    const val NEXT_SCORE = 2
    const val NEXT_SCRAMBLE = 3
    const val PREV_SCRAMBLE = 4
    const val COUNT = 5
}

/** 轮盘一个扇区的文案 */
data class WheelItem(
    val label: String,
    /** 危险项（删除）用红色高亮 */
    val danger: Boolean = false
)

/**
 * CS-Timer 风格的轮盘菜单（radial / wheel menu）。
 *
 * 体验设计：手指按下 → 在**手指位置**弹出这个圆盘；手指拖动改变方向 → 对应扇区高亮；
 * 松手 → 执行高亮项；把手指拖回中心死区 → 视为取消（不会误删）。
 *
 * 视觉：整盘是一块圆形毛玻璃（复用 [FrostedGlass]，取景与页面背景同源，
 * 所以盘里能看到「自己下面那块背景」的糊影），扇区之间用极淡白分割，
 * 选中的扇区整块染成主蓝（危险项染红），外圈再加一条 1.6dp 高光。
 *
 * @param centerX / centerY 圆心在父容器里的像素坐标（就是手指按下点）
 * @param selected 当前高亮的扇区下标，[WheelSlot.CANCEL] 表示在中心死区
 */
@Composable
fun RadialWheelMenu(
    centerX: Float,
    centerY: Float,
    selected: Int,
    items: List<WheelItem>,
    modifier: Modifier = Modifier,
    diameter: Dp = 268.dp
) {
    val density = LocalDensity.current
    val dPx = with(density) { diameter.toPx() }
    val sweep = 360f / items.size
    // 让第 0 项的中心对准正上方
    val startAngle = -90f - sweep / 2f
    val innerRatio = 0.30f

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = (centerX - dPx / 2f).roundToInt(),
                    y = (centerY - dPx / 2f).roundToInt()
                )
            }
            .size(diameter)
    ) {
        FrostedGlass(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            tintAlphaShift = -0.02f
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                val inner = r * innerRatio
                val gapDeg = 2.5f

                items.forEachIndexed { index, item ->
                    val isSel = index == selected
                    val s = startAngle + index * sweep + gapDeg
                    val sw = sweep - gapDeg * 2f
                    val fill = when {
                        isSel && item.danger -> DragonCatBlue.ResultWorse.copy(alpha = 0.32f)
                        isSel -> DragonCatBlue.PrimaryBlue.copy(alpha = 0.30f)
                        else -> Color.White.copy(alpha = 0.09f)
                    }
                    drawArc(
                        color = fill,
                        startAngle = s,
                        sweepAngle = sw,
                        useCenter = true,
                        topLeft = Offset(c.x - r, c.y - r),
                        size = Size(r * 2, r * 2)
                    )
                    if (isSel) {
                        val ringColor =
                            if (item.danger) DragonCatBlue.ResultWorse else DragonCatBlue.PrimaryBlue
                        val inset = 1.5.dp.toPx()
                        drawArc(
                            color = ringColor,
                            startAngle = s,
                            sweepAngle = sw,
                            useCenter = false,
                            topLeft = Offset(c.x - r + inset, c.y - r + inset),
                            size = Size((r - inset) * 2, (r - inset) * 2),
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                    }
                }

                // 中心死区：亮一点的圆，表示「拖到这里就取消」
                drawCircle(Color.White.copy(alpha = 0.26f), radius = inner, center = c)
                drawCircle(
                    color = DragonCatBlue.PrimaryBlue.copy(alpha = 0.40f),
                    radius = inner * 0.42f,
                    center = c,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // 中心 "取消" 文案
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(with(density) { (dPx * innerRatio).toDp() }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selected == WheelSlot.CANCEL) "松开取消" else "取消",
                    fontSize = 10.sp,
                    color = DragonCatBlue.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // 各扇区文案，沿圆周摆在内侧半径上
            items.forEachIndexed { index, item ->
                val midDeg = startAngle + index * sweep + sweep / 2f
                val rad = Math.toRadians(midDeg.toDouble())
                val labelR = dPx * 0.345f
                val dx = (labelR * cos(rad)).roundToInt()
                val dy = (labelR * sin(rad)).roundToInt()
                val isSel = index == selected

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(dx, dy) }
                        .width(with(density) { (dPx * 0.55f).toDp() }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            isSel && item.danger -> DragonCatBlue.ResultWorse
                            isSel -> DragonCatBlue.PrimaryBlue
                            else -> DragonCatBlue.TextPrimary
                        }
                    )
                }
            }
        }
    }
}

/** 默认的五个槽位文案 */
val DefaultWheelItems: List<WheelItem> = listOf(
    WheelItem("删除成绩", danger = true),
    WheelItem("上一个成绩"),
    WheelItem("下一个成绩"),
    WheelItem("下一条公式"),
    WheelItem("上一条公式")
)
