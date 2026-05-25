package com.rungether.app.bluetooth.protocol

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * 指令编解码器
 *
 * 将 GuideCommand 与 JSON 字符串相互转换，统一 JSON 协议字段：
 * { "kind": "direction|shortcut|sos|status", "type": "STRAIGHT|...", "payload": "..." }
 * 解析失败时返回 null，由上层选择忽略或上报告警。
 */
object CommandCodec {

    private const val KEY_KIND = "kind"
    private const val KEY_TYPE = "type"
    private const val KEY_ANGLE = "angle"
    private const val KEY_PAYLOAD = "payload"
    private const val KEY_LATITUDE = "lat"
    private const val KEY_LONGITUDE = "lng"
    private const val KEY_ACCUMULATED_M = "accumulated_m"
    private const val KEY_ELAPSED_SEC = "elapsed_sec"

    private const val KIND_DIRECTION = "direction"
    private const val KIND_SHORTCUT = "shortcut"
    private const val KIND_SOS = "sos"
    private const val KIND_STATUS = "status"
    private const val KIND_TELEMETRY = "telemetry"

    private val gson = Gson()

    // 将指令编码为 JSON 字符串，末尾追加换行作为简单的分包定界
    fun encode(command: GuideCommand): String {
        val obj = JsonObject()
        when (command) {
            is GuideCommand.Direction -> {
                obj.addProperty(KEY_KIND, KIND_DIRECTION)
                obj.addProperty(KEY_TYPE, command.type.name)
                obj.addProperty(KEY_ANGLE, command.angleDeg)
            }
            is GuideCommand.Shortcut -> {
                obj.addProperty(KEY_KIND, KIND_SHORTCUT)
                obj.addProperty(KEY_TYPE, command.type.name)
            }
            GuideCommand.Sos -> {
                obj.addProperty(KEY_KIND, KIND_SOS)
            }
            is GuideCommand.Status -> {
                obj.addProperty(KEY_KIND, KIND_STATUS)
                obj.addProperty(KEY_TYPE, command.type.name)
                if (command.payload != null) {
                    obj.addProperty(KEY_PAYLOAD, command.payload)
                }
            }
            is GuideCommand.Telemetry -> {
                obj.addProperty(KEY_KIND, KIND_TELEMETRY)
                obj.addProperty(KEY_LATITUDE, command.latitude)
                obj.addProperty(KEY_LONGITUDE, command.longitude)
                obj.addProperty(KEY_ACCUMULATED_M, command.accumulatedM)
                obj.addProperty(KEY_ELAPSED_SEC, command.elapsedSec)
            }
        }
        return gson.toJson(obj) + "\n"
    }

    // 解析单行 JSON 文本为指令，失败时返回 null
    fun decode(line: String): GuideCommand? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        val obj = runCatching { JsonParser.parseString(trimmed).asJsonObject }
            .getOrNull() ?: return null
        val kind = obj.get(KEY_KIND)?.asString ?: return null
        return when (kind) {
            KIND_DIRECTION -> parseDirection(obj)
            KIND_SHORTCUT -> parseShortcut(obj)
            KIND_SOS -> GuideCommand.Sos
            KIND_STATUS -> parseStatus(obj)
            KIND_TELEMETRY -> parseTelemetry(obj)
            else -> null
        }
    }

    private fun parseTelemetry(obj: JsonObject): GuideCommand? {
        val lat = obj.get(KEY_LATITUDE)?.asDouble ?: return null
        val lng = obj.get(KEY_LONGITUDE)?.asDouble ?: return null
        val accumulated = obj.get(KEY_ACCUMULATED_M)?.asDouble ?: 0.0
        val elapsed = obj.get(KEY_ELAPSED_SEC)?.asLong ?: 0L
        return GuideCommand.Telemetry(lat, lng, accumulated, elapsed)
    }

    private fun parseDirection(obj: JsonObject): GuideCommand? {
        val type = obj.get(KEY_TYPE)?.asString ?: return null
        val angle = obj.get(KEY_ANGLE)?.asInt ?: 0
        val parsedType = runCatching { DirectionType.valueOf(type) }.getOrNull() ?: return null
        return GuideCommand.Direction(parsedType, angle)
    }

    private fun parseShortcut(obj: JsonObject): GuideCommand? {
        val type = obj.get(KEY_TYPE)?.asString ?: return null
        val parsedType = runCatching { ShortcutType.valueOf(type) }.getOrNull() ?: return null
        return GuideCommand.Shortcut(parsedType)
    }

    private fun parseStatus(obj: JsonObject): GuideCommand? {
        val type = obj.get(KEY_TYPE)?.asString ?: return null
        val parsedType = runCatching { StatusType.valueOf(type) }.getOrNull() ?: return null
        val payload = obj.get(KEY_PAYLOAD)?.asString
        return GuideCommand.Status(parsedType, payload)
    }
}
