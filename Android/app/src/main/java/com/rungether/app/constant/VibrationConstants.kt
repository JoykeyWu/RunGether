package com.rungether.app.constant

/**
 * 震动反馈相关常量
 *
 * 集中需求文档中对每种指令的震动规则毫秒数；调整规则时只需修改本文件，
 * 服务层会按规则映射为 VibrationEffect。
 */
object VibrationConstants {

    // 单次短震毫秒
    const val SHORT_MS: Long = 120L

    // 短震之间的间隔毫秒
    const val SHORT_GAP_MS: Long = 100L

    // 长震毫秒（障碍）
    const val LONG_MS: Long = 600L

    // 停止指令对应的持续震动毫秒
    const val STOP_HOLD_MS: Long = 3_000L
}
