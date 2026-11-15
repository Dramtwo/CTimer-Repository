package com.ctimer2.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.ctimer2.data.SessionEntity
import com.ctimer2.data.SolveRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 成绩备份 / 导入。
 * 自动备份写到公共下载目录 Download/CTimer2/ctimer2_scores.json（Android 10+ 走 MediaStore，无需权限），
 * 换包名重装后可在文件管理器里找到该文件，再用「导入成绩」把它读回来。
 */
object ScoreBackup {

    const val FILE_NAME = "ctimer2_scores.json"
    const val DIR_NAME = "CTimer2"

    data class Parsed(val sessions: List<SessionEntity>, val solves: List<SolveRecord>)

    fun buildJson(sessions: List<SessionEntity>, solves: List<SolveRecord>): String {
        val root = JSONObject()
        root.put("app", "CTimer2")
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val sArr = JSONArray()
        sessions.forEach { s ->
            sArr.put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("createdAt", s.createdAt)
                put("sortOrder", s.sortOrder)
            })
        }
        root.put("sessions", sArr)

        val rArr = JSONArray()
        solves.forEach { r ->
            rArr.put(JSONObject().apply {
                put("sessionId", r.sessionId)
                put("timeMs", r.timeMs)
                put("timestamp", r.timestamp)
            })
        }
        root.put("solves", rArr)
        return root.toString()
    }

    fun parse(json: String): Parsed? = try {
        val root = JSONObject(json)
        val sessions = mutableListOf<SessionEntity>()
        val solves = mutableListOf<SolveRecord>()

        val sArr = root.optJSONArray("sessions") ?: JSONArray()
        for (i in 0 until sArr.length()) {
            val o = sArr.getJSONObject(i)
            sessions.add(
                SessionEntity(
                    id = o.optLong("id", 0),
                    name = o.optString("name", "导入列表"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    sortOrder = o.optInt("sortOrder", i)
                )
            )
        }

        val rArr = root.optJSONArray("solves") ?: JSONArray()
        for (i in 0 until rArr.length()) {
            val o = rArr.getJSONObject(i)
            solves.add(
                SolveRecord(
                    sessionId = o.optLong("sessionId", 0),
                    timeMs = o.optLong("timeMs", 0),
                    timestamp = o.optLong("timestamp", 0)
                )
            )
        }
        if (sessions.isEmpty() && solves.isEmpty()) null else Parsed(sessions, solves)
    } catch (_: Throwable) {
        null
    }

    /** 自动备份到公共下载目录，返回是否成功 */
    fun autoBackup(context: Context, json: String): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DIR_NAME")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            // 已存在同名文件先删掉，避免堆积
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Downloads._ID),
                "${MediaStore.Downloads.DISPLAY_NAME}=?",
                arrayOf(FILE_NAME),
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val id = c.getLong(0)
                    resolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        "${MediaStore.Downloads._ID}=?",
                        arrayOf(id.toString())
                    )
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return false
            resolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                DIR_NAME
            )
            if (!dir.exists()) dir.mkdirs()
            File(dir, FILE_NAME).writeText(json)
            true
        }
    } catch (_: Throwable) {
        false
    }

    /** 读取公共下载目录里的自动备份 */
    fun readAutoBackup(context: Context): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val cursor = resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Downloads._ID),
                "${MediaStore.Downloads.DISPLAY_NAME}=?",
                arrayOf(FILE_NAME),
                null
            )
            var json: String? = null
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val id = c.getLong(0)
                    val uri = Uri.withAppendedPath(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    json = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
            }
            json
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                DIR_NAME
            )
            val f = File(dir, FILE_NAME)
            if (f.exists()) f.readText() else null
        }
    } catch (_: Throwable) {
        null
    }

    fun readFromUri(context: Context, uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    } catch (_: Throwable) {
        null
    }

    fun writeToUri(context: Context, uri: Uri, json: String): Boolean = try {
        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
        true
    } catch (_: Throwable) {
        false
    }

    /** 加载设置页预览用的背景图（按目标尺寸采样，避免 OOM） */
    fun loadBitmap(context: Context, uriString: String, reqW: Int = 1600, reqH: Int = 1000): Bitmap? =
        try {
            val uri = Uri.parse(uriString)
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            var sample = 1
            while (opts.outWidth / sample > reqW || opts.outHeight / sample > reqH) sample *= 2

            val opts2 = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts2)
            }
        } catch (_: Throwable) {
            null
        }
}
