package com.ctimer2.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("ctimer2_settings")

class SessionDataStore(private val context: Context) {
    private val KEY_SESSION_ID = longPreferencesKey("current_session_id")

    val currentSessionId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SESSION_ID]
    }

    suspend fun setCurrentSession(id: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SESSION_ID] = id
        }
    }

    // ---- 各列表历史最佳纪录 ----

    fun bestSolveKey(sessionId: Long) = longPreferencesKey("best_solve_$sessionId")
    fun bestAo5Key(sessionId: Long) = doublePreferencesKey("best_ao5_$sessionId")

    fun bestSolve(sessionId: Long): Flow<Long?> = context.dataStore.data.map { it[bestSolveKey(sessionId)] }
    fun bestAo5(sessionId: Long): Flow<Double?> = context.dataStore.data.map { it[bestAo5Key(sessionId)] }

    suspend fun setBestSolve(sessionId: Long, ms: Long) {
        context.dataStore.edit { it[bestSolveKey(sessionId)] = ms }
    }

    suspend fun setBestAo5(sessionId: Long, seconds: Double) {
        context.dataStore.edit { it[bestAo5Key(sessionId)] = seconds }
    }

    // ---- 卡片拖拽位置（dp 偏移） ----

    private fun cardXKey(card: String) = floatPreferencesKey("card_x_$card")
    private fun cardYKey(card: String) = floatPreferencesKey("card_y_$card")

    fun cardOffset(card: String): Flow<Pair<Float, Float>> = context.dataStore.data.map { prefs ->
        (prefs[cardXKey(card)] ?: 0f) to (prefs[cardYKey(card)] ?: 0f)
    }

    suspend fun setCardOffset(card: String, x: Float, y: Float) {
        context.dataStore.edit { prefs ->
            prefs[cardXKey(card)] = x
            prefs[cardYKey(card)] = y
        }
    }

    // ---- 外观设置：背景图 / 字体 / 字体颜色 ----

    private val KEY_BG_URI = stringPreferencesKey("bg_uri")
    private val KEY_FONT = stringPreferencesKey("font_family")
    private val KEY_FONT_COLOR = longPreferencesKey("font_color")

    fun backgroundUri(): Flow<String?> = context.dataStore.data.map { it[KEY_BG_URI] }
    fun fontFamilyName(): Flow<String?> = context.dataStore.data.map { it[KEY_FONT] }
    fun fontColor(): Flow<Long?> = context.dataStore.data.map { it[KEY_FONT_COLOR] }

    suspend fun setBackgroundUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(KEY_BG_URI) else prefs[KEY_BG_URI] = uri
        }
    }

    suspend fun setFontFamilyName(name: String) {
        context.dataStore.edit { it[KEY_FONT] = name }
    }

    suspend fun setFontColor(argb: Long) {
        context.dataStore.edit { it[KEY_FONT_COLOR] = argb }
    }

    // ---- 材质：玻璃模式 / 面板模糊 / 背景模糊 ----

    private val KEY_GLASS_MODE = stringPreferencesKey("glass_mode")
    private val KEY_GLASS_BLUR = floatPreferencesKey("glass_blur")
    private val KEY_BG_BLUR = floatPreferencesKey("bg_blur")
    private val KEY_USER_NAME = stringPreferencesKey("user_name")

    /** 云端用户名（只要用户名，不需要密码） */
    fun userName(): Flow<String?> = context.dataStore.data.map { it[KEY_USER_NAME] }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun clearUserName() {
        context.dataStore.edit { it.remove(KEY_USER_NAME) }
    }

    fun glassMode(): Flow<String?> = context.dataStore.data.map { it[KEY_GLASS_MODE] }
    fun glassBlur(): Flow<Float?> = context.dataStore.data.map { it[KEY_GLASS_BLUR] }
    fun bgBlur(): Flow<Float?> = context.dataStore.data.map { it[KEY_BG_BLUR] }

    suspend fun setGlassMode(name: String) {
        context.dataStore.edit { it[KEY_GLASS_MODE] = name }
    }

    suspend fun setGlassBlur(value: Float) {
        context.dataStore.edit { it[KEY_GLASS_BLUR] = value }
    }

    suspend fun setBgBlur(value: Float) {
        context.dataStore.edit { it[KEY_BG_BLUR] = value }
    }
}
