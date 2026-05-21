package com.rungether.app.bluetooth.connection

/**
 * 经典蓝牙连接状态
 *
 * 表示连接生命周期的离散态；UI 顶部连接状态条与重连策略均依据本枚举切换。
 */
sealed class ConnectionState {

    // 空闲：未发起连接
    data object Idle : ConnectionState()

    // 搜索中：扫描附近设备
    data object Scanning : ConnectionState()

    // 连接中：正在建立 SPP 通道
    data class Connecting(val deviceName: String?, val deviceAddress: String) : ConnectionState()

    // 已连接：SPP 通道已建立
    data class Connected(val deviceName: String?, val deviceAddress: String) : ConnectionState()

    // 已断开：附带是否触发超时报警
    data class Disconnected(val triggeredAlert: Boolean) : ConnectionState()

    // 出错：附带可读消息
    data class Error(val message: String) : ConnectionState()
}
