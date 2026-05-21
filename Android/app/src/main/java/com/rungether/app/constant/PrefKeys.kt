package com.rungether.app.constant

/**
 * 偏好存储键名常量
 *
 * 集中定义 SharedPreferences 的存储文件名与字段键，避免双端读写不一致。
 */
object PrefKeys {

    // SharedPreferences 文件名
    const val PREF_FILE: String = "rungether_prefs"

    // 当前角色键
    const val KEY_ROLE: String = "current_role"

    // TTS 语速档位键（取值 SLOW/NORMAL/FAST）
    const val KEY_TTS_SPEED: String = "tts_speed"

    // 震动开关键
    const val KEY_VIBRATION_ENABLED: String = "vibration_enabled"

    // 最近成功配对的蓝牙设备地址
    const val KEY_LAST_PAIRED_MAC: String = "last_paired_mac"
}
