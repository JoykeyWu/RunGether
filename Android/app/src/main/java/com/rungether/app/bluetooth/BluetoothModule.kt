package com.rungether.app.bluetooth

import android.content.Context
import com.rungether.app.bluetooth.connection.BluetoothConnectionManager
import com.rungether.app.bluetooth.protocol.CommandChannel

/**
 * 蓝牙模块全局单例容器
 *
 * 同一份 ConnectionManager 与 CommandChannel 在主界面、配对页、跑步中页面之间共享，
 * 避免任意页面单独构造导致连接被反复打断。
 */
object BluetoothModule {

    @Volatile
    private var connectionManager: BluetoothConnectionManager? = null

    @Volatile
    private var commandChannel: CommandChannel? = null

    // 获取连接管理器单例
    fun connectionManager(context: Context): BluetoothConnectionManager {
        return connectionManager ?: synchronized(this) {
            connectionManager ?: BluetoothConnectionManager(context.applicationContext)
                .also { connectionManager = it }
        }
    }

    // 获取指令通道单例
    fun commandChannel(context: Context): CommandChannel {
        return commandChannel ?: synchronized(this) {
            commandChannel ?: CommandChannel(connectionManager(context))
                .also { commandChannel = it }
        }
    }
}
