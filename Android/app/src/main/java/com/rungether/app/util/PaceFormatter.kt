package com.rungether.app.util

import java.util.Locale

/**
 * 配速计算与格式化工具
 *
 * 跑步数据面板、结算页、历史详情共用同一套配速文本，避免不同页面口径不一致。
 */
object PaceFormatter {

    // 计算平均配速：分钟/公里
    // distanceM 与 durationSec 任一为 0 时返回 0.0
    fun averagePaceMinPerKm(distanceM: Double, durationSec: Long): Double {
        if (distanceM <= 0.0 || durationSec <= 0) return 0.0
        val km = distanceM / 1_000.0
        val minutes = durationSec / 60.0
        return minutes / km
    }

    // 配速格式化展示：5'30''
    fun formatForDisplay(paceMinPerKm: Double): String {
        if (paceMinPerKm <= 0.0 || paceMinPerKm.isInfinite() || paceMinPerKm.isNaN()) {
            return "—"
        }
        val totalSeconds = (paceMinPerKm * 60).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.CHINA, "%d'%02d''", minutes, seconds)
    }

    // 配速语音播报：5 分 30 秒
    fun formatForSpeech(paceMinPerKm: Double): String {
        if (paceMinPerKm <= 0.0 || paceMinPerKm.isInfinite() || paceMinPerKm.isNaN()) {
            return "未达到统计阈值"
        }
        val totalSeconds = (paceMinPerKm * 60).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes 分 $seconds 秒每公里"
    }
}
