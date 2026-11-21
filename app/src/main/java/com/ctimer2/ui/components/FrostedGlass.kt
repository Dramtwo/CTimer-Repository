package com.ctimer2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ctimer2.ui.theme.DragonCatBlue
import com.ctimer2.ui.theme.GlassMode
import com.ctimer2.ui.theme.LocalGlass
import com.ctimer2.ui.theme.LocalGlassBackdrop
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 玻璃面板：半透明白 + 1dp 高光描边，并叠上**预烘焙好的模糊背景**的对应区域。
 *
 * 关键设计：模糊不在这里做。整屏的模糊版本由 [LocalGlassBackdrop] 统一提供，
 * 面板只是把这张图按自己的窗口坐标平移、裁切。因此：
 * - 滚动 / 拖动时完全不重算模糊（不掉帧）；
 * - 面板内看到的永远是同一张背景图的一部分，和面板外的背景不会「不是同一层」。
 *
 * @param tintAlphaShift 相对全局设定的透明度微调（次级面更透传 -0.18 之类）
 */
@Composable
fun FrostedGlass(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    tintAlphaShift: Float = 0f,
    showBorder: Boolean = true,
    /**
     * 是否在本面板内取景渲染糊过的背景。
     * - true：玻璃「窗口」，能看到下层糊影（叠在清晰背景上用）
     * - false：玻璃「分区」，只用淡色+高光边（叠在别的玻璃层上用，避免双层模糊造成割裂）
     */
    backdrop: Boolean = true,
    /**
     * 高亮态：边缘会做「呼吸式」高光渐变（白色描边 + 主蓝外晕在渲染层来回呼吸）。
     * 只重绘不重组，适合选中态 / 提示条这类需要存在感的玻璃。
     */
    highlighted: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlass.current
    val backdropState = LocalGlassBackdrop.current
    var pos by remember { mutableStateOf(IntOffset.Zero) }
    // 用根布局的真实像素尺寸换算，避免 dp 与像素比例不一致导致取景错位（割裂）
    var windowSize by remember { mutableStateOf(IntSize.Zero) }

    val alpha = (glass.tintAlpha + tintAlphaShift +
        if (backdrop) 0f else 0.10f).coerceIn(0.2f, 0.98f)
    val solidBase = when (glass.mode) {
        GlassMode.SOLID -> DragonCatBlue.SolidCard
        else -> Color.White
    }

    // 呼吸高光：只在高亮且非实色时创建无限过渡；数值在 draw 阶段读取（渲染层，不触发重组）
    val breath: State<Float> = if (highlighted && glass.mode != GlassMode.SOLID) {
        val transition = rememberInfiniteTransition(label = "glassBreath")
        transition.animateFloat(
            initialValue = 0.22f,
            targetValue = 0.80f,
            animationSpec = infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(
                    durationMillis = 1400,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathAlpha"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(
        modifier = modifier
            .drawWithContent {
                drawContent()
                // 渲染阶段读 alpha：动画只引起重绘，不引起重组
                val a = breath.value
                if (a > 0.02f) {
                    val path = Path()
                    when (val outline = shape.createOutline(size, layoutDirection, this)) {
                        is Outline.Rectangle -> path.addRect(outline.rect)
                        is Outline.Rounded -> path.addRoundRect(outline.roundRect)
                        is Outline.Generic -> path.addPath(outline.path)
                        else -> Unit
                    }
                    // 主蓝外晕 + 白色内高光，两层描边一起呼吸
                    drawPath(
                        path = path,
                        color = DragonCatBlue.PrimaryBlue.copy(alpha = a * 0.30f),
                        style = Stroke(width = 3.4.dp.toPx())
                    )
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = a * 0.55f),
                        style = Stroke(width = 1.7.dp.toPx())
                    )
                }
            }
            .onGloballyPositioned { coords ->
                pos = coords.positionInWindow().toIntOffset()
                var root: LayoutCoordinates = coords
                while (true) {
                    val parent = root.parentLayoutCoordinates
                    if (parent == null || parent === root) break
                    root = parent
                }
                if (root.size.width > 0 && root.size.height > 0) windowSize = root.size
            }
            .clip(shape)
            .background(solidBase.copy(alpha = if (glass.mode == GlassMode.SOLID) 1f else alpha))
            .then(
                when {
                    !showBorder -> Modifier
                    // 玻璃：上亮下暗的渐变描边，比单色白线更像真实玻璃的边缘反光
                    glass.mode != GlassMode.SOLID -> Modifier.border(
                        width = 1.3.dp,
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.82f),
                            0.42f to Color.White.copy(alpha = 0.24f),
                            1f to Color.White.copy(alpha = 0.46f)
                        ),
                        shape = shape
                    )
                    // 实色（默认）：也给一条极淡的亮边，避免界面看起来「死板」
                    else -> Modifier.border(1.dp, DragonCatBlue.GlassBorderSoft, shape)
                }
            )
    ) {
        // 预烘焙模糊背景：按窗口像素精确裁切对应区域（不会错位、不撑高卡片）
        val baked = backdropState?.image
        if (backdrop && glass.mode != GlassMode.SOLID && baked != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind { drawBackdropSlice(baked, pos, windowSize) }
            )
            // 一层半透明白压在糊图之上，才是玻璃的「磨砂」观感
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = alpha))
            )
        }

        // 顶部一层极淡的内高光：玻璃上缘反光，让面板「薄」一点、更像磨砂玻璃
        if (glass.mode != GlassMode.SOLID) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.18f),
                            0.28f to Color.White.copy(alpha = 0.04f),
                            1f to Color.Transparent
                        )
                    )
            )
        }

        content()
    }
}

private fun Offset.toIntOffset(): IntOffset =
    IntOffset(x.roundToInt(), y.roundToInt())

/** 面板上通用的一层极淡白色，用于统一色调 */
@Composable
fun GlassVeil(alpha: Float = 0.06f) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = alpha))
    )
}

/**
 * 把整屏糊图裁出「面板在窗口中的那块」，画满整个面板。
 * pos 是面板左上角在窗口里的像素坐标，windowSize 是窗口像素尺寸，
 * 位图宽高与窗口宽高成同一比例，因此缩放系数一致。
 */
private fun DrawScope.drawBackdropSlice(
    bitmap: ImageBitmap,
    pos: IntOffset,
    windowSize: IntSize
) {
    if (windowSize.width <= 0 || windowSize.height <= 0) return
    if (size.width <= 0f || size.height <= 0f) return

    val scaleX = bitmap.width.toFloat() / windowSize.width.toFloat()
    val scaleY = bitmap.height.toFloat() / windowSize.height.toFloat()

    val srcLeft = pos.x * scaleX
    val srcTop = pos.y * scaleY
    val srcWidth = size.width * scaleX
    val srcHeight = size.height * scaleY

    // 边界夹紧，防止越界采样造成透明边缘
    val left = srcLeft.roundToInt().coerceIn(0, max(0, bitmap.width - 1))
    val top = srcTop.roundToInt().coerceIn(0, max(0, bitmap.height - 1))
    val w = srcWidth.roundToInt().coerceIn(1, bitmap.width - left)
    val h = srcHeight.roundToInt().coerceIn(1, bitmap.height - top)

    drawImage(
        image = bitmap,
        srcOffset = IntOffset(left, top),
        srcSize = IntSize(w, h),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
    )
}

private fun max(a: Int, b: Int): Int = if (a > b) a else b
