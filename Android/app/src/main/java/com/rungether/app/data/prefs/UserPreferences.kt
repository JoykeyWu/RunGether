package com.rungether.app.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.rungether.app.constant.PrefKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 用户偏好存储
 *
 * 单进程单实例封装 SharedPreferences，对外暴露同步读写与 StateFlow 观察。
 * 跨页面读写同一份配置时确保数据一致性；进程重启后保留所有选择。
 */
class UserPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PrefKeys.PREF_FILE, Context.MODE_PRIVATE)

    private val roleFlow = MutableStateFlow(loadRole())
    private val ttsSpeedFlow = MutableStateFlow(loadTtsSpeed())
    private val vibrationEnabledFlow = MutableStateFlow(loadVibrationEnabled())

    // 当前角色（同步读取）
    var currentRole: UserRole
        get() = roleFlow.value
        set(value) {
            prefs.edit().putString(PrefKeys.KEY_ROLE, value.name).apply()
            roleFlow.value = value
        }

    // 当前 TTS 语速档位（同步读取）
    var ttsSpeed: TtsSpeed
        get() = ttsSpeedFlow.value
        set(value) {
            prefs.edit().putString(PrefKeys.KEY_TTS_SPEED, value.name).apply()
            ttsSpeedFlow.value = value
        }

    // 震动开关（同步读取）
    var vibrationEnabled: Boolean
        get() = vibrationEnabledFlow.value
        set(value) {
            prefs.edit().putBoolean(PrefKeys.KEY_VIBRATION_ENABLED, value).apply()
            vibrationEnabledFlow.value = value
        }

    // 最近成功配对的蓝牙 MAC 地址
    var lastPairedMac: String?
        get() = prefs.getString(PrefKeys.KEY_LAST_PAIRED_MAC, null)
        set(value) {
            prefs.edit().putString(PrefKeys.KEY_LAST_PAIRED_MAC, value).apply()
        }

    // 当前角色的可观察 StateFlow
    fun observeRole(): StateFlow<UserRole> = roleFlow.asStateFlow()

    // TTS 语速的可观察 StateFlow
    fun observeTtsSpeed(): StateFlow<TtsSpeed> = ttsSpeedFlow.asStateFlow()

    // 震动开关的可观察 StateFlow
    fun observeVibrationEnabled(): StateFlow<Boolean> = vibrationEnabledFlow.asStateFlow()

    private fun loadRole(): UserRole {
        val raw = prefs.getString(PrefKeys.KEY_ROLE, null) ?: return UserRole.UNSELECTED
        return runCatching { UserRole.valueOf(raw) }.getOrDefault(UserRole.UNSELECTED)
    }

    private fun loadTtsSpeed(): TtsSpeed {
        val raw = prefs.getString(PrefKeys.KEY_TTS_SPEED, null) ?: return TtsSpeed.NORMAL
        return runCatching { TtsSpeed.valueOf(raw) }.getOrDefault(TtsSpeed.NORMAL)
    }

    private fun loadVibrationEnabled(): Boolean =
        prefs.getBoolean(PrefKeys.KEY_VIBRATION_ENABLED, true)

    companion object {
        @Volatile
        private var instance: UserPreferences? = null

        // 获取偏好存储单例
        fun from(context: Context): UserPreferences {
            return instance ?: synchronized(this) {
                instance ?: UserPreferences(context).also { instance = it }
            }
        }
    }
}
