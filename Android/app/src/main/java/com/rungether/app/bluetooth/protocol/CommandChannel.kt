package com.rungether.app.bluetooth.protocol

import com.rungether.app.bluetooth.connection.BluetoothConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * 指令通道
 *
 * 在 BluetoothConnectionManager 的字节流之上叠加按行拆包与 JSON 编解码，
 * 对外暴露发送高阶指令 send 与接收指令的 Flow。
 *
 * 严格遵守 CLAUDE.md 的约束：UI 层只与 CommandChannel 交互，不允许直接持有 socket。
 */
class CommandChannel(
    private val connectionManager: BluetoothConnectionManager
) {

    private val channelScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _incoming = MutableSharedFlow<GuideCommand>(extraBufferCapacity = 64)
    private val incomingBuffer = StringBuilder()

    init {
        channelScope.launch {
            connectionManager.incomingBytes.collect { bytes ->
                appendAndDispatch(bytes)
            }
        }
    }

    // 对外暴露的解析后指令流
    val incoming: SharedFlow<GuideCommand> = _incoming.asSharedFlow()

    // 发送指令到对端
    fun send(command: GuideCommand): Boolean {
        val payload = CommandCodec.encode(command)
        return connectionManager.send(payload.toByteArray(Charsets.UTF_8))
    }

    private fun appendAndDispatch(bytes: ByteArray) {
        incomingBuffer.append(String(bytes, Charsets.UTF_8))
        while (true) {
            val newlineIndex = incomingBuffer.indexOf('\n')
            if (newlineIndex < 0) break
            val line = incomingBuffer.substring(0, newlineIndex)
            incomingBuffer.delete(0, newlineIndex + 1)
            val command = CommandCodec.decode(line) ?: continue
            _incoming.tryEmit(command)
        }
    }
}
