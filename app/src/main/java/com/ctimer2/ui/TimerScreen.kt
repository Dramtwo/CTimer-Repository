package com.ctimer2.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.ctimer2.R
import com.ctimer2.ui.components.*
import com.ctimer2.ui.theme.DragonCatBlue
import com.ctimer2.ui.theme.LocalAppFontColor
import com.ctimer2.ui.theme.LocalAppFontFamily
import com.ctimer2.ui.theme.LocalGlass
import com.ctimer2.ui.theme.LocalGlassBackdrop
import com.ctimer2.ui.theme.buildGlassBackdrop
import com.ctimer2.ui.theme.GlassBackdrop
import com.ctimer2.ui.theme.LocalBackgroundSpec
import com.ctimer2.ui.theme.BackgroundSpec
import com.ctimer2.util.BackgroundStore
import com.ctimer2.util.InputDevices
import com.ctimer2.util.Scramble
import com.ctimer2.util.ScoreBackup
import com.ctimer2.util.Stats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 内容区的页面：正常计时界面 / 成绩总评 / 公式速记 / 设置 */
private enum class AppPage { NORMAL, SUMMARY, FORMULA, SETTINGS, LEADERBOARD, USER }

/** 背景图 + 它对应的版本号；版本与当前 [TimerViewModel.bgVersion] 不符说明这张图已经过期 */
private data class ReadyBackground(val version: Int, val bitmap: androidx.compose.ui.graphics.ImageBitmap?)

@Composable
fun TimerScreen(vm: TimerViewModel = viewModel()) {
    val state = vm.timerState
    val isRunning = state == TimerState.RUNNING
    val drawerOpen = vm.drawerOpen
    val menuOpen = vm.actionMenuOpen
    val summaryOpen = vm.summaryPageOpen
    val formulaOpen = vm.formulaPageOpen
    val settingsOpen = vm.settingsPageOpen
    val leaderboardOpen = vm.leaderboardPageOpen
    val userOpen = vm.userPageOpen

    var displayText by remember { mutableStateOf("0.00") }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            withFrameNanos {
                displayText = Stats.format(vm.currentElapsedMs())
            }
        }
    }
    LaunchedEffect(state, vm.lastFinishedMs) {
        if (!isRunning) {
            displayText = vm.lastFinishedMs?.let { Stats.format(it) } ?: "0.00"
        }
    }

    val shownText = if (menuOpen) {
        vm.browsedSolve()?.let { Stats.format(it.timeMs) } ?: displayText
    } else displayText

    val timerColor = when (state) {
        TimerState.RUNNING -> vm.fontPalette.color
        TimerState.HOLDING -> DragonCatBlue.HoldColor
        TimerState.READY -> DragonCatBlue.ReadyColor
        TimerState.IDLE -> vm.flashColor ?: vm.fontPalette.color
    }

    val page = when {
        userOpen -> AppPage.USER
        settingsOpen -> AppPage.SETTINGS
        leaderboardOpen -> AppPage.LEADERBOARD
        formulaOpen -> AppPage.FORMULA
        summaryOpen -> AppPage.SUMMARY
        else -> AppPage.NORMAL
    }

    // 键鼠连接状态：每 3 秒探测一次
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputHint by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            inputHint = InputDevices.hint(context)
            delay(3000)
        }
    }

    // 背景图只解码一次，供底图与各玻璃面板共用
    val appCtx = LocalContext.current

    // ---- 背景就绪状态 ----
    // 关键修复：背景解码与玻璃烘焙原来是两条独立的异步链，烘焙会抢在解码完成前
    // 读到旧图（或 null → 默认图），之后就再也不重算 ——「换成自定义背景，玻璃里还是默认背景」。
    // 现在用「版本号 + 解码结果」打包成一个状态，只有版本对得上才算就绪。
    var bgReady by remember { mutableStateOf<ReadyBackground?>(null) }
    LaunchedEffect(vm.hasCustomBg, vm.bgVersion) {
        val targetVersion = vm.bgVersion
        val bmp = withContext(Dispatchers.IO) {
            if (vm.hasCustomBg) {
                runCatching { BackgroundStore.load(appCtx)?.asImageBitmap() }.getOrNull()
            } else null
        }
        bgReady = ReadyBackground(targetVersion, bmp)
    }
    val readyBg = bgReady?.takeIf { it.version == vm.bgVersion }
    val isBgReady = readyBg != null
    val customBitmap: ImageBitmap? = readyBg?.bitmap

    // 预烘焙的玻璃底图：模糊只算一次，面板只做平移裁切（滚动不掉帧）
    val density = LocalDensity.current
    val densityScale = density.density
    val cfg = LocalConfiguration.current
    var rootSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val screenWpx = if (rootSize.width > 0) rootSize.width
        else with(density) { cfg.screenWidthDp.dp.roundToPx() }
    val screenHpx = if (rootSize.height > 0) rootSize.height
        else with(density) { cfg.screenHeightDp.dp.roundToPx() }
    val glassBackdrop by produceState<GlassBackdrop?>(
        initialValue = null,
        isBgReady,
        customBitmap,
        vm.glassMode,
        vm.glassBlur,
        vm.bgBlur,
        screenWpx,
        screenHpx
    ) {
        // 背景还没就绪：保持上一份结果，宁可暂时用旧图，也不烘焙出「默认背景」的错图
        if (!isBgReady) return@produceState
        value = buildGlassBackdrop(
            context = appCtx,
            custom = customBitmap,
            blurDp = if (vm.glassMode == com.ctimer2.ui.theme.GlassMode.SOLID) 0f else vm.glassBlur,
            bgBlurDp = if (vm.glassMode == com.ctimer2.ui.theme.GlassMode.SOLID) 0f else vm.bgBlur,
            screenWpx = screenWpx,
            screenHpx = screenHpx,
            density = densityScale,
            version = vm.bgVersion
        )
    }

    BackHandler(enabled = drawerOpen || menuOpen || summaryOpen || formulaOpen || settingsOpen || leaderboardOpen || userOpen) {
        when {
            userOpen -> vm.closeUserPage()
            settingsOpen -> vm.closeSettingsPage()
            leaderboardOpen -> vm.closeLeaderboardPage()
            formulaOpen -> vm.closeFormulaPage()
            summaryOpen -> vm.closeSummaryPage()
            menuOpen -> vm.closeActionMenu()
            else -> vm.closeDrawer()
        }
    }

    // ---- 系统相册选图（Android 13+ Photo Picker，无需权限）----
    val pickBackground = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Throwable) {
            }
            vm.saveCustomBackground(uri)
        }
    }

    // ---- 导出成绩（SAF 保存到用户选定位置）----
    val exportScores = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val json = vm.buildBackupJson()
                val ok = ScoreBackup.writeToUri(context, uri, json)
                vm.showHint(if (ok) "成绩已导出" else "导出失败")
            }
        }
    }

    // ---- 导入成绩（SAF 选择文件）----
    val importScores = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val json = ScoreBackup.readFromUri(context, uri)
            if (json == null) vm.showHint("读取失败") else vm.importFromJson(json)
        }
    }

    val launchImport = { importScores.launch(arrayOf("application/json", "text/plain", "*/*")) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)

                    // 只要不在主计时页（总评 / 公式 / 云端排行 / 设置 / 用户中心任一页开着），
                    // 或者抽屉 / 成绩悬浮菜单开着，就不响应整屏计时手势，避免在别的页面点按钮时误计时。
                    // 注意：这里必须直接读 vm 的状态（组合期局部变量在 pointerInput 闭包里会停留在旧值）。
                    val onOtherPage = vm.summaryPageOpen || vm.formulaPageOpen ||
                        vm.leaderboardPageOpen || vm.settingsPageOpen || vm.userPageOpen
                    if (onOtherPage || vm.drawerOpen || vm.actionMenuOpen) {
                        down.consume()
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }

                    val wasRunning = vm.timerState == TimerState.RUNNING
                    vm.onPressStart()

                    if (wasRunning) {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                            if (!ch.pressed) break
                        }
                        vm.onPressEnd()
                        return@awaitEachGesture
                    }

                    var total = Offset.Zero
                    var dragged = false
                    val threshold = 30.dp.toPx()
                    // 轮盘几何：外径与中心死区，和 RadialWheelMenu 保持同一套参数
                    val wheelDiameterPx = with(density) { 268.dp.toPx() }
                    val deadZonePx = wheelDiameterPx * 0.30f + 8.dp.toPx()
                    val slotSweep = 72.0
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        total += ch.positionChange()
                        if (!dragged && total.getDistance() > threshold) {
                            dragged = true
                            vm.onCancel()
                            // 在手指按下的位置弹出轮盘（CS-Timer 的手感）
                            vm.openWheel(down.position.x, down.position.y)
                        }
                        if (dragged) {
                            val dx = ch.position.x - vm.wheelCenterX
                            val dy = ch.position.y - vm.wheelCenterY
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                            if (dist < deadZonePx) {
                                vm.updateWheelSlot(WheelSlot.CANCEL)
                            } else {
                                val deg = Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble())
                                val rel = ((deg + 90.0 + slotSweep / 2.0) % 360.0 + 360.0) % 360.0
                                vm.updateWheelSlot((rel / slotSweep).toInt().coerceIn(0, 4))
                            }
                            ch.consume()
                        }
                    }
                    if (dragged) vm.releaseWheel() else vm.onPressEnd()
                }
            }
    ) {
        CompositionLocalProvider(
            LocalAppFontFamily provides vm.fontMode.fontFamily,
            LocalAppFontColor provides vm.fontPalette.color,
            LocalGlass provides vm.glassStyle,
            LocalBackgroundSpec provides BackgroundSpec(customBitmap, vm.bgVersion),
            LocalGlassBackdrop provides glassBackdrop
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { if (it.size.width > 0) rootSize = it.size }
        ) {
        // 底层：自定义背景图或内置默认背景图（默认保持清晰）
        AppBackground(customBitmap, vm.bgBlur)

        // 云端上传提示（顶部浮出的小玻璃条，2 秒后自动消失）
        vm.cloudUploadHint?.let { hint ->
            FrostedGlass(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(3f)
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(14.dp),
                tintAlphaShift = 0.10f
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(22.dp)) { IconCloud() }
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = hint,
                        fontSize = 13.sp,
                        color = DragonCatBlue.TextPrimary
                    )
                }
            }
        }

        // 内容区整块切换页面：新页面从侧栏这一侧「释放」滑入
        val sidePad by animateDpAsState(
            targetValue = if (isRunning) 0.dp else 56.dp,
            animationSpec = tween(220),
            label = "sidePad"
        )
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                (slideInHorizontally(animationSpec = tween(300)) { -it / 2 } + fadeIn(tween(180)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(220)) { it / 4 } + fadeOut(tween(100)))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(start = sidePad),
            label = "page"
        ) { p ->
                when (p) {
                    AppPage.NORMAL -> NormalPage(
                        vm = vm,
                        isRunning = isRunning,
                        shownText = shownText,
                        timerColor = timerColor,
                        menuOpen = menuOpen,
                        inputHint = inputHint
                    )

                    AppPage.SUMMARY -> {
                        val currentName = vm.sessions
                            .firstOrNull { it.id == vm.currentSessionId }?.name ?: "默认列表"
                        SummaryPage(
                            summary = vm.summary(),
                            sessionName = currentName,
                            onBack = { vm.closeSummaryPage() }
                        )
                    }

                    AppPage.FORMULA -> FormulaPage(onBack = { vm.closeFormulaPage() })

                    AppPage.LEADERBOARD -> LeaderboardScreen(
                        defaultSession = vm.currentSessionName,
                        userName = vm.userName,
                        onBack = { vm.closeLeaderboardPage() }
                    )

                    AppPage.USER -> UserScreen(
                        userName = vm.userName,
                        syncing = vm.cloudSyncing,
                        cloudStatus = vm.cloudStatus,
                        onLogin = { vm.applyUserName(it) },
                        onLogout = { vm.logout() },
                        onBack = { vm.closeUserPage() }
                    )

                    AppPage.SETTINGS -> SettingsPage(
                        useCustomBackground = vm.hasCustomBg,
                        font = vm.fontMode,
                        palette = vm.fontPalette,
                        glassMode = vm.glassMode,
                        glassBlur = vm.glassBlur,
                        bgBlur = vm.bgBlur,
                        hintMessage = vm.hintMessage,
                        onPickBackground = {
                            pickBackground.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onClearBackground = {
                            vm.clearCustomBackground()
                            vm.showHint("已恢复默认背景")
                        },
                        onSelectFont = { vm.setFont(it) },
                        onSelectPalette = { vm.applyFontPalette(it) },
                        onSelectGlassMode = { vm.applyGlassMode(it) },
                        onGlassBlurChange = { vm.applyGlassBlur(it) },
                        onBgBlurChange = { vm.applyBgBlur(it) },
                        onExport = {
                            scope.launch {
                                exportScores.launch("ctimer2_scores.json")
                            }
                        },
                        onImport = { launchImport() },
                        onBack = { vm.closeSettingsPage() }
                    )
                }
        }

        // 侧栏常驻最上层：抽屉从它背后被「拉开」，而不是盖在它身上
        AnimatedVisibility(
            visible = !isRunning,
            enter = fadeIn() + expandHorizontally(animationSpec = tween(220)),
            exit = fadeOut() + shrinkHorizontally(animationSpec = tween(220)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .zIndex(2f)
        ) {
            DrawerSideBar(
                onOpen = { vm.openDrawer() },
                onOpenSummary = { vm.toggleSummaryPage() },
                onOpenFormula = { vm.toggleFormulaPage() },
                onOpenLeaderboard = { vm.toggleLeaderboardPage() },
                onResetCards = { vm.resetCardOffsets() },
                onImport = { launchImport() },
                onOpenSettings = { vm.toggleSettingsPage() },
                onOpenUser = { vm.toggleUserPage() }
            )
        }

        // 轮盘菜单（在手指按下的位置弹出，松手执行高亮项，拖回中心则取消）
        AnimatedVisibility(
            visible = vm.wheelOpen && !isRunning,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(120)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(4f)
        ) {
            RadialWheelMenu(
                centerX = vm.wheelCenterX,
                centerY = vm.wheelCenterY,
                selected = vm.wheelSelected,
                items = DefaultWheelItems
            )
        }

        // 抽屉遮罩（只压暗内容区，侧栏本身在最上层不受影响）
        AnimatedVisibility(
            visible = drawerOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DragonCatBlue.Scrim)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            vm.closeDrawer()
                        }
                    }
            )
        }

        // 抽屉面板：与侧栏联动。上层侧栏不动，抽屉从它背后滑出 = 侧栏被拉开的效果
        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInHorizontally(animationSpec = tween(280)) { -it },
            exit = slideOutHorizontally(animationSpec = tween(240)) { -it },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 56.dp)
                .zIndex(1f)
        ) {
            DrawerContent(
                sessions = vm.sessions,
                currentSessionId = vm.currentSessionId,
                viewMode = vm.viewModeState,
                countOf = { sid -> vm.solves.count { it.sessionId == sid } },
                onSelect = { vm.selectSession(it) },
                onSelectView = { vm.setViewMode(it) },
                onOpenSummary = { vm.toggleSummaryPage() },
                onOpenFormula = { vm.toggleFormulaPage() },
                onOpenLeaderboard = { vm.toggleLeaderboardPage() },
                onCreate = { vm.createSession(it) },
                onRename = { id, name -> vm.renameSession(id, name) },
                onDelete = { vm.deleteSession(it) }
            )
        }
        
        }}
    }
}

/** 背景：自定义图（私有目录）优先，否则用内置默认背景图。默认保持清晰，只在设置里调高时才模糊 */
@Composable
private fun AppBackground(customBitmap: ImageBitmap?, blurDp: Float) {
    Box(modifier = Modifier.fillMaxSize().background(DragonCatBlue.BackgroundDeep)) {
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurDp > 0f) Modifier.backgroundBlur(blurDp) else Modifier)
            )
        } else {
            Image(
                painter = painterResource(R.drawable.default_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurDp > 0f) Modifier.backgroundBlur(blurDp) else Modifier)
            )
        }
        // 一层极淡的奶白雾面，压低背景对比度，让前景文字更清楚
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x14FFFFFF))
        )
    }
}

/** 背景整体模糊（可选，默认 0 即不模糊） */
@Composable
private fun Modifier.backgroundBlur(blurDp: Float): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.blur(radius = blurDp.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    } else {
        this
    }
}

@Composable
private fun NormalPage(
    vm: TimerViewModel,
    isRunning: Boolean,
    shownText: String,
    timerColor: Color,
    menuOpen: Boolean,
    inputHint: String?
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // 卡片区：长按卡片可拖动
        AnimatedVisibility(
            visible = !isRunning,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Column(
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
                    .padding(start = 8.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DraggableCard(
                    cardKey = "logo",
                    initialOffset = vm.logoOffset,
                    onOffsetCommit = { x, y -> vm.saveCardOffset("logo", x, y) }
                ) {
                    LogoCard(
                        modifier = Modifier.fillMaxWidth(),
                        marqueeText = vm.achievement,
                        badgeLabel = vm.achievementLabel,
                        inputHint = inputHint
                    )
                }

                DraggableCard(
                    cardKey = "list",
                    initialOffset = vm.listOffset,
                    onOffsetCommit = { x, y -> vm.saveCardOffset("list", x, y) },
                    modifier = Modifier.weight(1f),
                    fillRemaining = true
                ) {
                    // 切换条与成绩列表合成一张完整卡片（玻璃面板）
                    FrostedGlass(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                        ListModeSwitcher(
                            mode = vm.viewModeState,
                            onChange = { vm.setViewMode(it) },
                            embedded = true
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(DragonCatBlue.Divider)
                        )
                        AnimatedContent(
                            targetState = vm.viewModeState to vm.currentSessionId,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "listContent",
                            modifier = Modifier.weight(1f)
                        ) { _ ->
                            ResultListCard(
                                rows = vm.resultRows(),
                                modifier = Modifier.fillMaxSize(),
                                embedded = true
                            )
                        }
                        }
                    }
                }
            }
        }

        // 主计时区
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 未开始复原前显示打乱公式（中文口诀 + 字母对照）
            AnimatedVisibility(visible = !isRunning && !menuOpen) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = Scramble.toChinese(vm.scramble),
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        color = DragonCatBlue.TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = vm.scramble,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center,
                        color = DragonCatBlue.TextSecondary
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            // 计时数字：扁平显示，不加任何材质底板
            TimerDisplay(
                text = shownText,
                color = timerColor,
                enlarged = isRunning
            )
            Spacer(Modifier.height(18.dp))
            StatsRow(
                ao5 = Stats.formatStat(vm.ao5),
                avg = Stats.formatStat(vm.avg),
                last = vm.lastMs?.let { Stats.format(it) } ?: "--",
                dimmed = isRunning
            )
        }
    }
}

@Composable
private fun StatsRow(ao5: String, avg: String, last: String, dimmed: Boolean) {
    val valueColor = if (dimmed) DragonCatBlue.TextSecondary else DragonCatBlue.TimerText
    Row(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp)
    ) {
        StatItem("ao5", ao5, valueColor)
        StatItem("avg", avg, valueColor)
        StatItem("last", last, valueColor)
    }
}

@Composable
private fun StatItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = DragonCatBlue.TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}
