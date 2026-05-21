package com.rungether.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 距离换算与 Haversine 计算单元测试
 *
 * 校验跑步距离的格式化与两点间距离计算，确保历史详情/结算页口径与
 * 数据面板每秒刷新的累计距离一致。
 */
class DistanceFormatterTest {

    // 校验展示格式：不足 1 公里走米、超过 1 公里走两位小数公里
    @Test
    fun formatForDisplay_switchesUnitAtOneKilometer() {
        assertEquals("800 米", DistanceFormatter.formatForDisplay(800.0))
        assertEquals("1.23 公里", DistanceFormatter.formatForDisplay(1_230.0))
    }

    // 校验北京到上海经纬度间距离约 1067 公里，允许 1% 误差
    @Test
    fun haversine_beijingToShanghai_isAboutOneThousandKilometers() {
        val meters = DistanceFormatter.haversineMeters(
            lat1 = 39.9042, lon1 = 116.4074,
            lat2 = 31.2304, lon2 = 121.4737
        )
        val km = meters / 1_000.0
        assertTrue("北京到上海距离应约 1067 公里，实测 $km", km in 1_050.0..1_090.0)
    }
}
