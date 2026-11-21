package com.ctimer2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ctimer2.ui.theme.DragonCatBlue
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 轮盘槽位常量。顺序 = 顺时针排列，第 0 个落在正上方。
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

/**
 * 轮盘几何参数。手势判定与渲染共用这一套，避免两边数字不同步导致「滑到按钮却不亮」。
 */
object WheelGeometry {
    /** 单个按钮直径 */
    val ButtonSize: Dp = 62.dp
    /** 按钮圆心到轮盘中心的距离 */
    val RingRadius: Dp = 84.dp
    /** 中心「取消」按钮直径 */
    val CenterSize: Dp = 54.dp
    /** 中心死区半径 = 中心按钮半径 + 这段余量 */
    val DeadZonePadding: Dp = 10.dp
    /** 触碰判定半径相对按钮半径的放大倍数（宽容一点，更好按） */
    const val HitSlop: Float = 1.25f
}

/** 轮盘一个按钮的文案 */
data class WheelItem(
    val label: String,
    /** 危险项（删除）用红色高亮 */
    val danger: Boolean = false
)

/**
 * CS-Timer 风格的轮盘菜单（radial / wheel menu）。
 *
 * 结构：**不再是整盘一块玻璃**，而是中心一个「取消」圆钮 + 环绕的 5 个圆形毛玻璃按钮。
 * 这样轮盘不会在屏幕上糊出一大块玻璃饼，只有按钮本身是玻璃材质；也更好看、更圆润。
 *
 * 交互：手指按下点即圆心（自动夹紧到屏幕内，绝不越界）；拖动方向对应按钮高亮放大；
 * 松手执行高亮项；拖回中心死区 = 取消。
 *
 * @param centerX / centerY 圆心在父容器里的像素坐标（手指按下点）
 * @param selected 当前高亮的槽位，[WheelSlot.CANCEL] 表示在中心死区
 */
@Composable
fun RadialWheelMenu(
    centerX: Float,
    centerY: Float,
    selected: Int,
    items: List<WheelItem>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val btnPx = with(density) { WheelGeometry.ButtonSize.toPx() }
    val ringPx = with(density) { WheelGeometry.RingRadius.toPx() }
    val centerPx = with(density) { WheelGeometry.CenterSize.toPx() }

    var container by remember { mutableStateOf(IntSize.Zero) }

    // 入场：整体从 0.86 放大淡入，按钮再逐个「弹出」
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val enter by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(170),
        label = "wheelEnter"
    )

    val extent = ringPx + btnPx / 2f
    val w = container.width.toFloat()
    val h = container.height.toFloat()
    // 夹紧圆心，保证整个轮盘留在屏幕里（贴着边缘按下时也不会被切掉）
    val cx = if (w > 2 * extent) centerX.coerceIn(extent, w - extent) else w / 2f
    val cy = if (h > 2 * extent) centerY.coerceIn(extent, h - extent) else h / 2f

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { if (it.size.width > 0) container = it.size }
    ) {
        // ---- 中心「取消」钮 ----
        val cancelSel = selected == WheelSlot.CANCEL
        val cancelScale by animateFloatAsState(
            targetValue = if (cancelSel) 1.10f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "cancelScale"
        )
        val cancelBg by animateColorAsState(
            targetValue = if (cancelSel) DragonCatBlue.PrimaryBlue else DragonCatBlue.TextPrimary,
            animationSpec = tween(160),
            label = "cancelColor"
        )
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (cx - centerPx / 2f).roundToInt(),
                        y = (cy - centerPx / 2f).roundToInt()
                    )
                }
                .size(WheelGeometry.CenterSize)
                .graphicsLayer {
                    scaleX = enter * cancelScale
                    scaleY = enter * cancelScale
                    alpha = enter
                }
        ) {
            FrostedGlass(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                tintAlphaShift = if (cancelSel) -0.20f else -0.02f
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (cancelSel) "松开\n取消" else "取消",
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = if (cancelSel) FontWeight.SemiBold else FontWeight.Normal,
                        color = cancelBg
                    )
                }
            }
        }

        // ---- 环绕的五个按钮 ----
        items.forEachIndexed { index, item ->
            val isSel = index == selected
            val midDeg = -90f + index * (360f / items.size)
            val rad = Math.toRadians(midDeg.toDouble())
            val bx = cx + ringPx * cos(rad).toFloat()
            val by = cy + ringPx * sin(rad).toFloat()

            val scale by animateFloatAsState(
                targetValue = if (isSel) 1.13f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "btnScale$index"
            )
            // 逐个弹出的入场延迟
            val pop by animateFloatAsState(
                targetValue = if (shown) 1f else 0f,
                animationSpec = tween(durationMillis = 150, delayMillis = index * 26),
                label = "btnPop$index"
            )
            val labelColor by animateColorAsState(
                targetValue = when {
                    isSel && item.danger -> DragonCatBlue.ResultWorse
                    isSel -> DragonCatBlue.PrimaryBlue
                    else -> DragonCatBlue.TextPrimary
                },
                animationSpec = tween(140),
                label = "labelColor$index"
            )

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (bx - btnPx / 2f).roundToInt(),
                            y = (by - btnPx / 2f).roundToInt()
                        )
                    }
                    .size(WheelGeometry.ButtonSize)
                    .graphicsLayer {
                        val s = enter * pop * scale
                        scaleX = s
                        scaleY = s
                        alpha = enter * pop
                    }
            ) {
                FrostedGlass(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    tintAlphaShift = when {
                        isSel && item.danger -> -0.30f
                        isSel -> -0.36f
                        else -> 0.02f
                    }
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = item.label,
                            modifier = Modifier
                                .width(with(density) { (btnPx * 0.80f).toDp() })
                                .padding(horizontal = 1.dp),
                            fontSize = 10.5.sp,
                            lineHeight = 12.5.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                            color = labelColor
                        )
                    }
                }
            }
        }
    }
}

/** 默认的五个槽位文案 */
val DefaultWheelItems: List<WheelItem> = listOf(
    WheelItem("删除\n成绩", danger = true),
    WheelItem("上一个\n成绩"),
    WheelItem("下一个\n成绩"),
    WheelItem("下一条\n公式"),
    WheelItem("上一条\n公式")
)
