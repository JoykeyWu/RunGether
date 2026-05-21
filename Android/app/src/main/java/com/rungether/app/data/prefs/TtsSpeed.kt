package com.rungether.app.data.prefs

/**
 * TTS 语速档位
 *
 * 对应需求文档「语音速度：慢 / 正常 / 快」三档；
 * rate 字段为 TextToSpeech 的 setSpeechRate 直接传参。
 */
enum class TtsSpeed(val rate: Float) {
    SLOW(0.8f),
    NORMAL(1.0f),
    FAST(1.3f)
}
