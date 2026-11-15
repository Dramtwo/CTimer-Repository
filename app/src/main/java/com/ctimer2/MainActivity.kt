package com.ctimer2

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ctimer2.ui.TimerScreen
import com.ctimer2.ui.TimerViewModel
import com.ctimer2.ui.theme.CTimer2Theme

class MainActivity : ComponentActivity() {

    private lateinit var vm: TimerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        setContent {
            vm = viewModel()
            CTimer2Theme {
                TimerScreen(vm)
            }
        }
    }

    /** 空格键统一计时：不依赖 Compose 焦点，避免焦点丢失导致失效 */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_SPACE) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) vm.onPressStart()
                }
                KeyEvent.ACTION_UP -> vm.onPressEnd()
            }
            return true // 消费事件，防止触发按钮点击等副作用
        }
        return super.dispatchKeyEvent(event)
    }
}
