package com.rungether.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日期时间格式化工具
 *
 * 跑步记录、历史列表与结算页共用同一套显示格式，避免不同页面格式不一致。
 */
object DateFormatter {

    private val LIST_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    private val DETAIL_FORMAT = SimpleDateFormat("yyyy 年 MM 月 dd 日 HH:mm", Locale.CHINA)
    private val SPEECH_FORMAT = SimpleDateFormat("M 月 d 日 H 时 m 分", Locale.CHINA)

    // 列表展示用：2026-05-20 09:41
    fun formatForList(timestampMs: Long): String = LIST_FORMAT.format(Date(timestampMs))

    // 详情展示用：2026 年 05 月 20 日 09:41
    fun formatForDetail(timestampMs: Long): String = DETAIL_FORMAT.format(Date(timestampMs))

    // 语音播报用：5 月 20 日 9 时 41 分
    fun formatForSpeech(timestampMs: Long): String = SPEECH_FORMAT.format(Date(timestampMs))

    // 跑步时长（秒）格式化为 HH:mm:ss
    fun formatDuration(durationSec: Long): String {
        val safe = if (durationSec < 0) 0 else durationSec
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return String.format(Locale.CHINA, "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
