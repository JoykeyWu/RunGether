package com.rungether.app.constant

import java.util.UUID

/**
 * 经典蓝牙相关常量
 *
 * 集中 SPP 协议的 UUID、断线超时阈值、连接重试参数等关键配置，
 * 避免散落在多处导致两端不一致引起握手失败。
 */
object BluetoothConstants {

    // 经典蓝牙串口协议（SPP）标准 UUID，双端必须保持一致
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // 蓝牙服务名，搜索时用于过滤
    const val SERVICE_NAME: String = "RunGether-SPP"

    // 断线超过该毫秒数后触发语音提醒
    const val DISCONNECT_ALERT_MS: Long = 10_000L

    // 自动重连最大尝试次数
    const val MAX_RECONNECT_ATTEMPTS: Int = 5

    // 单次重连退避起始毫秒数
    const val RECONNECT_BACKOFF_START_MS: Long = 1_000L

    // 心跳间隔毫秒数
    const val HEARTBEAT_INTERVAL_MS: Long = 3_000L
}
