package com.rungether.app.ui.guide

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rungether.app.bluetooth.BluetoothModule
import com.rungether.app.bluetooth.connection.ConnectionState
import com.rungether.app.bluetooth.protocol.DirectionType
import com.rungether.app.bluetooth.protocol.GuideCommand
import com.rungether.app.bluetooth.protocol.ShortcutType
import com.rungether.app.bluetooth.protocol.StatusType
import com.rungether.app.data.local.entity.RunRecordEntity
import com.rungether.app.data.prefs.UserRole
import com.rungether.app.data.repository.RepositoryProvider
import com.rungether.app.service.location.LocationService
import com.rungether.app.service.location.LocationUpdate
import com.rungether.app.util.PaceFormatter
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * 陪跑员引导主界面 ViewModel
 *
 * 持有跑步会话的实时状态：阶段、时长、距离、配速、轨迹点；
 * 提供开始/结束、方向指令下行、快捷指令下行、SOS 接收等业务入口。
 * 严格遵守仓库 + 服务层抽象，不直接持有 socket、Vibrator、LocationManager 等系统对象。
 */
class GuideMainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val connectionManager = BluetoothModule.connectionManager(appContext)
    private val commandChannel = BluetoothModule.commandChannel(appContext)
    private val locationService = LocationService.from(appContext)
    private val runRecordRepository = RepositoryProvider.runRecord(appContext)

    private val _phase = MutableStateFlow(GuideRunPhase.IDLE)
    private val _elapsedSec = MutableStateFlow(0L)
    private val _distanceM = MutableStateFlow(0.0)
    private val _paceMinPerKm = MutableStateFlow(0.0)
    private val _trackPoints = MutableStateFlow<List<DoubleArray>>(emptyList())
    private val _sosReceived = MutableStateFlow(0L)
    private val _currentDirection = MutableStateFlow("保持直行")

    private var startedAt: Long = 0L
    private var tickerJob: Job? = null
    private var locationJob: Job? = null
    private val lastDirectionSentMs = AtomicLong(0L)

    // 跑步阶段
    val phase: StateFlow<GuideRunPhase> = _phase.asStateFlow()

    // 累计时长（秒）
    val elapsedSec: StateFlow<Long> = _elapsedSec.asStateFlow()

    // 累计距离（米）
    val distanceM: StateFlow<Double> = _distanceM.asStateFlow()

    // 实时配速（分钟/公里）
    val paceMinPerKm: StateFlow<Double> = _paceMinPerKm.asStateFlow()

    // 轨迹坐标序列
    val trackPoints: StateFlow<List<DoubleArray>> = _trackPoints.asStateFlow()

    // 收到 SOS 的时间戳计数器（每次改变触发 UI 弹窗）
    val sosReceived: StateFlow<Long> = _sosReceived.asStateFlow()

    // 当前发送的方向文案，用于摇杆下方提示
    val currentDirection: StateFlow<String> = _currentDirection.asStateFlow()

    // 连接状态流
    val connectionState: StateFlow<ConnectionState> = connectionManager.state

    // 入站指令流，用于监听 SOS
    val incomingCommands: SharedFlow<GuideCommand> = commandChannel.incoming

    init {
        viewModelScope.launch {
            commandChannel.incoming.collect { command ->
                if (command is GuideCommand.Sos) {
                    _sosReceived.value = System.currentTimeMillis()
                }
            }
        }
    }

    // 开始陪跑：重置数据、启动定时器与 GPS、通知对端跑步开始
    fun startRun() {
        if (_phase.value == GuideRunPhase.RUNNING) return
        startedAt = System.currentTimeMillis()
        _elapsedSec.value = 0L
        _distanceM.value = 0.0
        _paceMinPerKm.value = 0.0
        _trackPoints.value = emptyList()
        _currentDirection.value = "保持直行"
        _phase.value = GuideRunPhase.RUNNING

        startTicker()
        startLocation()
        commandChannel.send(GuideCommand.Status(StatusType.START))
    }

    // 结束陪跑：停止定时器与 GPS、通知对端、落本地并异步上传
    fun endRun(onSaved: (Long) -> Unit) {
        if (_phase.value != GuideRunPhase.RUNNING) return
        tickerJob?.cancel()
        tickerJob = null
        locationJob?.cancel()
        locationJob = null
        commandChannel.send(GuideCommand.Status(StatusType.END))
        val duration = _elapsedSec.value
        val distance = _distanceM.value
        val pace = PaceFormatter.averagePaceMinPerKm(distance, duration)
        val points = _trackPoints.value
        val entity = RunRecordEntity(
            ownerRole = UserRole.GUIDE.name,
            startedAt = startedAt,
            durationSec = duration,
            distanceM = distance,
            averagePace = pace,
            trackJson = Gson().toJson(points)
        )
        viewModelScope.launch {
            val id = runRecordRepository.saveLocallyThenSync(entity)
            _phase.value = GuideRunPhase.IDLE
            onSaved(id)
        }
    }

    // 摇杆下发方向指令；同档位重复在 300ms 内的发送会被节流忽略，避免高频写满 socket
    fun onDirectionChanged(type: DirectionType, angleDeg: Int) {
        val now = System.currentTimeMillis()
        val last = lastDirectionSentMs.get()
        if (now - last < 200) return
        lastDirectionSentMs.set(now)
        _currentDirection.value = directionText(type, angleDeg)
        commandChannel.send(GuideCommand.Direction(type, angleDeg))
    }

    // 快捷指令下发
    fun sendShortcut(type: ShortcutType) {
        commandChannel.send(GuideCommand.Shortcut(type))
        _currentDirection.value = shortcutText(type)
    }

    // 重置 SOS 提醒标记
    fun consumeSos() {
        _sosReceived.value = 0L
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val seconds = ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(0L)
                _elapsedSec.value = seconds
                _paceMinPerKm.value = PaceFormatter.averagePaceMinPerKm(_distanceM.value, seconds)
                delay(1_000L)
            }
        }
    }

    private fun startLocation() {
        locationJob?.cancel()
        if (!locationService.hasPermission()) return
        locationJob = viewModelScope.launch {
            runCatching {
                locationService.start().collect { update ->
                    onLocationUpdate(update)
                }
            }
        }
    }

    private fun onLocationUpdate(update: LocationUpdate) {
        _distanceM.value = update.accumulatedM
        _trackPoints.update { points ->
            points + doubleArrayOf(update.latitude, update.longitude)
        }
    }

    private fun directionText(type: DirectionType, angleDeg: Int): String {
        val side = if (angleDeg >= 0) "右" else "左"
        return when (type) {
            DirectionType.STRAIGHT -> "保持直行"
            DirectionType.MICRO -> "微调 $side"
            DirectionType.TURN -> "转弯 $side"
            DirectionType.HARD_TURN -> "大转弯 $side"
        }
    }

    private fun shortcutText(type: ShortcutType): String = when (type) {
        ShortcutType.OBSTACLE -> "前方障碍"
        ShortcutType.SLOW_DOWN -> "减速"
        ShortcutType.STOP -> "停止"
    }

    override fun onCleared() {
        tickerJob?.cancel()
        locationJob?.cancel()
        super.onCleared()
    }
}
