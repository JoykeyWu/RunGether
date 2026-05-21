package com.rungether.app.bluetooth.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.rungether.app.constant.BluetoothConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

/**
 * 经典蓝牙连接生命周期管理
 *
 * 负责搜索、配对、连接、自动重连、断线计时与 SPP Socket 的读写通道暴露。
 * 仅维护连接生命周期，不解析具体业务指令；指令协议由 bluetooth/protocol 实现。
 *
 * 外部访问严格遵守经典蓝牙的线程模型，所有 IO 在 IO 调度器执行，
 * 状态流在主线程或绑定生命周期处收集即可。
 */
class BluetoothConnectionManager(
    private val appContext: Context
) {

    private val managerScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    private val _incoming = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var readJob: Job? = null
    private var reconnectAttempt: Int = 0

    // 连接状态流
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    // 原始入站字节流（按 socket 单次读返回粒度）
    val incomingBytes: SharedFlow<ByteArray> = _incoming.asSharedFlow()

    // 已配对设备列表
    @SuppressLint("MissingPermission")
    fun bondedDevices(): List<BluetoothDevice> {
        val adapter = adapter() ?: return emptyList()
        return adapter.bondedDevices?.toList().orEmpty()
    }

    // 是否支持蓝牙
    fun isBluetoothSupported(): Boolean = adapter() != null

    // 蓝牙是否已开启
    fun isBluetoothEnabled(): Boolean = adapter()?.isEnabled == true

    // 主动连接指定设备
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        val adapter = adapter() ?: run {
            _state.value = ConnectionState.Error("当前设备不支持蓝牙")
            return
        }
        adapter.cancelDiscovery()
        _state.value = ConnectionState.Connecting(device.name, device.address)
        managerScope.launch { performConnect(device) }
    }

    // 断开当前连接
    fun disconnect() {
        managerScope.launch { closeQuietly(triggerAlert = false) }
    }

    // 发送一段字节到对端
    fun send(bytes: ByteArray): Boolean {
        val out = outputStream ?: return false
        return runCatching {
            out.write(bytes)
            out.flush()
            true
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    private suspend fun performConnect(device: BluetoothDevice) {
        runCatching {
            val newSocket = device.createRfcommSocketToServiceRecord(BluetoothConstants.SPP_UUID)
            newSocket.connect()
            socket = newSocket
            inputStream = newSocket.inputStream
            outputStream = newSocket.outputStream
            reconnectAttempt = 0
            _state.value = ConnectionState.Connected(device.name, device.address)
            startReadLoop(device)
        }.onFailure { throwable ->
            closeQuietly(triggerAlert = true)
            _state.value = ConnectionState.Error(throwable.message ?: "蓝牙连接失败")
            scheduleReconnect(device)
        }
    }

    private fun startReadLoop(device: BluetoothDevice) {
        readJob?.cancel()
        readJob = managerScope.launch {
            val buffer = ByteArray(1024)
            val input = inputStream ?: return@launch
            while (true) {
                val read = runCatching { input.read(buffer) }.getOrElse { -1 }
                if (read <= 0) {
                    closeQuietly(triggerAlert = true)
                    scheduleReconnect(device)
                    break
                }
                _incoming.tryEmit(buffer.copyOf(read))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun scheduleReconnect(device: BluetoothDevice) {
        if (reconnectAttempt >= BluetoothConstants.MAX_RECONNECT_ATTEMPTS) return
        val backoff = BluetoothConstants.RECONNECT_BACKOFF_START_MS * (1L shl reconnectAttempt)
        reconnectAttempt += 1
        delay(backoff)
        _state.value = ConnectionState.Connecting(device.name, device.address)
        performConnect(device)
    }

    private suspend fun closeQuietly(triggerAlert: Boolean) = withContext(Dispatchers.IO) {
        readJob?.cancel()
        readJob = null
        runCatching { inputStream?.close() }
        runCatching { outputStream?.close() }
        runCatching { socket?.close() }
        inputStream = null
        outputStream = null
        socket = null
        _state.value = ConnectionState.Disconnected(triggerAlert)
    }

    private fun adapter(): BluetoothAdapter? {
        val manager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    // 释放所有资源（在应用退出或角色切换时调用）
    fun release() {
        managerScope.launch {
            closeQuietly(triggerAlert = false)
        }
    }
}
