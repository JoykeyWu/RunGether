package com.rungether.app.bluetooth.connection

import android.bluetooth.BluetoothDevice

/**
 * 蓝牙搜索过程中发现的设备
 *
 * 在 BluetoothDevice 基础上携带本轮搜索捕获到的 RSSI 信号强度，
 * 用于陪跑员配对页按强中弱三档展示。
 */
data class DiscoveredDevice(
    val device: BluetoothDevice,
    val name: String?,
    val address: String,
    val rssi: Short,
    val bonded: Boolean
) {

    // 信号强度三档
    enum class SignalLevel { STRONG, MEDIUM, WEAK }

    // 根据 RSSI 给出强中弱三档：>= -60 强；-60 ~ -80 中；< -80 弱
    val signalLevel: SignalLevel
        get() = when {
            rssi.toInt() >= -60 -> SignalLevel.STRONG
            rssi.toInt() >= -80 -> SignalLevel.MEDIUM
            else -> SignalLevel.WEAK
        }
}
