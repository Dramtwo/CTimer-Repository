package com.ctimer2.util

import android.content.Context
import android.content.res.Configuration
import android.view.InputDevice

/**
 * 检测当前是否接入了物理键盘 / 鼠标。
 * 键盘用 Configuration 判断；鼠标、外接键盘再遍历 InputManager 的输入设备。
 */
object InputDevices {

    fun keyboardMouseState(context: Context): Pair<Boolean, Boolean> {
        val config = context.resources.configuration
        val hasKeyboard = config.keyboard != Configuration.KEYBOARD_NOKEYS ||
                config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO

        var mouse = false
        var keyboard = hasKeyboard
        try {
            val ids = InputDevice.getDeviceIds()
            ids.forEach { id ->
                val device = InputDevice.getDevice(id) ?: return@forEach
                val sources = device.sources
                if (sources and InputDevice.SOURCE_MOUSE != 0) mouse = true
                if (sources and InputDevice.SOURCE_KEYBOARD != 0 &&
                    device.keyboardType != InputDevice.KEYBOARD_TYPE_NONE
                ) keyboard = true
            }
        } catch (_: Throwable) {
        }
        return keyboard to mouse
    }

    /** 提示文案；都没连则返回 null */
    fun hint(context: Context): String? {
        val (kb, mouse) = keyboardMouseState(context)
        return when {
            kb && mouse -> "键鼠已连接"
            kb -> "键盘已连接"
            mouse -> "鼠标已连接"
            else -> null
        }
    }
}
