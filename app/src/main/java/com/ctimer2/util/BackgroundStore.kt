package com.ctimer2.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * 自定义背景：把系统相册选中的图复制到应用私有目录保存。
 * 系统相册（Photo Picker）给的 URI 授权在重启后常常失效，所以不能直接依赖它，
 * 落盘成文件后重启必然还在。
 */
object BackgroundStore {
    private const val FILE_NAME = "custom_background.jpg"
    private const val MAX_SIDE = 1400

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun hasCustom(context: Context): Boolean = file(context).exists()

    /** 从相册 URI 解码后压缩落盘，返回是否成功 */
    fun saveFromUri(context: Context, uri: Uri): Boolean {
        return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
        if (opts.outWidth <= 0) return false

        var sample = 1
        while (opts.outWidth / sample > MAX_SIDE * 2 || opts.outHeight / sample > MAX_SIDE * 2) {
            sample *= 2
        }
        val opts2 = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts2)
        } ?: return false

        // 等比缩到最长边不超过 MAX_SIDE
        val longest = maxOf(bitmap.width, bitmap.height)
        val scaled = if (longest > MAX_SIDE) {
            val ratio = MAX_SIDE.toFloat() / longest
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else bitmap

        file(context).outputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        true
        } catch (_: Throwable) {
            false
        }
    }

    fun load(context: Context): Bitmap? = try {
        val f = file(context)
        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    } catch (_: Throwable) {
        null
    }

    fun delete(context: Context) {
        try {
            val f = file(context)
            if (f.exists()) f.delete()
        } catch (_: Throwable) {
        }
    }
}
