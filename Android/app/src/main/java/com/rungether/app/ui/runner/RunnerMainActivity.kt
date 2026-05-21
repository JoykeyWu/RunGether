package com.rungether.app.ui.runner

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rungether.app.R
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

    override fun onResume() {
        super.onResume()
        startShakeListening()
    }

    override fun onPause() {
        super.onPause()
        stopShakeListening()
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
}
