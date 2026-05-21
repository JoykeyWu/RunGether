package com.rungether.app.service.torch

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 手电筒服务
 *
 * CameraManager 闪光灯启停 + 按节拍闪烁封装；含资源释放与异常兜底。
 * 紧急求助页通过 startBlink 在前景持续吸引注意，解除时调用 stop。
 */
class TorchService private constructor(appContext: Context) {

    private val applicationContext: Context = appContext.applicationContext
    private val cameraManager: CameraManager? =
        applicationContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val torchScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var blinkJob: Job? = null
    private var torchOn: Boolean = false

    // 是否具备闪光灯硬件
    fun hasFlash(): Boolean = resolveCameraId() != null

    // 单次开关：true 开、false 关
    fun setTorch(enabled: Boolean): Boolean {
        val cameraId = resolveCameraId() ?: return false
        val manager = cameraManager ?: return false
        return runCatching {
            manager.setTorchMode(cameraId, enabled)
            torchOn = enabled
            true
        }.getOrElse {
            if (it is CameraAccessException) false else throw it
        }
    }

    // 按节拍闪烁：onMs 亮、offMs 灭，循环到 stop 调用
    fun startBlink(onMs: Long = 400L, offMs: Long = 400L) {
        stopBlink()
        if (!hasFlash()) return
        blinkJob = torchScope.launch {
            while (true) {
                setTorch(true)
                delay(onMs)
                setTorch(false)
                delay(offMs)
            }
        }
    }

    // 停止闪烁并关灯
    fun stop() {
        stopBlink()
        if (torchOn) setTorch(false)
    }

    // 仅停止闪烁循环，不强制关灯（用于切换到常亮）
    private fun stopBlink() {
        blinkJob?.cancel()
        blinkJob = null
    }

    private fun resolveCameraId(): String? {
        val manager = cameraManager ?: return null
        val ids = runCatching { manager.cameraIdList }.getOrNull() ?: return null
        for (id in ids) {
            val characteristics = runCatching { manager.getCameraCharacteristics(id) }.getOrNull()
                ?: continue
            val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                return id
            }
        }
        return null
    }

    companion object {
        @Volatile
        private var instance: TorchService? = null

        // 获取手电筒服务单例
        fun from(context: Context): TorchService {
            return instance ?: synchronized(this) {
                instance ?: TorchService(context).also { instance = it }
            }
        }
    }
}
