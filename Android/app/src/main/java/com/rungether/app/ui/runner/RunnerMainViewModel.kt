package com.rungether.app.ui.runner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
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
import com.rungether.app.service.tts.TtsService
import com.rungether.app.service.vibration.VibrationService
import com.rungether.app.util.PaceFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 盲人端主界面 ViewModel
 *
 * 维护跑步阶段、累计时长与距离；订阅 CommandChannel 入站指令并触发 TTS 与震动反馈；
 * 严格通过 Repository 与服务层入口，不直接持有 Vibrator、Socket、LocationManager。
 */
class RunnerMainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val connectionManager = BluetoothModule.connectionManager(appContext)
    private val commandChannel = BluetoothModule.commandChannel(appContext)
    private val locationService = LocationService.from(appContext)
    private val ttsService = TtsService.from(appContext)
    private val vibrationService = VibrationService.from(appContext)
    private val runRecordRepository = RepositoryProvider.runRecord(appContext)

    private val _phase = MutableStateFlow(RunnerRunPhase.IDLE)
    private val _elapsedSec = MutableStateFlow(0L)
    private val _distanceM = MutableStateFlow(0.0)
    private val _trackPoints = MutableStateFlow<List<DoubleArray>>(emptyList())

    private var startedAt: Long = 0L
    private var tickerJob: Job? = null
    private var locationJob: Job? = null
    private var lastSpokenDirection: DirectionType? = null
    private var lastDirectionSpokenAt: Long = 0L
    private var lastTelemetrySentAt: Long = 0L

    // 跑步阶段
    val phase: StateFlow<RunnerRunPhase> = _phase.asStateFlow()

    // 累计时长（秒）
    val elapsedSec: StateFlow<Long> = _elapsedSec.asStateFlow()

    // 累计距离（米）
    val distanceM: StateFlow<Double> = _distanceM.asStateFlow()

    // 连接状态流
    val connectionState: StateFlow<ConnectionState> = connectionManager.state

    init {
        viewModelScope.launch {
            commandChannel.incoming.collect { handleIncoming(it) }
        }
    }

    // 开始本次跑步：重置数据、启动 ticker 与 GPS、TTS 播报、状态告知陪跑员
    fun startRun() {
        if (_phase.value == RunnerRunPhase.RUNNING) return
        startedAt = System.currentTimeMillis()
        _elapsedSec.value = 0L
        _distanceM.value = 0.0
        _trackPoints.value = emptyList()
        _phase.value = RunnerRunPhase.RUNNING

        startTicker()
        startLocation()
        commandChannel.send(GuideCommand.Status(StatusType.START))
        ttsService.speak("开始跑步，已为您开启计时与定位", TtsService.Mode.FLUSH)
    }

    // 结束本次跑步：停止 ticker 与 GPS、状态告知陪跑员、落本地并返回主键
    fun endRun(onSaved: (Long) -> Unit) {
        if (_phase.value != RunnerRunPhase.RUNNING) return
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
            ownerRole = UserRole.RUNNER.name,
            startedAt = startedAt,
            durationSec = duration,
            distanceM = distance,
            averagePace = pace,
            trackJson = Gson().toJson(points)
        )
        viewModelScope.launch {
            val id = runRecordRepository.saveLocallyThenSync(entity)
            _phase.value = RunnerRunPhase.IDLE
            onSaved(id)
        }
    }

    // 触发紧急求助：通知陪跑员、TTS 播报、震动；后续手电筒 / 警报声由求助页接管
    fun triggerSos() {
        commandChannel.send(GuideCommand.Sos)
        vibrationService.feedback(GuideCommand.Sos)
        ttsService.speak("紧急求助已启动", TtsService.Mode.FLUSH)
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val seconds = ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(0L)
                _elapsedSec.value = seconds
                delay(1_000L)
            }
        }
    }

    private fun startLocation() {
        locationJob?.cancel()
        if (!locationService.hasPermission()) return
        locationJob = viewModelScope.launch {
            runCatching {
                locationService.start().collect { update -> onLocationUpdate(update) }
            }
        }
    }

    private fun onLocationUpdate(update: LocationUpdate) {
        _distanceM.value = update.accumulatedM
        _trackPoints.update { it + doubleArrayOf(update.latitude, update.longitude) }
        broadcastTelemetry(update)
    }

    // 经过精度/抖动过滤的位置才会进到这里，1 秒一次节流广播给陪跑端
    private fun broadcastTelemetry(update: LocationUpdate) {
        val now = System.currentTimeMillis()
        if (now - lastTelemetrySentAt < TELEMETRY_INTERVAL_MS) return
        lastTelemetrySentAt = now
        commandChannel.send(
            GuideCommand.Telemetry(
                latitude = update.latitude,
                longitude = update.longitude,
                accumulatedM = update.accumulatedM,
                elapsedSec = _elapsedSec.value
            )
        )
    }

    private fun handleIncoming(command: GuideCommand) {
        vibrationService.feedback(command)
        when (command) {
            is GuideCommand.Direction -> speakDirection(command)
            is GuideCommand.Shortcut -> ttsService.speak(shortcutText(command.type), TtsService.Mode.FLUSH)
            GuideCommand.Sos -> Unit
            is GuideCommand.Status -> speakStatus(command.type)
            is GuideCommand.Telemetry -> Unit
        }
    }

    // 方向类指令：同类型在 1.5 秒内仅播报一次，避免高频播报抢断；直行不播报
    private fun speakDirection(direction: GuideCommand.Direction) {
        if (direction.type == DirectionType.STRAIGHT) {
            lastSpokenDirection = DirectionType.STRAIGHT
            return
        }
        val now = System.currentTimeMillis()
        val cooled = now - lastDirectionSpokenAt > 1_500L
        if (direction.type == lastSpokenDirection && !cooled) return
        lastSpokenDirection = direction.type
        lastDirectionSpokenAt = now
        val side = if (direction.angleDeg >= 0) "右" else "左"
        val text = when (direction.type) {
            DirectionType.MICRO -> "${side}侧微调"
            DirectionType.TURN -> "向${side}转弯"
            DirectionType.HARD_TURN -> "向${side}大转弯"
            DirectionType.STRAIGHT -> return
        }
        ttsService.speak(text, TtsService.Mode.FLUSH)
    }

    private fun shortcutText(type: ShortcutType): String = when (type) {
        ShortcutType.OBSTACLE -> "注意，前方障碍"
        ShortcutType.SLOW_DOWN -> "请减速"
        ShortcutType.STOP -> "请立即停止"
    }

    private fun speakStatus(type: StatusType) {
        when (type) {
            StatusType.START -> Unit
            StatusType.END -> ttsService.speak("陪跑员已结束本次跑步")
            StatusType.HEARTBEAT -> Unit
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        locationJob?.cancel()
        super.onCleared()
    }

    private companion object {
        // 位置上报最小间隔，避免高频写满蓝牙串口
        const val TELEMETRY_INTERVAL_MS = 1_000L
    }
}
