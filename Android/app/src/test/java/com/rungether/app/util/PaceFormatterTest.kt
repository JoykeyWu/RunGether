package com.rungether.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 配速换算与展示格式单元测试
 *
 * 校验需求文档中配速字段的计算口径与 UI 展示格式，避免后续重构破坏既有口径。
 */
class PaceFormatterTest {

    // 校验 1 公里 5 分钟应得到 5'00'' 文本
    @Test
    fun averagePace_oneKmInFiveMinutes_returnsFive() {
        val pace = PaceFormatter.averagePaceMinPerKm(distanceM = 1_000.0, durationSec = 300L)
        assertEquals(5.0, pace, 0.0001)
        assertEquals("5'00''", PaceFormatter.formatForDisplay(pace))
    }

    // 校验零距离场景应返回占位符
    @Test
    fun averagePace_zeroDistance_returnsZero() {
        val pace = PaceFormatter.averagePaceMinPerKm(distanceM = 0.0, durationSec = 600L)
        assertEquals(0.0, pace, 0.0001)
        assertEquals("—", PaceFormatter.formatForDisplay(pace))
    }
}
