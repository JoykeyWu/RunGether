package com.rungether.app.service.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.rungether.app.data.prefs.TtsSpeed
import com.rungether.app.data.prefs.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * 语音播报服务
 *
 * 封装 TextToSpeech 生命周期、中文引擎检测与语速档位；
 * 调用方传入文案与可选优先级，服务内部统一队列管理避免跨页面漏播或抢断。
 */
class TtsService private constructor(
    appContext: Context
) {

    private val applicationContext: Context = appContext.applicationContext
    private val preferences = UserPreferences.from(applicationContext)

    private val _ready = MutableStateFlow(false)
    private val _speaking = MutableStateFlow(false)
    private val utteranceCounter = AtomicLong(0L)

    private val tts: TextToSpeech = TextToSpeech(applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            applyLocale()
            applySpeed(preferences.ttsSpeed)
            _ready.value = true
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _speaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _speaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _speaking.value = false
            }
        })
    }

    // 播报一段文本：mode 决定是否打断当前队列
    fun speak(text: CharSequence, mode: Mode = Mode.QUEUE) {
        if (text.isBlank()) return
        if (!_ready.value) return
        val queueMode = when (mode) {
            Mode.QUEUE -> TextToSpeech.QUEUE_ADD
            Mode.FLUSH -> TextToSpeech.QUEUE_FLUSH
        }
        val id = "utt-${utteranceCounter.incrementAndGet()}"
        tts.speak(text, queueMode, null, id)
    }

    // 应用最新语速档位
    fun applySpeed(speed: TtsSpeed) {
        tts.setSpeechRate(speed.rate)
    }

    // 停止当前播报
    fun stop() {
        tts.stop()
        _speaking.value = false
    }

    private fun applyLocale() {
        val locale = Locale.SIMPLIFIED_CHINESE
        val supported = tts.isLanguageAvailable(locale)
        if (supported != TextToSpeech.LANG_MISSING_DATA &&
            supported != TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            tts.language = locale
        } else {
            tts.language = Locale.CHINESE
        }
    }

    // 播报模式
    enum class Mode {
        QUEUE,
        FLUSH
    }

    companion object {
        @Volatile
        private var instance: TtsService? = null

        // 获取语音播报服务单例
        fun from(context: Context): TtsService {
            return instance ?: synchronized(this) {
                instance ?: TtsService(context).also { instance = it }
            }
        }
    }
}
