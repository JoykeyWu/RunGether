package com.rungether.app.service.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

/**
 * 摇一摇检测器
 *
 * SensorManager 加速度采集 + 阈值算法：要求明显用力且持续约 2 秒以上才触发，
 * 跑步过程中的中低强度晃动不应误触发 SOS。
 *
 * 算法上对加速度去重力后取模长，连续超过阈值的窗口累计超 2 秒即触发一次事件，
 * 触发后进入冷静期避免连续连发。
 */
class ShakeDetector private constructor(appContext: Context) {

    private val applicationContext: Context = appContext.applicationContext
    private val sensorManager: SensorManager? =
        applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // 是否支持摇一摇
    fun isSupported(): Boolean = accelerometer != null

    // 启动摇一摇监听，返回 SOS 触发事件 Flow
    fun start(): Flow<ShakeEvent> = callbackFlow {
        val manager = sensorManager ?: run {
            close(IllegalStateException("SensorManager 不可用"))
            return@callbackFlow
        }
        val sensor = accelerometer ?: run {
            close(IllegalStateException("当前设备不支持加速度传感器"))
            return@callbackFlow
        }

        var overThresholdStartMs = 0L
        var lastFireMs = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x + y * y + z * z).toDouble())
                val now = SystemClock.elapsedRealtime()

                val active = magnitude >= TRIGGER_MAGNITUDE
                if (active) {
                    if (overThresholdStartMs == 0L) overThresholdStartMs = now
                    val held = now - overThresholdStartMs
                    val coolDownOk = now - lastFireMs > COOL_DOWN_MS
                    if (held >= MIN_HOLD_MS && coolDownOk) {
                        lastFireMs = now
                        overThresholdStartMs = 0L
                        trySend(ShakeEvent(magnitude = magnitude, heldMs = held))
                    }
                } else {
                    overThresholdStartMs = 0L
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { manager.unregisterListener(listener) }
    }

    companion object {
        // 触发阈值（m/s^2），含重力分量；19 对应明显用力摇晃
        private const val TRIGGER_MAGNITUDE = 19.0

        // 持续达到阈值的最小毫秒数
        private const val MIN_HOLD_MS = 2_000L

        // 触发后冷静期，避免连续连发
        private const val COOL_DOWN_MS = 4_000L

        @Volatile
        private var instance: ShakeDetector? = null

        // 获取摇一摇检测器单例
        fun from(context: Context): ShakeDetector {
            return instance ?: synchronized(this) {
                instance ?: ShakeDetector(context).also { instance = it }
            }
        }
    }
}

/**
 * 摇一摇触发事件
 *
 * 记录触发时的加速度模长与持续毫秒数，便于日后调参与定位误触发。
 */
data class ShakeEvent(
    val magnitude: Double,
    val heldMs: Long
)
