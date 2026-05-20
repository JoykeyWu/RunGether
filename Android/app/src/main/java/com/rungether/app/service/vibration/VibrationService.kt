package com.rungether.app.service.vibration

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.rungether.app.bluetooth.protocol.DirectionType
import com.rungether.app.bluetooth.protocol.GuideCommand
import com.rungether.app.bluetooth.protocol.ShortcutType
import com.rungether.app.constant.VibrationConstants
import com.rungether.app.data.prefs.UserPreferences

/**
 * 震动反馈服务
 *
 * 按需求文档规则映射震动样式：
 * 直行不震、微调短1、转弯短2、大转弯短3、障碍长1、减速短2、停止持续 3 秒。
 * 受偏好开关控制：开关关闭时所有调用立即静默返回。
 */
class VibrationService private constructor(appContext: Context) {

    private val applicationContext: Context = appContext.applicationContext
    private val preferences = UserPreferences.from(applicationContext)

    private val vibrator: Vibrator? = resolveVibrator(applicationContext)

    // 按指令派发震动模式
    fun feedback(command: GuideCommand) {
        when (command) {
            is GuideCommand.Direction -> feedbackDirection(command.type)
            is GuideCommand.Shortcut -> feedbackShortcut(command.type)
            GuideCommand.Sos -> playPattern(longArrayOf(0, VibrationConstants.LONG_MS, 200, VibrationConstants.LONG_MS))
            is GuideCommand.Status -> Unit
        }
    }

    private fun feedbackDirection(type: DirectionType) {
        when (type) {
            DirectionType.STRAIGHT -> Unit
            DirectionType.MICRO -> shortBursts(count = 1)
            DirectionType.TURN -> shortBursts(count = 2)
            DirectionType.HARD_TURN -> shortBursts(count = 3)
        }
    }

    private fun feedbackShortcut(type: ShortcutType) {
        when (type) {
            ShortcutType.OBSTACLE -> playSingle(VibrationConstants.LONG_MS)
            ShortcutType.SLOW_DOWN -> shortBursts(count = 2)
            ShortcutType.STOP -> playSingle(VibrationConstants.STOP_HOLD_MS)
        }
    }

    private fun shortBursts(count: Int) {
        if (count <= 0) return
        val pattern = mutableListOf<Long>()
        pattern.add(0L)
        repeat(count) { index ->
            pattern.add(VibrationConstants.SHORT_MS)
            if (index != count - 1) pattern.add(VibrationConstants.SHORT_GAP_MS)
        }
        playPattern(pattern.toLongArray())
    }

    private fun playSingle(durationMs: Long) {
        if (!enabled() || durationMs <= 0) return
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(durationMs)
        }
    }

    private fun playPattern(pattern: LongArray) {
        if (!enabled()) return
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(pattern, -1)
        }
    }

    private fun enabled(): Boolean = preferences.vibrationEnabled

    private fun resolveVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    companion object {
        @Volatile
        private var instance: VibrationService? = null

        // 获取震动服务单例
        fun from(context: Context): VibrationService {
            return instance ?: synchronized(this) {
                instance ?: VibrationService(context).also { instance = it }
            }
        }
    }
}
