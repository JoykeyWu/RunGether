package com.rungether.app.service.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.rungether.app.ui.common.PermissionUtils
import com.rungether.app.util.DistanceFormatter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * GPS 定位服务
 *
 * 封装 LocationManager 的启停、权限校验、原始位置过滤与累计距离输出。
 * 处理 GPS 冷启动漂移与静止抖动，避免「没动就有距离」的体感问题。
 */
class LocationService private constructor(appContext: Context) {

    private val applicationContext: Context = appContext.applicationContext
    private val locationManager: LocationManager? =
        applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    // 是否具备定位权限
    fun hasPermission(): Boolean =
        PermissionUtils.isGranted(applicationContext, PermissionUtils.LOCATION_PERMISSIONS)

    // 启动定位流，发出已过滤的 LocationUpdate；过滤逻辑见 shouldAccept / isStationaryJitter
    @SuppressLint("MissingPermission")
    fun start(
        minTimeMs: Long = 1_000L
    ): Flow<LocationUpdate> = callbackFlow {
        val manager = locationManager ?: run {
            close(IllegalStateException("LocationManager 不可用"))
            return@callbackFlow
        }
        if (!hasPermission()) {
            close(SecurityException("缺少定位权限"))
            return@callbackFlow
        }

        var lastAccepted: Location? = null
        var accumulatedM = 0.0
        var warmedUp = false

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!location.hasAccuracy() || location.accuracy > ACCURACY_MAX_M) return
                if (!warmedUp) {
                    if (location.accuracy > ACCURACY_WARMUP_M) return
                    warmedUp = true
                    lastAccepted = location
                    trySend(buildUpdate(location, accumulatedM))
                    return
                }
                val previous = lastAccepted ?: return
                val delta = DistanceFormatter.haversineMeters(
                    previous.latitude, previous.longitude,
                    location.latitude, location.longitude
                )
                val jitterThreshold = (location.accuracy * JITTER_ACCURACY_FACTOR)
                    .coerceAtLeast(JITTER_MIN_M)
                if (delta < jitterThreshold) return
                accumulatedM += delta
                lastAccepted = location
                trySend(buildUpdate(location, accumulatedM))
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }

        manager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            minTimeMs,
            0f,
            listener,
            Looper.getMainLooper()
        )

        awaitClose { manager.removeUpdates(listener) }
    }

    private fun buildUpdate(location: Location, accumulatedM: Double): LocationUpdate =
        LocationUpdate(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyM = location.accuracy,
            speedMps = if (location.hasSpeed()) location.speed else 0f,
            elapsedSinceStartMs = android.os.SystemClock.elapsedRealtime(),
            accumulatedM = accumulatedM
        )

    companion object {
        // 全程接受的最大水平精度，超过此值的点视为不可信直接丢弃
        private const val ACCURACY_MAX_M = 30f

        // 冷启动 warm-up 阶段需达到的精度，达到后才认定 GPS 已锁定
        private const val ACCURACY_WARMUP_M = 20f

        // 静止抖动阈值的精度倍数：位移小于 accuracy*该系数视为站立漂移
        private const val JITTER_ACCURACY_FACTOR = 1.5f

        // 静止抖动阈值下限，避免高精度时阈值过低
        private const val JITTER_MIN_M = 2.0f

        @Volatile
        private var instance: LocationService? = null

        // 获取定位服务单例
        fun from(context: Context): LocationService {
            return instance ?: synchronized(this) {
                instance ?: LocationService(context).also { instance = it }
            }
        }
    }
}

/**
 * 单次位置更新
 *
 * 既携带原始经纬度，也提供累计米数，避免 UI 层反复换算。
 */
data class LocationUpdate(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float,
    val speedMps: Float,
    val elapsedSinceStartMs: Long,
    val accumulatedM: Double
)
