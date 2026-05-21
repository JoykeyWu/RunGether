package com.rungether.app.ui.sos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rungether.app.bluetooth.BluetoothModule
import com.rungether.app.bluetooth.protocol.GuideCommand
import com.rungether.app.data.local.entity.EmergencyContactEntity
import com.rungether.app.data.repository.RepositoryProvider
import com.rungether.app.service.audio.AlarmPlayer
import com.rungether.app.service.torch.TorchService
import com.rungether.app.service.tts.TtsService
import com.rungether.app.service.vibration.VibrationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 盲人端紧急求助页 ViewModel
 *
 * 进入页面时并行启动四路反馈：
 * 1) TTS 播报「紧急求助已启动」与状态提示
 * 2) 手电筒按节拍闪烁
 * 3) 系统警报铃声循环播放
 * 4) 蓝牙下行 SOS 通知陪跑员
 * 同步对外暴露紧急联系人列表与各路反馈状态，由 UI 渲染状态条。
 */
class RunnerSosViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val ttsService = TtsService.from(appContext)
    private val torchService = TorchService.from(appContext)
    private val alarmPlayer = AlarmPlayer.from(appContext)
    private val vibrationService = VibrationService.from(appContext)
    private val commandChannel = BluetoothModule.commandChannel(appContext)
    private val contactRepository = RepositoryProvider.emergencyContact(appContext)

    private val _torchActive = MutableStateFlow(false)
    private val _alarmActive = MutableStateFlow(false)
    private val _notifyDelivered = MutableStateFlow(false)
    private var triggered: Boolean = false

    // 手电筒闪烁是否启用
    val torchActive: StateFlow<Boolean> = _torchActive.asStateFlow()

    // 警报铃声是否启用
    val alarmActive: StateFlow<Boolean> = _alarmActive.asStateFlow()

    // 是否已向陪跑员下发 SOS 通知
    val notifyDelivered: StateFlow<Boolean> = _notifyDelivered.asStateFlow()

    // 紧急联系人列表（来自本地 Room，进入页面时异步拉取一次远端）
    val contacts: StateFlow<List<EmergencyContactEntity>> = contactRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { contactRepository.refreshFromRemote() }
    }

    // 触发求助并启动四路反馈；重复调用幂等
    fun trigger() {
        if (triggered) return
        triggered = true
        _notifyDelivered.value = commandChannel.send(GuideCommand.Sos)
        vibrationService.feedback(GuideCommand.Sos)
        ttsService.speak(
            "紧急求助已启动，正在通知陪跑员，警报已响起，手电筒闪烁中",
            TtsService.Mode.FLUSH
        )
        if (torchService.hasFlash()) {
            torchService.startBlink()
            _torchActive.value = true
        }
        alarmPlayer.start()
        _alarmActive.value = true
    }

    // 解除求助：停止 TTS、手电筒、警报声；保留是否已通知陪跑员的事实
    fun dismiss() {
        if (!triggered) return
        triggered = false
        ttsService.stop()
        ttsService.speak("已解除紧急求助", TtsService.Mode.FLUSH)
        torchService.stop()
        _torchActive.value = false
        alarmPlayer.stop()
        _alarmActive.value = false
    }

    override fun onCleared() {
        if (triggered) {
            triggered = false
            torchService.stop()
            alarmPlayer.stop()
            ttsService.stop()
        }
        super.onCleared()
    }
}
