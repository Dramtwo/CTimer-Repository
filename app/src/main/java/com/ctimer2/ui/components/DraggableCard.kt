package com.ctimer2.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp

/**
 * 可长按拖动的卡片容器。
 * - 长按卡片任意位置（约 400ms）进入拖动，之后手指移到哪卡片跟到哪（逐帧更新，无瞬移）
 * - 位移走 graphicsLayer，不触发重组
 * - 拖动时卡片轻微放大作为反馈，位置以 dp 持久化
 */
@Composable
fun DraggableCard(
    cardKey: String,
    initialOffset: Pair<Float, Float>,
    onOffsetCommit: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    fillRemaining: Boolean = false,
    header: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val offsetX = remember { mutableFloatStateOf(initialOffset.first) }
    val offsetY = remember { mutableFloatStateOf(initialOffset.second) }
    var dragging by remember { mutableStateOf(false) }
    // 「拿起来再放下」：拖动时放大到 1.05 并加深阴影，松手用弹簧瞬间弹回
    // 低性能档位不做放大与阴影，减少合成层开销
    val perf = com.ctimer2.ui.theme.LocalPerfMode.current
    val lift = dragging && perf != com.ctimer2.ui.theme.PerfMode.LOW
    val scale by animateFloatAsState(
        targetValue = if (lift) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (lift) 18f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardElevation"
    )

    // 外部复位（侧栏底部「重置卡片位置」）时把内部位移同步回来
    LaunchedEffect(initialOffset) {
        offsetX.floatValue = initialOffset.first
        offsetY.floatValue = initialOffset.second
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                translationX = offsetX.floatValue.dp.toPx()
                translationY = offsetY.floatValue.dp.toPx()
                scaleX = scale
                scaleY = scale
                // 阴影也走渲染层：拿起来时浮起来，松手落回去
                shape = RoundedCornerShape(16.dp)
                shadowElevation = elevation.dp.toPx()
                ambientShadowColor = androidx.compose.ui.graphics.Color(0x335B9BD5)
                spotShadowColor = androidx.compose.ui.graphics.Color(0x405B9BD5)
            }
            .pointerInput(cardKey) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // 立即消费按下：子元素（切换按钮）已在 Main pass 先行处理点击，
                    // 父级根 Box 便不会启动计时，避免拖动结束后被误触发一次计时。
                    down.consume()
                    // 等待长按；普通点击会在此返回 null，交还给子元素
                    val longPress = awaitLongPressOrCancellation(down.id)
                        ?: return@awaitEachGesture

                    longPress.consume()
                    dragging = true
                    val density = this.density
                    try {
                        drag(longPress.id) { change ->
                            // 逐帧更新，手指到哪卡片到哪
                            offsetX.floatValue = (offsetX.floatValue +
                                    change.positionChange().x / density).coerceIn(-160f, 360f)
                            offsetY.floatValue = (offsetY.floatValue +
                                    change.positionChange().y / density).coerceIn(-160f, 320f)
                            change.consume()
                        }
                    } finally {
                        dragging = false
                    }
                    onOffsetCommit(offsetX.floatValue, offsetY.floatValue)
                }
            }
    ) {
        header?.let {
            Box { it() }
        }
        if (fillRemaining) {
            Box(modifier = Modifier.weight(1f)) { content() }
        } else {
            content()
        }
    }
}
