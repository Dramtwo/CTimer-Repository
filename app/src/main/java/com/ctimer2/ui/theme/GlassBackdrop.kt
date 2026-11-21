package com.ctimer2.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.ctimer2.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 预烘焙的玻璃底图（backdrop）。
 *
 * 设计要点（对应之前踩过的坑）：
 * 1. **模糊只算一次**：整屏背景烘焙成一张位图，各面板只做平移裁切，滚动时零计算、不掉帧。
 * 2. **必须和当前背景严格绑定**：早期版本把背景解码和烘焙拆成两条独立的异步流程，
 *    换图时烘焙会抢在解码完成前读到旧图（或 null → 默认图），之后就再也不重算，
 *    表现就是「换成自定义背景，玻璃里还是默认背景」。现在用 [signature] 做指纹，
 *    背景图 / 两个模糊值 / 尺寸任一变化都重算；背景没就绪时保持旧值，绝不烘焙半成品。
 * 3. **模糊质量**：不再用「下采样两次 + 双线性放大」这种会有块状感的做法，
 *    改为在 1/4 尺寸的小图上做三轮盒式模糊（≈ 高斯），再放大回屏，
 *    磨砂过渡更均匀、没有马赛克方块。
 * 4. **和外部背景一致**：烘焙时把「背景整体模糊」也算进去，
 *    面板取的景与面板外看到的背景是同一套模糊，不会内外对不上。
 */
data class GlassBackdrop(
    /** 已按屏幕比例裁好、并糊过的整屏背景 */
    val image: ImageBitmap? = null,
    /** 这份结果的指纹；与当前请求不一致说明还在用旧图 */
    val signature: String = "",
    /** 兼容旧调用 */
    val version: Int = 0
)

val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

/**
 * 背景指纹：自定义图用对象身份 + 尺寸，默认图用资源名。
 * 只要用户换了图，identityHashCode 就会变，玻璃必然重算。
 */
fun backgroundSignature(custom: ImageBitmap?, version: Int): String =
    if (custom == null) "default@$version"
    else "custom:${System.identityHashCode(custom)}:${custom.width}x${custom.height}@$version"

/**
 * 生成玻璃底图。
 *
 * @param blurDp   面板模糊强度（dp）
 * @param bgBlurDp 背景整体模糊（dp），与 AppBackground 上的模糊保持一致
 * @param density  屏幕密度，用来把 dp 换成像素
 */
suspend fun buildGlassBackdrop(
    context: Context,
    custom: ImageBitmap?,
    blurDp: Float,
    bgBlurDp: Float,
    screenWpx: Int,
    screenHpx: Int,
    density: Float,
    version: Int
): GlassBackdrop = withContext(Dispatchers.Default) {
    val signature = backgroundSignature(custom, version) +
        "|b$blurDp|bb$bgBlurDp|${screenWpx}x${screenHpx}"
    if (screenWpx <= 0 || screenHpx <= 0) {
        return@withContext GlassBackdrop(null, signature, version)
    }
    runCatching {
        val src: Bitmap = custom?.asAndroidBitmap() ?: BitmapFactory.decodeResource(
            context.resources,
            R.drawable.default_background
        )

        // 1) 居中裁剪到屏幕比例，保证与真实背景显示区域一一对应
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
        val cropped = Bitmap.createBitmap(src, cropX, cropY, max(1, cropW), max(1, cropH))

        // 2) 缩到 1/4 尺寸的工作图（模糊在这个小图上做，成本低且更均匀）
        val workW = max(8, screenWpx / 4)
        val workH = max(8, screenHpx / 4)
        val work = Bitmap.createScaledBitmap(cropped, workW, workH, true)

        val pixels = IntArray(workW * workH)
        work.getPixels(pixels, 0, workW, 0, 0, workW, workH)

        // 3) 两级模糊半径（dp → 像素 → 折算到工作图比例），按高斯叠加近似合并
        val panelPx = blurDp * density
        val bgPx = bgBlurDp * density
        val equivPx = sqrt(panelPx * panelPx + bgPx * bgPx)
        val radiusInWork = (equivPx / 4f).roundToInt().coerceIn(1, 46)

        // 4) 三轮盒式模糊 ≈ 高斯（每轮横一次、纵一次，滑动窗口实现）
        repeat(3) {
            boxBlurPass(pixels, workW, workH, radiusInWork)
        }

        // 5) 工作图 → 放大回屏幕（双线性放大让边缘继续变柔）
        val blurredSmall = Bitmap.createBitmap(workW, workH, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, workW, 0, 0, workW, workH)
        }
        val baked = Bitmap.createScaledBitmap(blurredSmall, screenWpx, screenHpx, true)

        // 6) 压一层极淡白雾：糊完容易偏灰偏暗，加一点亮度才像「磨砂白玻璃」
        runCatching {
            val canvas = android.graphics.Canvas(baked)
            canvas.drawColor(android.graphics.Color.argb(20, 255, 255, 255))
        }

        GlassBackdrop(baked.asImageBitmap(), signature, version)
    }.getOrDefault(GlassBackdrop(null, signature, version))
}

/**
 * 盒式模糊（滑动窗口累加，O(n)），横竖各做一次。
 * 直接在 IntArray 上操作，避免 Bitmap 反复读写。
 */
private fun boxBlurPass(pixels: IntArray, w: Int, h: Int, radius: Int) {
    if (radius < 1 || w < 2 || h < 2) return

    // 横向
    val tmp = IntArray(w * h)
    val count = radius * 2 + 1
    for (y in 0 until h) {
        var sumA = 0; var sumR = 0; var sumG = 0; var sumB = 0
        val rowStart = y * w
        for (dx in -radius..radius) {
            val x = dx.coerceIn(0, w - 1)
            val c = pixels[rowStart + x]
            sumA += (c shr 24) and 0xFF
            sumR += (c shr 16) and 0xFF
            sumG += (c shr 8) and 0xFF
            sumB += c and 0xFF
        }
        for (x in 0 until w) {
            tmp[rowStart + x] = ((sumA / count) shl 24) or
                ((sumR / count) shl 16) or
                ((sumG / count) shl 8) or
                (sumB / count)
            val outX = (x - radius).coerceIn(0, w - 1)
            val inX = (x + radius + 1).coerceIn(0, w - 1)
            val out = pixels[rowStart + outX]
            val inn = pixels[rowStart + inX]
            sumA += ((inn shr 24) and 0xFF) - ((out shr 24) and 0xFF)
            sumR += ((inn shr 16) and 0xFF) - ((out shr 16) and 0xFF)
            sumG += ((inn shr 8) and 0xFF) - ((out shr 8) and 0xFF)
            sumB += (inn and 0xFF) - (out and 0xFF)
        }
    }

    // 纵向（写回 pixels）
    for (x in 0 until w) {
        var sumA = 0; var sumR = 0; var sumG = 0; var sumB = 0
        for (dy in -radius..radius) {
            val y = dy.coerceIn(0, h - 1)
            val c = tmp[y * w + x]
            sumA += (c shr 24) and 0xFF
            sumR += (c shr 16) and 0xFF
            sumG += (c shr 8) and 0xFF
            sumB += c and 0xFF
        }
        for (y in 0 until h) {
            pixels[y * w + x] = ((sumA / count) shl 24) or
                ((sumR / count) shl 16) or
                ((sumG / count) shl 8) or
                (sumB / count)
            val outY = (y - radius).coerceIn(0, h - 1)
            val inY = (y + radius + 1).coerceIn(0, h - 1)
            val out = tmp[outY * w + x]
            val inn = tmp[inY * w + x]
            sumA += ((inn shr 24) and 0xFF) - ((out shr 24) and 0xFF)
            sumR += ((inn shr 16) and 0xFF) - ((out shr 16) and 0xFF)
            sumG += ((inn shr 8) and 0xFF) - ((out shr 8) and 0xFF)
            sumB += (inn and 0xFF) - (out and 0xFF)
        }
    }
}
