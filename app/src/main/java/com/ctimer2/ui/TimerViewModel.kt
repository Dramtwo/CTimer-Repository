package com.ctimer2.ui

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ctimer2.data.AppDatabase
import com.ctimer2.data.SessionDataStore
import com.ctimer2.data.SessionEntity
import com.ctimer2.data.SolveRecord
import com.ctimer2.network.GitHubRepository
import com.ctimer2.ui.components.ResultRow
import com.ctimer2.ui.components.WheelSlot
import com.ctimer2.ui.theme.DragonCatBlue
import com.ctimer2.ui.theme.GlassMode
import com.ctimer2.ui.theme.GlassStyle
import com.ctimer2.util.BackgroundStore
import com.ctimer2.util.Scramble
import com.ctimer2.util.ScoreBackup
import com.ctimer2.util.Stats
import com.ctimer2.util.Summary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TimerState { IDLE, HOLDING, READY, RUNNING }

/** 右侧内容区当前显示的功能页（切换式，同一时刻最多一个） */
enum class AppPageKey { SUMMARY, FORMULA, SETTINGS, LEADERBOARD, USER }

/** 卡片区展示的列表视图（总评已独立成页，见 summaryPageOpen） */
enum class ListViewMode { BY_TIME, BY_SCORE }

/** 可切换的字体 */
enum class AppFont(val label: String) {
    MONO("等宽"),
    SANS("黑体"),
    SERIF("宋体"),
    CURSIVE("手写");

    val fontFamily: androidx.compose.ui.text.font.FontFamily
        get() = when (this) {
            MONO -> androidx.compose.ui.text.font.FontFamily.Monospace
            SANS -> androidx.compose.ui.text.font.FontFamily.SansSerif
            SERIF -> androidx.compose.ui.text.font.FontFamily.Serif
            CURSIVE -> androidx.compose.ui.text.font.FontFamily.Cursive
        }
}

/** 可选的字体颜色 */
enum class FontPalette(val label: String, val argb: Long) {
    INK("深蓝灰", 0xFF23384D),
    BLACK("墨黑", 0xFF141414),
    GREEN("墨绿", 0xFF1F5A46),
    WINE("酒红", 0xFF7A2B33),
    BLUE("靛蓝", 0xFF2C4A82);

    val color: Color get() = Color(argb)
}

class TimerViewModel(app: Application) : AndroidViewModel(app) {

    private val sessionDao = AppDatabase.getInstance(app).sessionDao()
    private val solveDao = AppDatabase.getInstance(app).solveDao()
    private val dataStore = SessionDataStore(app)

    // ---- 计时状态 ----
    var timerState by mutableStateOf(TimerState.IDLE)
        private set
    var startNanos by mutableStateOf(0L)
        private set
    var lastFinishedMs by mutableStateOf<Long?>(null)
        private set
    var flashColor by mutableStateOf<Color?>(null)
        private set

    // ---- 多列表状态 ----
    var sessions by mutableStateOf<List<SessionEntity>>(emptyList())
        private set
    var currentSessionId by mutableStateOf<Long?>(null)
        private set
    var solves by mutableStateOf<List<SolveRecord>>(emptyList())
        private set

    // ---- 抽屉 ----
    var drawerOpen by mutableStateOf(false)
        private set

    // ---- 列表视图模式 ----
    var viewModeState by mutableStateOf(ListViewMode.BY_TIME)
        private set

    // ---- 用户系统页面 ----
    var userPageOpen by mutableStateOf(false)
        private set

    fun toggleUserPage() = switchPage(AppPageKey.USER)

    fun closeUserPage() {
        userPageOpen = false
    }

    // ---- 页面切换核心：同一时刻只有一个功能页开着 ----
    private fun switchPage(key: AppPageKey) {
        val wasOpen = when (key) {
            AppPageKey.SUMMARY -> summaryPageOpen
            AppPageKey.FORMULA -> formulaPageOpen
            AppPageKey.SETTINGS -> settingsPageOpen
            AppPageKey.LEADERBOARD -> leaderboardPageOpen
            AppPageKey.USER -> userPageOpen
        }
        // 先全关：保证任何时候都只有一个页面，而不是层层叠加
        summaryPageOpen = false
        formulaPageOpen = false
        settingsPageOpen = false
        leaderboardPageOpen = false
        userPageOpen = false
        actionMenuOpen = false
        browseIndex = null
        drawerOpen = false
        if (wasOpen) return  // 再次点击同一个图标 = 收起，回到主界面

        when (key) {
            AppPageKey.SUMMARY -> summaryPageOpen = true
            AppPageKey.FORMULA -> formulaPageOpen = true
            AppPageKey.SETTINGS -> settingsPageOpen = true
            AppPageKey.LEADERBOARD -> leaderboardPageOpen = true
            AppPageKey.USER -> userPageOpen = true
        }
    }

    // ---- 成绩总评独立页面 ----
    var summaryPageOpen by mutableStateOf(false)
        private set

    /** 点侧栏图标是切换语义：已经在总评页时再点一次就回主界面，不用再找返回按钮 */
    fun toggleSummaryPage() = switchPage(AppPageKey.SUMMARY)

    fun closeSummaryPage() {
        summaryPageOpen = false
    }

    // ---- 打乱公式（csTimer 风格 random-move） ----
    // 带历史，轮盘可以前后翻：下一条 = 更新的，上一条 = 更早的
    private val scrambleHistory = mutableStateListOf(Scramble.generate())

    /** 当前显示历史里的第几条，0 = 最新 */
    var scrambleIndex by mutableStateOf(0)
        private set

    /** 当前打乱公式（历史与索引都是 State，读取即参与重组） */
    val scramble: String
        get() = scrambleHistory.getOrNull(scrambleIndex) ?: scrambleHistory.firstOrNull() ?: ""

    /** 历史里可翻的条数 */
    val scrambleHistorySize: Int get() = scrambleHistory.size

    fun refreshScramble() {
        scrambleHistory.add(0, Scramble.generate())
        while (scrambleHistory.size > 60) scrambleHistory.removeAt(scrambleHistory.lastIndex)
        scrambleIndex = 0
    }

    /** 轮盘：往前翻到更早的一条打乱 */
    fun scramblePrev() {
        if (scrambleIndex < scrambleHistory.lastIndex) scrambleIndex += 1
    }

    /** 轮盘：回到更新的一条打乱 */
    fun scrambleNext() {
        if (scrambleIndex > 0) scrambleIndex -= 1
    }

    // ---- 公式速记独立页面 ----
    var formulaPageOpen by mutableStateOf(false)
        private set

    fun toggleFormulaPage() = switchPage(AppPageKey.FORMULA)

    fun closeFormulaPage() {
        formulaPageOpen = false
    }

    // ---- 云端排行榜独立页面 ----
    var leaderboardPageOpen by mutableStateOf(false)
        private set

    fun toggleLeaderboardPage() = switchPage(AppPageKey.LEADERBOARD)

    fun closeLeaderboardPage() {
        leaderboardPageOpen = false
    }

    // ---- 用户系统（只有用户名，无密码） ----
    var userName by mutableStateOf<String?>(null)
        private set

    fun applyUserName(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        userName = clean
        viewModelScope.launch {
            dataStore.setUserName(clean)
            showHint("已登录：$clean")
        }
        // 切换用户后重新按新用户拉一次自己的成绩
        syncFromCloud(silent = false)
    }

    fun logout() {
        userName = null
        viewModelScope.launch {
            dataStore.clearUserName()
            showHint("已退出登录")
        }
    }

    // ---- 云端同步状态 ----
    /** 上次云端同步的结果，null 表示尚未同步/无异常 */
    var cloudStatus by mutableStateOf<String?>(null)
        private set
    var cloudSyncing by mutableStateOf(false)
        private set
    /** 顶部上传提示条文案，null 表示不显示 */
    var cloudUploadHint by mutableStateOf<String?>(null)
        private set

    fun clearUploadHint() { cloudUploadHint = null }

    /** 当前列表名（上传时用于标注成绩归属） */
    val currentSessionName: String
        get() = sessions.firstOrNull { it.id == currentSessionId }?.name ?: "默认列表"

    /**
     * 从 GitHub 云端拉回全部成绩，补进本地。
     * 去重键 = 列表名 + 时间戳 + 成绩毫秒，因此重复启动不会造出重复记录。
     */
    fun syncFromCloud(silent: Boolean = false) {
        if (cloudSyncing) return
        cloudSyncing = true
        viewModelScope.launch {
            val remote = withContext(Dispatchers.IO) {
                runCatching { GitHubRepository.fetchScores(userName) }.getOrNull()
            }
            cloudSyncing = false
            if (remote == null) {
                if (!silent) cloudStatus = "云端连接失败"
                return@launch
            }
            if (remote.isEmpty()) {
                if (!silent) cloudStatus = "云端暂无成绩"
                return@launch
            }

            // 现有成绩的指纹集合，用于去重
            val existingKeys = mutableSetOf<String>()
            val nameOf = sessions.associate { it.id to it.name }
            solveDao.getAll().forEach { r ->
                val sname = nameOf[r.sessionId] ?: return@forEach
                existingKeys += "$sname|${r.timestamp}|${r.timeMs}"
            }

            var added = 0
            remote.forEach { rs ->
                if (rs.key in existingKeys) return@forEach
                val sessionName = rs.session.ifBlank { "云端成绩" }
                var target = sessionDao.getAll().firstOrNull { it.name == sessionName }
                if (target == null) {
                    val order = sessionDao.getAll().size
                    val newId = sessionDao.insert(
                        SessionEntity(
                            name = sessionName,
                            createdAt = if (rs.timestamp > 0) rs.timestamp else System.currentTimeMillis(),
                            sortOrder = order
                        )
                    )
                    target = SessionEntity(newId, sessionName, rs.timestamp, order)
                }
                val sid = target.id
                solveDao.insert(
                    SolveRecord(
                        sessionId = sid,
                        timeMs = rs.timeMs,
                        timestamp = if (rs.timestamp > 0) rs.timestamp else System.currentTimeMillis()
                    )
                )
                existingKeys += rs.key
                added++
            }

            sessions = sessionDao.getAll()
            reloadSolves()
            cloudStatus = if (added > 0) "从云端补入 $added 条成绩" else "云端已是最新"
        }
    }

    // ---- 成绩浏览（悬浮菜单上一条/下一条） ----
    /** null 表示未进入浏览态；否则为 solves 下标 */
    var browseIndex by mutableStateOf<Int?>(null)
        private set

    // ---- 卡片拖拽位置（dp） ----
    var logoOffset by mutableStateOf(0f to 0f)
        private set
    var listOffset by mutableStateOf(0f to 0f)
        private set

    // ---- 悬浮菜单 / 轮盘 ----
    /** 有浮层打开的语义标记（侧栏隐藏、返回键拦截、整屏手势屏蔽都用它） */
    var actionMenuOpen by mutableStateOf(false)
        private set

    /** 轮盘是否展开 */
    var wheelOpen by mutableStateOf(false)
        private set

    /** 轮盘圆心 = 手指按下点（窗口像素） */
    var wheelCenterX by mutableStateOf(0f)
        private set
    var wheelCenterY by mutableStateOf(0f)
        private set

    /** 当前高亮槽位，WheelSlot.CANCEL(-1) 表示在中心死区 */
    var wheelSelected by mutableStateOf(WheelSlot.CANCEL)
        private set

    /** 在手指按下点展开轮盘 */
    fun openWheel(x: Float, y: Float) {
        if (timerState == TimerState.RUNNING) return
        if (drawerOpen) return
        wheelCenterX = x
        wheelCenterY = y
        wheelSelected = WheelSlot.CANCEL
        wheelOpen = true
        actionMenuOpen = true
        if (browseIndex == null && solves.isNotEmpty()) browseIndex = 0
    }

    /** 手指移动时更新高亮槽位 */
    fun updateWheelSlot(slot: Int) {
        if (wheelOpen && wheelSelected != slot) wheelSelected = slot
    }

    /** 松手：执行当前高亮项；停在中心死区则什么都不做（也不会误删） */
    fun releaseWheel() {
        if (!wheelOpen) return
        val slot = wheelSelected
        wheelOpen = false
        wheelSelected = WheelSlot.CANCEL
        actionMenuOpen = false
        when (slot) {
            WheelSlot.DELETE -> if (solves.isNotEmpty()) deleteCurrentSolve()
            WheelSlot.PREV_SCORE -> if (solves.isNotEmpty()) browsePrev()
            WheelSlot.NEXT_SCORE -> if (solves.isNotEmpty()) browseNext()
            WheelSlot.NEXT_SCRAMBLE -> scrambleNext()
            WheelSlot.PREV_SCRAMBLE -> scramblePrev()
            else -> Unit
        }
        browseIndex = null
    }

    /** 取消轮盘（返回键 / 手指抬起前被打断） */
    fun cancelWheel() {
        wheelOpen = false
        wheelSelected = WheelSlot.CANCEL
        actionMenuOpen = false
        browseIndex = null
    }

    /** 兼容旧的打开入口：没有坐标时按屏幕中间展开（正常手势不会走这里） */
    fun openActionMenu() {
        if (timerState == TimerState.RUNNING) return
        if (drawerOpen) return
        openWheel(wheelCenterX.takeIf { it > 0f } ?: 0f, wheelCenterY.takeIf { it > 0f } ?: 0f)
    }

    fun closeActionMenu() = cancelWheel()

    // ---- 成就字幕 ----
    /** 非空时 Logo 卡片切换为滚动横幅；7 秒后自动清除 */
    var achievement by mutableStateOf<String?>(null)
        private set
    /** 横幅副标题，如「刷新了本列表最快」 */
    var achievementLabel by mutableStateOf<String?>(null)
        private set
    private var achievementToken = 0

    private fun showAchievement(title: String, label: String) {
        achievement = title
        achievementLabel = label
        val token = ++achievementToken
        viewModelScope.launch {
            delay(7000)
            if (achievementToken == token) {
                achievement = null
                achievementLabel = null
            }
        }
    }

    private var holdJob: Job? = null

    // ---- 设置页 / 外观 ----
    var settingsPageOpen by mutableStateOf(false)
        private set
    /** 是否使用自定义背景图（图片本身存在应用私有目录，见 BackgroundStore） */
    var hasCustomBg by mutableStateOf(false)
        private set
    /** 版本号：每次更换/清除背景自增，用来触发背景重新解码 */
    var bgVersion by mutableStateOf(0)
        private set

    // ---- 材质 ----
    var glassMode by mutableStateOf(GlassMode.MICA)
        private set
    /** 面板模糊强度（dp），0 表示不模糊、纯半透明 */
    var glassBlur by mutableStateOf(18f)
        private set
    /** 背景整体模糊强度（dp），默认 0 = 背景清晰通亮 */
    var bgBlur by mutableStateOf(0f)
        private set

    fun applyGlassMode(mode: GlassMode) {
        glassMode = mode
        viewModelScope.launch { dataStore.setGlassMode(mode.name) }
    }

    fun applyGlassBlur(value: Float) {
        glassBlur = value
        viewModelScope.launch { dataStore.setGlassBlur(value) }
    }

    fun applyBgBlur(value: Float) {
        bgBlur = value
        viewModelScope.launch { dataStore.setBgBlur(value) }
    }

    /** 供 CompositionLocal 用的材质样式 */
    val glassStyle: GlassStyle
        get() = when (glassMode) {
            GlassMode.FRIT -> GlassStyle(GlassMode.FRIT, glassBlur, 0.34f)
            GlassMode.MICA -> GlassStyle(GlassMode.MICA, glassBlur * 0.35f, 0.50f)
            GlassMode.SOLID -> GlassStyle(GlassMode.SOLID, 0f, 1f)
        }
    var fontMode by mutableStateOf(AppFont.MONO)
        private set
    var fontPalette by mutableStateOf(FontPalette.INK)
        private set
    /** 操作结果提示（设置页顶部显示，几秒后自动消失） */
    var hintMessage by mutableStateOf<String?>(null)
        private set

    fun toggleSettingsPage() = switchPage(AppPageKey.SETTINGS)

    fun closeSettingsPage() {
        settingsPageOpen = false
    }

    /** 把相册选中的图片复制进私有目录并立即生效 */
    fun saveCustomBackground(uri: android.net.Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                BackgroundStore.saveFromUri(getApplication(), uri)
            }
            if (ok) {
                hasCustomBg = true
                bgVersion++
                showHint("背景已更新")
            } else {
                showHint("背景保存失败，换一张图试试")
            }
        }
    }

    /** 清除自定义背景，回到内置默认背景 */
    fun clearCustomBackground() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { BackgroundStore.delete(getApplication()) }
            hasCustomBg = false
            bgVersion++
            showHint("已恢复默认背景")
        }
    }

    fun setFont(mode: AppFont) {
        fontMode = mode
        viewModelScope.launch { dataStore.setFontFamilyName(mode.name) }
    }

    fun applyFontPalette(palette: FontPalette) {
        fontPalette = palette
        viewModelScope.launch { dataStore.setFontColor(palette.argb) }
    }

    fun showHint(text: String) {
        hintMessage = text
        viewModelScope.launch {
            delay(3200)
            if (hintMessage == text) hintMessage = null
        }
    }

    // ---- 成绩导入 / 导出 ----

    /** 生成全量成绩 JSON（耗时短，主线程可调用） */
    suspend fun buildBackupJson(): String {
        val allSessions = sessionDao.getAll()
        val allSolves = solveDao.getAll()
        return ScoreBackup.buildJson(allSessions, allSolves)
    }

    /** 导入 JSON：列表重建、成绩按列表重映射 */
    fun importFromJson(json: String) {
        viewModelScope.launch {
            val parsed = ScoreBackup.parse(json)
            if (parsed == null) {
                showHint("导入失败：文件格式不对")
                return@launch
            }
            var count = 0
            if (sessionDao.count() <= 1 && solves.isEmpty()) {
                // 当前是空库（只有一个默认列表且无成绩）：直接清空默认列表再按文件重建
                currentSessionId?.let { sessionDao.delete(it) }
                sessionDao.getAll().forEach { sessionDao.delete(it.id) }
            }
            val idMap = mutableMapOf<Long, Long>()
            parsed.sessions.forEachIndexed { idx, s ->
                val newId = sessionDao.insert(
                    SessionEntity(
                        name = s.name.ifBlank { "导入列表" },
                        createdAt = if (s.createdAt > 0) s.createdAt else System.currentTimeMillis(),
                        sortOrder = if (s.sortOrder > 0) s.sortOrder else idx
                    )
                )
                idMap[s.id] = newId
            }
            parsed.solves.forEach { r ->
                val sid = idMap[r.sessionId]
                if (sid != null && r.timeMs > 0) {
                    solveDao.insert(
                        SolveRecord(
                            sessionId = sid,
                            timeMs = r.timeMs,
                            timestamp = if (r.timestamp > 0) r.timestamp else System.currentTimeMillis()
                        )
                    )
                    count++
                }
            }
            sessions = sessionDao.getAll()
            val target = sessions.firstOrNull { it.id == currentSessionId } ?: sessions.firstOrNull()
            currentSessionId = target?.id
            currentSessionId?.let { dataStore.setCurrentSession(it) }
            reloadSolves()
            showHint("导入完成：${parsed.sessions.size} 个列表 / $count 条成绩")
        }
    }

    /** 自动备份到 Download/CTimer2（换包名后可在文件管理器找回，再手动导入） */
    fun autoBackup() {
        viewModelScope.launch {
            try {
                val json = buildBackupJson()
                ScoreBackup.autoBackup(getApplication(), json)
            } catch (_: Throwable) {
            }
        }
    }

    init {
        viewModelScope.launch {
            // 首次启动（库里没有任何列表）：先看公共下载目录有没有历史备份
            if (sessionDao.count() == 0) {
                val backup = ScoreBackup.readAutoBackup(getApplication())
                val parsed = backup?.let { ScoreBackup.parse(it) }
                if (parsed != null) {
                    val idMap = mutableMapOf<Long, Long>()
                    parsed.sessions.forEachIndexed { idx, s ->
                        val newId = sessionDao.insert(
                            SessionEntity(
                                name = s.name.ifBlank { "导入列表" },
                                createdAt = if (s.createdAt > 0) s.createdAt else System.currentTimeMillis(),
                                sortOrder = if (s.sortOrder > 0) s.sortOrder else idx
                            )
                        )
                        idMap[s.id] = newId
                    }
                    parsed.solves.forEach { r ->
                        val sid = idMap[r.sessionId]
                        if (sid != null && r.timeMs > 0) {
                            solveDao.insert(
                                SolveRecord(
                                    sessionId = sid,
                                    timeMs = r.timeMs,
                                    timestamp = if (r.timestamp > 0) r.timestamp else System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }

            ensureDefaultSession()
            sessions = sessionDao.getAll()
            // 恢复上次选中的列表
            val saved = dataStore.currentSessionId.first()
            val target = sessions.firstOrNull { it.id == saved } ?: sessions.firstOrNull()
            currentSessionId = target?.id
            currentSessionId?.let { dataStore.setCurrentSession(it) }
            reloadSolves()

            // 恢复卡片拖拽位置
            logoOffset = dataStore.cardOffset("logo").first()
            listOffset = dataStore.cardOffset("list").first()

            // 恢复外观设置
            hasCustomBg = withContext(Dispatchers.IO) {
                BackgroundStore.hasCustom(getApplication())
            }
            val savedFont = dataStore.fontFamilyName().first()
            fontMode = AppFont.entries.firstOrNull { it.name == savedFont } ?: AppFont.MONO
            val savedColor = dataStore.fontColor().first()
            fontPalette = FontPalette.entries.firstOrNull { it.argb == savedColor } ?: FontPalette.INK

            // 恢复材质设置
            val gm = dataStore.glassMode().first()
            glassMode = GlassMode.entries.firstOrNull { it.name == gm } ?: GlassMode.MICA
            glassBlur = dataStore.glassBlur().first() ?: 18f
            bgBlur = dataStore.bgBlur().first() ?: 0f

            // 恢复用户名（登录态）
            userName = dataStore.userName().first()?.takeIf { it.isNotBlank() }

            // 每次启动都从云端拉一次，补齐本机没有的成绩
            syncFromCloud(silent = true)
        }
    }

    /** 保存卡片拖拽位置（拖动结束时调用） */
    fun saveCardOffset(card: String, x: Float, y: Float) {
        if (card == "logo") logoOffset = x to y else listOffset = x to y
        viewModelScope.launch { dataStore.setCardOffset(card, x, y) }
    }

    /** 卡片位置复位 */
    fun resetCardOffsets() {
        viewModelScope.launch {
            dataStore.setCardOffset("logo", 0f, 0f)
            dataStore.setCardOffset("list", 0f, 0f)
            logoOffset = 0f to 0f
            listOffset = 0f to 0f
        }
    }

    private suspend fun ensureDefaultSession() {
        if (sessionDao.count() == 0) {
            sessionDao.insert(
                SessionEntity(
                    name = "默认列表",
                    createdAt = System.currentTimeMillis(),
                    sortOrder = 0
                )
            )
        }
    }

    private suspend fun reloadSolves() {
        val sid = currentSessionId ?: return
        solves = solveDao.getBySession(sid)
    }

    // ---- 抽屉控制 ----

    fun openDrawer() {
        if (timerState == TimerState.RUNNING) return
        drawerOpen = true
    }

    fun closeDrawer() {
        drawerOpen = false
    }

    // ---- 统一输入：触摸 / 鼠标 / 空格 全部走这两个入口 ----

    /** 按下（触摸 down、鼠标左键 down、空格 ACTION_DOWN） */
    fun onPressStart() {
        if (drawerOpen) return
        if (timerState == TimerState.RUNNING) {
            stopTimer()
            return
        }
        timerState = TimerState.HOLDING
        holdJob?.cancel()
        holdJob = viewModelScope.launch {
            delay(400)
            if (timerState == TimerState.HOLDING) timerState = TimerState.READY
        }
    }

    /** 松开（触摸 up、鼠标 up、空格 ACTION_UP） */
    fun onPressEnd() {
        if (drawerOpen) return
        holdJob?.cancel()
        holdJob = null
        when (timerState) {
            TimerState.RUNNING -> stopTimer()
            TimerState.READY -> startTimer()
            TimerState.HOLDING -> timerState = TimerState.IDLE
            else -> {}
        }
    }

    /** 手势被系统打断 */
    fun onCancel() {
        holdJob?.cancel()
        holdJob = null
        if (timerState == TimerState.HOLDING || timerState == TimerState.READY) {
            timerState = TimerState.IDLE
        }
    }

    private fun startTimer() {
        startNanos = SystemClock.elapsedRealtimeNanos()
        lastFinishedMs = null
        flashColor = null
        timerState = TimerState.RUNNING
        // RUNNING 时自动关闭抽屉
        drawerOpen = false
        actionMenuOpen = false
        browseIndex = null
    }

    fun stopTimer() {
        if (timerState != TimerState.RUNNING) return
        val endNanos = SystemClock.elapsedRealtimeNanos()
        val ms = (endNanos - startNanos) / 1_000_000
        timerState = TimerState.IDLE
        lastFinishedMs = ms
        // 本次复原结束，换一条新打乱
        refreshScramble()

        val prevBest = solves.firstOrNull()?.timeMs
        flashColor = when {
            prevBest == null -> DragonCatBlue.ResultFirst
            ms < prevBest -> DragonCatBlue.ResultBetter
            ms > prevBest -> DragonCatBlue.ResultWorse
            else -> DragonCatBlue.ResultFirst
        }

        viewModelScope.launch {
            val sid = currentSessionId ?: return@launch

            // 用内存中的当前列表成绩做判定（与列表同源，不依赖 DataStore 读取时序）
            val bestBefore = solves.minOfOrNull { it.timeMs }
            val isFirstSolve = solves.isEmpty()

            val now = System.currentTimeMillis()
            solveDao.insert(SolveRecord(sessionId = sid, timeMs = ms, timestamp = now))
            reloadSolves()
            autoBackup()

            // 本地每记录一条成绩，立即推到云端，并把进度显示在顶部提示条上
            val sessionName = sessions.firstOrNull { it.id == sid }?.name ?: "默认列表"
            val pretty = Stats.format(ms)
            launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    cloudUploadHint = "成绩 $pretty 正在上传至 GitHub 仓库…"
                }
                val err = GitHubRepository.uploadScoreWithRetry(sessionName, ms, now, userName)
                withContext(Dispatchers.Main) {
                    cloudUploadHint = if (err == null) {
                        "成绩 $pretty 已上传至 GitHub 仓库"
                    } else {
                        "成绩 $pretty 上传失败：$err"
                    }
                    delay(if (err == null) 2000 else 4500)
                    cloudUploadHint = null
                }
            }

            var title: String? = null
            var label: String? = null

            if (isFirstSolve) {
                title = "首个成绩"
                label = "本列表第一条记录"
                dataStore.setBestSolve(sid, ms)
            } else if (bestBefore != null && ms < bestBefore) {
                title = "列表最快"
                label = "比之前最好快 ${Stats.format(bestBefore - ms)}"
                dataStore.setBestSolve(sid, ms)
            }

            // ao5 最佳需与历史比对，读持久化值
            val oldBestAo5 = dataStore.bestAo5(sid).first()
            val newAo5 = Stats.ao5(solves.map { it.timeMs })
            if (newAo5 != null) {
                if (oldBestAo5 == null || newAo5 < oldBestAo5) {
                    dataStore.setBestAo5(sid, newAo5)
                    if (title == null) {
                        title = "ao5 最快"
                        label = "本列表 ao5 新纪录 ${String.format("%.2f", newAo5)}s"
                    }
                }
            }

            if (title != null) showAchievement(title, label ?: "")
        }

        viewModelScope.launch {
            delay(1500)
            flashColor = null
        }
    }

    // ---- 列表管理 ----

    fun selectSession(id: Long) {
        viewModeState = ListViewMode.BY_TIME
        browseIndex = null
        if (id == currentSessionId) {
            closeDrawer()
            return
        }
        viewModelScope.launch {
            currentSessionId = id
            achievement = null
            achievementLabel = null
            dataStore.setCurrentSession(id)
            reloadSolves()
            closeDrawer()
        }
    }

    fun createSession(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val order = sessions.size
            val newId = sessionDao.insert(
                SessionEntity(
                    name = trimmed,
                    createdAt = System.currentTimeMillis(),
                    sortOrder = order
                )
            )
            sessions = sessionDao.getAll()
            currentSessionId = newId
            dataStore.setCurrentSession(newId)
            reloadSolves()
        }
    }

    fun renameSession(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            sessionDao.rename(id, trimmed)
            sessions = sessionDao.getAll()
        }
    }

    /** 删除列表；至少保留一个。返回是否成功。 */
    fun deleteSession(id: Long) {
        viewModelScope.launch {
            if (sessionDao.count() <= 1) return@launch
            sessionDao.delete(id) // 外键级联删除成绩
            sessions = sessionDao.getAll()
            if (currentSessionId == id) {
                val fallback = sessions.firstOrNull()
                currentSessionId = fallback?.id
                fallback?.let { dataStore.setCurrentSession(it.id) }
            }
            reloadSolves()
        }
    }

    // ---- 视图切换 ----

    fun setViewMode(mode: ListViewMode) {
        viewModeState = mode
        browseIndex = null
        closeDrawer()
    }

    // ---- 成绩浏览 / 删除 ----

    /** 进入浏览态，指向当前 solves 下标 */
    fun beginBrowse(index: Int = 0) {
        if (solves.isEmpty()) return
        browseIndex = index.coerceIn(0, solves.size - 1)
    }

    fun exitBrowse() {
        browseIndex = null
    }

    /** 上一条（更早的成绩） */
    fun browsePrev() {
        val cur = browseIndex ?: return
        if (cur < solves.size - 1) browseIndex = cur + 1
    }

    /** 下一条（更新的成绩） */
    fun browseNext() {
        val cur = browseIndex ?: return
        if (cur > 0) browseIndex = cur - 1
    }

    /** 当前浏览的成绩（未浏览时为最新一条） */
    fun browsedSolve(): SolveRecord? {
        val idx = browseIndex ?: return solves.firstOrNull()
        return solves.getOrNull(idx)
    }

    /** 删除当前成绩：浏览态删浏览那条，否则删最新一条 */
    fun deleteCurrentSolve() {
        val target = browsedSolve() ?: return
        val wasBrowsing = browseIndex != null
        viewModelScope.launch {
            solveDao.deleteById(target.id)
            reloadSolves()
            // 删除后调整浏览位置，避免越界
            if (wasBrowsing) {
                browseIndex = when {
                    solves.isEmpty() -> null
                    (browseIndex ?: 0) >= solves.size -> solves.size - 1
                    else -> browseIndex
                }
            }
            lastFinishedMs = solves.firstOrNull()?.timeMs
            flashColor = null
            if (solves.isEmpty()) actionMenuOpen = false
        }
    }

    // ---- 派生数据 ----

    fun currentElapsedMs(): Long =
        if (timerState == TimerState.RUNNING)
            (SystemClock.elapsedRealtimeNanos() - startNanos) / 1_000_000
        else lastFinishedMs ?: 0L

    val ao5: Double? get() = Stats.ao5(solves.map { it.timeMs })
    val avg: Double? get() = Stats.avg(solves.map { it.timeMs })
    val lastMs: Long? get() = solves.firstOrNull()?.timeMs

    fun solveCount(sessionId: Long): Int = solves.count { it.sessionId == sessionId }

    /**
     * 按当前视图模式生成成绩行。
     * BY_TIME  ：全部按时间倒序（最新的在最上面，第一次在最下面），序号从 1 开始
     * BY_SCORE ：全部按成绩快→慢排列
     * 差值均以「显示顺序上的上一条」为基准，负=更快。
     */
    fun resultRows(): List<ResultRow> {
        return when (viewModeState) {
            ListViewMode.BY_TIME -> {
                // solves 本身即 timestamp DESC：最新在上、第一次在底
                solves.mapIndexed { i, r ->
                    val prev = solves.getOrNull(i + 1) // 更早一条
                    ResultRow(
                        index = solves.size - i,
                        timeMs = r.timeMs,
                        color = colorAgainst(r.timeMs, prev?.timeMs),
                        deltaMs = prev?.let { r.timeMs - it.timeMs }
                    )
                }
            }

            ListViewMode.BY_SCORE -> {
                val sorted = solves.sortedBy { it.timeMs } // 快的在上
                sorted.mapIndexed { i, r ->
                    val prev = sorted.getOrNull(i - 1) // 更快一条
                    ResultRow(
                        index = i + 1,
                        timeMs = r.timeMs,
                        color = if (prev == null) DragonCatBlue.ResultFirst
                        else colorAgainst(r.timeMs, prev.timeMs),
                        deltaMs = prev?.let { r.timeMs - it.timeMs }
                    )
                }
            }
        }
    }

    private fun colorAgainst(current: Long, baseline: Long?): Color = when {
        baseline == null -> DragonCatBlue.ResultFirst
        current < baseline -> DragonCatBlue.ResultBetter
        current > baseline -> DragonCatBlue.ResultWorse
        else -> DragonCatBlue.ResultFirst
    }

    // ---- 成绩总评 ----

    fun summary(): Summary = Stats.summary(solves.map { it.timeMs })
}
