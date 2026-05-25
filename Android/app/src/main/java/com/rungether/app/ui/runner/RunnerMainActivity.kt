package com.rungether.app.ui.runner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rungether.app.R
import com.rungether.app.bluetooth.BluetoothModule
import com.rungether.app.bluetooth.connection.ConnectionState
import com.rungether.app.databinding.ActivityRunnerMainBinding
import com.rungether.app.service.sensor.ShakeDetector
import com.rungether.app.service.tts.TtsService
import com.rungether.app.ui.common.BaseActivity
import com.rungether.app.ui.history.RunnerHistoryListActivity
import com.rungether.app.ui.settings.RunnerSettingsActivity
import com.rungether.app.ui.sos.RunnerSosActivity
import com.rungether.app.ui.summary.RunnerSummaryActivity
import com.rungether.app.util.DateFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Locale

/**
 * 盲人端主界面
 *
 * 双态超大按钮：待开始展示「开始跑步」「紧急求助」；跑步中展示已跑时长、距离、
 * 「结束跑步」「紧急求助」。订阅 ViewModel 中的阶段、时长、距离与连接状态流；
 * 在 onResume 启动摇一摇监听，明显用力摇晃 2 秒触发紧急求助；
 * 点击「开始跑步」前请求定位权限；点击「结束跑步」跳转结算页并自动落库。
 */
class RunnerMainActivity : BaseActivity<ActivityRunnerMainBinding>() {

    private val viewModel: RunnerMainViewModel by viewModels()
    private val shakeDetector by lazy { ShakeDetector.from(applicationContext) }
    private val ttsService by lazy { TtsService.from(applicationContext) }
    private val connectionManager by lazy { BluetoothModule.connectionManager(applicationContext) }
    private var shakeJob: Job? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            viewModel.startRun()
        } else {
            ttsService.speak("缺少必要权限，无法开始跑步", TtsService.Mode.FLUSH)
            toast("缺少必要权限，无法开始跑步")
        }
    }

    // 申请蓝牙相关运行时权限（Android 12+ 的 CONNECT/ADVERTISE）
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            requestDiscoverableAndListen()
        } else {
            toast("缺少蓝牙权限，陪跑员将无法连接")
        }
    }

    // 申请系统可被发现窗口；resultCode 即用户授权的可见秒数，<=0 表示拒绝
    private val discoverableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode > 0) {
            connectionManager.startListening()
            toast("已开放陪跑员发现，正在等待连接")
        } else {
            toast("已拒绝蓝牙可被发现，陪跑员将无法连接")
        }
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityRunnerMainBinding =
        ActivityRunnerMainBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.btnStartRun.setOnClickListener { requestPermissionsAndStart() }
        binding.btnEndRun.setOnClickListener { finishRun() }
        binding.btnSosIdle.setOnClickListener { gotoSos() }
        binding.btnSosRunning.setOnClickListener { gotoSos() }
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, RunnerHistoryListActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, RunnerSettingsActivity::class.java))
        }
    }

    override fun initObserver() {
        super.initObserver()
        viewModel.phase.collectOnStarted { renderPhase(it) }
        viewModel.elapsedSec.collectOnStarted {
            binding.tvDuration.text = formatDurationShort(it)
        }
        viewModel.distanceM.collectOnStarted {
            binding.tvDistance.text = String.format(Locale.CHINA, "%.2f", it / 1_000.0)
        }
        viewModel.connectionState.collectOnStarted { renderConnection(it) }
    }

    override fun initData() {
        super.initData()
        ensureBluetoothListeningReady()
    }

    override fun onResume() {
        super.onResume()
        startShakeListening()
    }

    override fun onPause() {
        super.onPause()
        stopShakeListening()
    }

    override fun onDestroy() {
        connectionManager.stopListening()
        super.onDestroy()
    }

    // 已连接或正在连接时跳过；其它情形请求权限 → 可被发现 → 启动监听
    private fun ensureBluetoothListeningReady() {
        if (!connectionManager.isBluetoothSupported()) return
        when (connectionManager.state.value) {
            is ConnectionState.Connected, is ConnectionState.Connecting -> return
            else -> Unit
        }
        if (!connectionManager.isBluetoothEnabled()) {
            toast("请先开启蓝牙以便陪跑员发现")
            return
        }
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            needed += Manifest.permission.BLUETOOTH_CONNECT
            needed += Manifest.permission.BLUETOOTH_ADVERTISE
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            requestDiscoverableAndListen()
        } else {
            bluetoothPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun requestDiscoverableAndListen() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION_SEC)
        }
        discoverableLauncher.launch(intent)
    }

    private fun renderPhase(phase: RunnerRunPhase) {
        when (phase) {
            RunnerRunPhase.IDLE -> {
                binding.stateIdle.visibility = View.VISIBLE
                binding.stateRunning.visibility = View.GONE
            }
            RunnerRunPhase.RUNNING -> {
                binding.stateIdle.visibility = View.GONE
                binding.stateRunning.visibility = View.VISIBLE
            }
        }
    }

    private fun renderConnection(state: ConnectionState) {
        val connected = state is ConnectionState.Connected
        binding.chipConnection.setBackgroundResource(
            if (connected) R.drawable.bg_runner_panel_success
            else R.drawable.bg_runner_panel_warn
        )
        binding.dotConnection.setBackgroundResource(
            if (connected) R.drawable.bg_runner_dot_success
            else R.drawable.bg_runner_dot_warn
        )
        val colorRes = if (connected) R.color.runner_success else R.color.runner_accent
        binding.tvConnection.setTextColor(ContextCompat.getColor(this, colorRes))
        binding.tvConnection.text = if (connected) {
            getString(R.string.runner_status_connected)
        } else {
            getString(R.string.runner_status_disconnected)
        }
    }

    private fun requestPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            viewModel.startRun()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun finishRun() {
        viewModel.endRun { recordId ->
            val intent = Intent(this, RunnerSummaryActivity::class.java).apply {
                putExtra(RunnerSummaryActivity.EXTRA_RECORD_ID, recordId)
            }
            startActivity(intent)
        }
    }

    private fun gotoSos() {
        viewModel.triggerSos()
        startActivity(Intent(this, RunnerSosActivity::class.java))
    }

    private fun startShakeListening() {
        if (!shakeDetector.isSupported()) return
        shakeJob?.cancel()
        shakeJob = shakeDetector.start()
            .onEach { gotoSos() }
            .launchIn(lifecycleScope)
    }

    private fun stopShakeListening() {
        shakeJob?.cancel()
        shakeJob = null
    }

    // 跑步时长：超过 1 小时显示 HH:MM:SS，否则 MM:SS
    private fun formatDurationShort(seconds: Long): String {
        return if (seconds >= 3_600) {
            DateFormatter.formatDuration(seconds)
        } else {
            val mm = (seconds / 60).coerceAtLeast(0)
            val ss = (seconds % 60).coerceAtLeast(0)
            String.format(Locale.CHINA, "%02d:%02d", mm, ss)
        }
    }

    private companion object {
        const val DISCOVERABLE_DURATION_SEC: Int = 300
    }
}
