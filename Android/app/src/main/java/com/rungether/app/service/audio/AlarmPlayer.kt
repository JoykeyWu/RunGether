package com.rungether.app.service.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager

/**
 * 警报声播放器
 *
 * 紧急求助页通过本播放器循环响起系统默认报警铃声，吸引周围注意；
 * 避免在工程内携带音频二进制，由系统铃声资源就地满足。
 * 解除求助时调用 stop 立即静音并释放 MediaPlayer。
 */
class AlarmPlayer private constructor(appContext: Context) {

    private val applicationContext: Context = appContext.applicationContext
    private val audioManager: AudioManager? =
        applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var player: MediaPlayer? = null
    private var previousAlarmVolume: Int = -1

    // 启动循环播放警报声；已在播则忽略
    fun start() {
        if (player != null) return
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        boostAlarmVolume()
        runCatching {
            MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(applicationContext, uri)
                isLooping = true
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        }.onSuccess { player = it }
            .onFailure { restoreAlarmVolume() }
    }

    // 停止播放并释放资源；恢复进入前的音量
    fun stop() {
        runCatching {
            player?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
        player = null
        restoreAlarmVolume()
    }

    private fun boostAlarmVolume() {
        val manager = audioManager ?: return
        if (previousAlarmVolume >= 0) return
        previousAlarmVolume = manager.getStreamVolume(AudioManager.STREAM_ALARM)
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        runCatching { manager.setStreamVolume(AudioManager.STREAM_ALARM, max, 0) }
    }

    private fun restoreAlarmVolume() {
        val manager = audioManager ?: return
        if (previousAlarmVolume < 0) return
        runCatching { manager.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, 0) }
        previousAlarmVolume = -1
    }

    companion object {
        @Volatile
        private var instance: AlarmPlayer? = null

        // 获取警报声播放器单例
        fun from(context: Context): AlarmPlayer {
            return instance ?: synchronized(this) {
                instance ?: AlarmPlayer(context).also { instance = it }
            }
        }
    }
}
