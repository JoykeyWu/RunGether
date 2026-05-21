package com.rungether.app.util

import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.atan2

/**
 * 距离换算与格式化工具
 *
 * 同时承担两类职责：跑步距离展示格式化、两点 GPS 坐标的 Haversine 距离计算。
 */
object DistanceFormatter {

    private const val EARTH_RADIUS_M = 6_371_000.0

    // 米转公里展示：1.23 公里；不足 1 公里返回 800 米
    fun formatForDisplay(distanceM: Double): String {
        val safe = if (distanceM < 0) 0.0 else distanceM
        return if (safe < 1_000) {
            String.format(Locale.CHINA, "%d 米", safe.toInt())
        } else {
            String.format(Locale.CHINA, "%.2f 公里", safe / 1_000.0)
        }
    }

    // 语音播报：12 点 3 公里
    fun formatForSpeech(distanceM: Double): String {
        val safe = if (distanceM < 0) 0.0 else distanceM
        return if (safe < 1_000) {
            "${safe.toInt()} 米"
        } else {
            String.format(Locale.CHINA, "%.2f 公里", safe / 1_000.0)
        }
    }

    // Haversine 公式计算两点经纬度间的米数
    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }
}
