package com.rungether.app.bluetooth.protocol

/**
 * 陪跑员下行指令模型
 *
 * 与需求文档「反馈规则」与「快捷指令」对齐：
 * 方向类（直行 / 微调 / 转弯 / 大转弯），快捷类（障碍 / 减速 / 停止），SOS 类、状态类。
 * 序列化采用 JSON snake_case，由 CommandCodec 负责双向转换。
 */
sealed class GuideCommand {

    // 方向类指令：angle 取 [-180, 180]，0 表示正前方，正数右转、负数左转
    data class Direction(val type: DirectionType, val angleDeg: Int) : GuideCommand()

    // 快捷指令
    data class Shortcut(val type: ShortcutType) : GuideCommand()

    // 紧急求助指令
    data object Sos : GuideCommand()

    // 状态同步指令：跑步开始、结束、心跳等
    data class Status(val type: StatusType, val payload: String? = null) : GuideCommand()

    // 盲人端实时位置上报：经纬度 + 累计米数 + 已运行秒数；用于陪跑端绘制对端轨迹
    data class Telemetry(
        val latitude: Double,
        val longitude: Double,
        val accumulatedM: Double,
        val elapsedSec: Long
    ) : GuideCommand()
}

/**
 * 方向类指令枚举
 *
 * 与震动反馈规则一一映射：STRAIGHT 不震、MICRO 短1、TURN 短2、HARD_TURN 短3。
 */
enum class DirectionType {
    STRAIGHT,
    MICRO,
    TURN,
    HARD_TURN
}

/**
 * 快捷指令枚举
 *
 * 与震动反馈规则一一映射：OBSTACLE 长1、SLOW_DOWN 短2、STOP 持续 3 秒。
 */
enum class ShortcutType {
    OBSTACLE,
    SLOW_DOWN,
    STOP
}

/**
 * 状态同步类型
 *
 * START / END 触发对端跑步计时的开启关闭；HEARTBEAT 用于断线超时计时。
 */
enum class StatusType {
    START,
    END,
    HEARTBEAT
}
