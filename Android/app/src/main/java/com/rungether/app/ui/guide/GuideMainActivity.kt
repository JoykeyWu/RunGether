package com.rungether.app.ui.guide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.rungether.app.bluetooth.connection.ConnectionState
import com.rungether.app.bluetooth.protocol.ShortcutType
import com.rungether.app.databinding.ActivityGuideMainBinding
import com.rungether.app.ui.bluetooth.BluetoothPairActivity
import com.rungether.app.ui.common.BaseActivity
import com.rungether.app.ui.history.GuideHistoryListActivity
import com.rungether.app.ui.settings.GuideSettingsActivity
import com.rungether.app.ui.summary.GuideSummaryActivity
import com.rungether.app.util.DateFormatter
import com.rungether.app.util.PaceFormatter
import java.util.Locale

/**
 * 陪跑员引导主界面
 *
 * 待开始与跑步中双态切换；待开始时展示连接提示与「开始陪跑」按钮，
 * 跑步中实时刷新配速、距离、时长，自绘轨迹随位置移动，方向摇杆与障碍/减速/停止
 * 快捷按钮触发指令下行；收到盲人端 SOS 时弹出模态提醒。
 *
 * 业务路径严格走 ViewModel + Repository + BluetoothModule，
 * 本类只负责视图绑定与生命周期。
 */
class GuideMainActivity : BaseActivity<ActivityGuideMainBinding>() {

    private val viewModel: GuideMainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            viewModel.startRun()
        } else {
            toast("缺少必要权限，无法开始陪跑")
        }
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityGuideMainBinding =
        ActivityGuideMainBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, GuideHistoryListActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, GuideSettingsActivity::class.java))
        }
        binding.btnOpenPair.setOnClickListener {
            startActivity(Intent(this, BluetoothPairActivity::class.java))
        }
        binding.btnStartRun.setOnClickListener { requestPermissionsAndStart() }
        binding.btnEndRun.setOnClickListener { finishRun() }

        binding.joystick.onDirectionChanged = { type, angle ->
            viewModel.onDirectionChanged(type, angle)
        }
        binding.btnShortcutObstacle.setOnClickListener {
            viewModel.sendShortcut(ShortcutType.OBSTACLE)
        }
        binding.btnShortcutSlow.setOnClickListener {
            viewModel.sendShortcut(ShortcutType.SLOW_DOWN)
        }
        binding.btnShortcutStop.setOnClickListener {
            viewModel.sendShortcut(ShortcutType.STOP)
        }
        binding.btnSosAck.setOnClickListener {
            binding.sosModal.visibility = View.GONE
            viewModel.consumeSos()
        }
    }

    override fun initObserver() {
        super.initObserver()
        viewModel.phase.collectOnStarted { phase -> renderPhase(phase) }
        viewModel.elapsedSec.collectOnStarted { seconds ->
            binding.tvDuration.text = formatDurationShort(seconds)
        }
        viewModel.distanceM.collectOnStarted { meters ->
            binding.tvDistance.text = String.format(Locale.CHINA, "%.2f", meters / 1_000.0)
        }
        viewModel.paceMinPerKm.collectOnStarted { pace ->
            binding.tvPace.text = PaceFormatter.formatForDisplay(pace)
        }
        viewModel.trackPoints.collectOnStarted { points ->
            binding.trackView.setPoints(points)
        }
        viewModel.currentDirection.collectOnStarted { hint ->
            binding.tvDirectionHint.text = hint
        }
        viewModel.connectionState.collectOnStarted { state -> renderConnection(state) }
        viewModel.sosReceived.collectOnStarted { ts ->
            if (ts > 0L) binding.sosModal.visibility = View.VISIBLE
        }
    }

    private fun renderPhase(phase: GuideRunPhase) {
        when (phase) {
            GuideRunPhase.IDLE -> {
                binding.stateIdle.visibility = View.VISIBLE
                binding.stateRunning.visibility = View.GONE
            }
            GuideRunPhase.RUNNING -> {
                binding.stateIdle.visibility = View.GONE
                binding.stateRunning.visibility = View.VISIBLE
            }
        }
    }

    private fun renderConnection(state: ConnectionState) {
        val context = this
        when (state) {
            is ConnectionState.Connected -> {
                val name = state.deviceName ?: "盲人端设备"
                binding.tvConnection.text = "盲人端已连接：$name"
                binding.tvConnection.setTextColor(ContextCompat.getColor(context, com.rungether.app.R.color.guide_success))
                binding.dotConnection.setBackgroundResource(com.rungether.app.R.drawable.bg_dot_success)
                binding.chipConnection.setBackgroundResource(com.rungether.app.R.drawable.bg_status_chip)
            }
            is ConnectionState.Connecting -> {
                binding.tvConnection.text = "正在连接：${state.deviceName ?: state.deviceAddress}"
                binding.tvConnection.setTextColor(ContextCompat.getColor(context, com.rungether.app.R.color.guide_warning))
                binding.dotConnection.setBackgroundResource(com.rungether.app.R.drawable.bg_dot_warn)
                binding.chipConnection.setBackgroundResource(com.rungether.app.R.drawable.bg_status_chip_warn)
            }
            is ConnectionState.Disconnected, is ConnectionState.Error, ConnectionState.Idle, ConnectionState.Scanning -> {
                binding.tvConnection.text = "未连接盲人端"
                binding.tvConnection.setTextColor(ContextCompat.getColor(context, com.rungether.app.R.color.guide_navy_70))
                binding.dotConnection.setBackgroundResource(com.rungether.app.R.drawable.bg_dot_warn)
                binding.chipConnection.setBackgroundResource(com.rungether.app.R.drawable.bg_status_chip_warn)
            }
        }
    }

    // 陪跑端定位由盲人端 Telemetry 推送驱动，本机仅需蓝牙权限
    private fun requestPermissionsAndStart() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_CONNECT
            permissions += Manifest.permission.BLUETOOTH_SCAN
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            viewModel.startRun()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun finishRun() {
        viewModel.endRun { recordId ->
            val intent = Intent(this, GuideSummaryActivity::class.java).apply {
                putExtra(GuideSummaryActivity.EXTRA_RECORD_ID, recordId)
            }
            startActivity(intent)
        }
    }

    // 跑步时长展示：超过 1 小时显示 HH:MM:SS，否则 MM:SS
    private fun formatDurationShort(seconds: Long): String {
        return if (seconds >= 3_600) {
            DateFormatter.formatDuration(seconds)
        } else {
            val mm = (seconds / 60).coerceAtLeast(0)
            val ss = (seconds % 60).coerceAtLeast(0)
            String.format(Locale.CHINA, "%02d:%02d", mm, ss)
        }
    }
}
