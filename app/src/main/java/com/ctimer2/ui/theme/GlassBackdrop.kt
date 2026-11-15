package com.ctimer2.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.ctimer2.R
import com.ctimer2.util.BackgroundStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * 预烘焙玻璃背景。
 *
 * 之前每块玻璃面板都在绘制时对整屏背景做一次 RenderEffect 模糊，导致：
 * 1）滚动 / 拖动时每帧重算，掉帧；
 * 2）模糊结果与真实背景对不齐，看起来「材质和背景不是同一块」。
 *
 * 这里改成：**整屏背景只糊一次（下采样 → 双线性放大模拟高斯模糊）**，
 * 生成一张与屏幕同比例的位图缓存起来。所有面板只是把这张图平移裁切，
 * 绘制成本接近贴图，滚动时不再重算模糊，而且面板内外永远是同一张图，不会有断层。
 */
data class GlassBackdrop(
    /** 已按屏幕比例裁好、并糊过的整屏背景 */
    val image: ImageBitmap? = null,
    /** 与 source 缓存对应的版本号，变化时触发重建 */
    val version: Int = 0
)

val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

/**
 * 生成玻璃背景位图。
 *
 * @param blurDp 面板模糊强度（dp），越大下采样越狠、糊得越厉害
 * @param screenWpx / screenHpx 目标位图尺寸（屏幕像素）
 */
suspend fun buildGlassBackdrop(
    context: Context,
    custom: ImageBitmap?,
    blurDp: Float,
    screenWpx: Int,
    screenHpx: Int,
    version: Int
): GlassBackdrop = withContext(Dispatchers.Default) {
    if (blurDp <= 0f || screenWpx <= 0 || screenHpx <= 0) {
        return@withContext GlassBackdrop(null, version)
    }
    runCatching {
        val src: Bitmap = custom?.asAndroidBitmap() ?: BitmapFactory.decodeResource(
            context.resources,
            R.drawable.default_background
        )

        // 1) 先按屏幕比例居中裁剪，保证与真实背景的显示区域一一对应
        val targetRatio = screenWpx.toFloat() / screenHpx.toFloat()
        val srcRatio = src.width.toFloat() / src.height.toFloat()
        val cropW: Int
        val cropH: Int
        if (srcRatio > targetRatio) {
            cropH = src.height
            cropW = (src.height * targetRatio).toInt()
        } else {
            cropW = src.width
            cropH = (src.width / targetRatio).toInt()
        }
        val cropX = ((src.width - cropW) / 2).coerceAtLeast(0)
        val cropY = ((src.height - cropH) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(src, cropX, cropY, cropW, cropH)

        // 2) 缩到屏幕尺寸，作为“清晰基准”
        val base = Bitmap.createScaledBitmap(cropped, screenWpx, screenHpx, true)

        // 3) 下采样再放大 = 廉价高斯近似。blurDp 18 约等于 1/14 采样
        val factor = (1f + blurDp / 1.4f).toInt().coerceIn(2, 40)
        val smallW = max(1, screenWpx / factor)
        val smallH = max(1, screenHpx / factor)
        val tiny = Bitmap.createScaledBitmap(base, smallW, smallH, true)
        // 再来一次极轻微的二次下采样，让边缘更柔（模拟高斯的两级衰减）
        val tiny2 = Bitmap.createScaledBitmap(tiny, max(1, smallW / 2), max(1, smallH / 2), true)
        val baked = Bitmap.createScaledBitmap(tiny2, screenWpx, screenHpx, true)

        // 4) 叠一点点白，避免糊完偏暗
        runCatching {
            val canvas = android.graphics.Canvas(baked)
            canvas.drawColor(android.graphics.Color.argb(26, 255, 255, 255))
        }

        GlassBackdrop(baked.asImageBitmap(), version)
    }.getOrDefault(GlassBackdrop(null, version))
}
