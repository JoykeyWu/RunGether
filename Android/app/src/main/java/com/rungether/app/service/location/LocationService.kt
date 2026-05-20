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
 * 封装 LocationManager 的启停、权限校验与位置点输出；
 * 仅返回原始 LatLngPoint 序列与累计米数，轨迹绘制由 ui/widget/TrackView 完成。
 */
class LocationService private constructor(appContext: Context) {

    private val applicationContext: Context = appContext.applicationContext
    private val locationManager: LocationManager? =
        applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    // 是否具备定位权限
    fun hasPermission(): Boolean =
        PermissionUtils.isGranted(applicationContext, PermissionUtils.LOCATION_PERMISSIONS)

    // 启动定位流：返回 LocationUpdate 的 Flow
    @SuppressLint("MissingPermission")
    fun start(
        minTimeMs: Long = 1_000L,
        minDistanceM: Float = 1f
    ): Flow<LocationUpdate> = callbackFlow {
        val manager = locationManager ?: run {
            close(IllegalStateException("LocationManager 不可用"))
            return@callbackFlow
        }
        if (!hasPermission()) {
            close(SecurityException("缺少定位权限"))
            return@callbackFlow
        }

        var lastLocation: Location? = null
        var accumulatedM = 0.0

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val previous = lastLocation
                if (previous != null) {
                    accumulatedM += DistanceFormatter.haversineMeters(
                        previous.latitude, previous.longitude,
                        location.latitude, location.longitude
                    )
                }
                lastLocation = location
                trySend(
                    LocationUpdate(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyM = location.accuracy,
                        speedMps = if (location.hasSpeed()) location.speed else 0f,
                        elapsedSinceStartMs = SystemClockSafe.elapsedRealtime(),
                        accumulatedM = accumulatedM
                    )
                )
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }

        manager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            minTimeMs,
            minDistanceM,
            listener,
            Looper.getMainLooper()
        )

        awaitClose { manager.removeUpdates(listener) }
    }

    companion object {
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

private object SystemClockSafe {
    fun elapsedRealtime(): Long = android.os.SystemClock.elapsedRealtime()
}
