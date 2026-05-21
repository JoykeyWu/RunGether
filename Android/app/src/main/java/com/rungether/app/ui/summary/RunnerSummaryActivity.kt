package com.rungether.app.ui.summary

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.rungether.app.data.repository.RepositoryProvider
import com.rungether.app.databinding.ActivityRunnerSummaryBinding
import com.rungether.app.service.tts.TtsService
import com.rungether.app.ui.common.BaseActivity
import com.rungether.app.ui.runner.RunnerMainActivity
import com.rungether.app.util.DateFormatter
import com.rungether.app.util.DistanceFormatter
import com.rungether.app.util.PaceFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 盲人端跑步结算页
 *
 * 接收主界面落库后的本次记录主键，从 Repository 读取展示日期、总时长、总距离、平均配速；
 * 进入即触发 TTS 完整播报本次跑步成绩；单一返回按钮跳回主界面。
 */
class RunnerSummaryActivity : BaseActivity<ActivityRunnerSummaryBinding>() {

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
    }

    private val repository by lazy { RepositoryProvider.runRecord(applicationContext) }
    private val ttsService by lazy { TtsService.from(applicationContext) }

    override fun inflateBinding(inflater: LayoutInflater): ActivityRunnerSummaryBinding =
        ActivityRunnerSummaryBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.btnBackMain.setOnClickListener { goBackToMain() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBackToMain()
            }
        })
    }

    override fun initData() {
        super.initData()
        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
        if (recordId <= 0L) {
            renderEmpty()
            return
        }
        lifecycleScope.launch {
            val record = repository.findById(recordId)
            if (record == null) {
                renderEmpty()
                return@launch
            }
            renderRecord(
                startedAt = record.startedAt,
                durationSec = record.durationSec,
                distanceM = record.distanceM,
                averagePace = record.averagePace
            )
            speakSummary(record.startedAt, record.durationSec, record.distanceM, record.averagePace)
        }
    }

    private fun renderRecord(
        startedAt: Long,
        durationSec: Long,
        distanceM: Double,
        averagePace: Double
    ) {
        binding.tvSummaryDate.text =
            SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(startedAt))
        binding.tvSummaryDuration.text = DateFormatter.formatDuration(durationSec)
        binding.tvSummaryDistance.text =
            String.format(Locale.CHINA, "%.2f 公里", distanceM / 1_000.0)
        binding.tvSummaryPace.text =
            "${PaceFormatter.formatForDisplay(averagePace)} / 公里"
    }

    private fun renderEmpty() {
        binding.tvSummaryDate.text =
            SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
        binding.tvSummaryDuration.text = "00:00:00"
        binding.tvSummaryDistance.text = "0.00 公里"
        binding.tvSummaryPace.text = "—'—'' / 公里"
        binding.tvSpeechState.text = getString(com.rungether.app.R.string.runner_summary_speech)
        ttsService.speak("未取到本次跑步记录", TtsService.Mode.FLUSH)
    }

    private fun speakSummary(
        startedAt: Long,
        durationSec: Long,
        distanceM: Double,
        averagePace: Double
    ) {
        val dateText = DateFormatter.formatForSpeech(startedAt)
        val durationText = formatDurationForSpeech(durationSec)
        val distanceText = DistanceFormatter.formatForSpeech(distanceM)
        val paceText = PaceFormatter.formatForSpeech(averagePace)
        val script = "本次跑步结束。日期，${dateText}。总时长，${durationText}。" +
            "总距离，${distanceText}。平均配速，${paceText}。"
        ttsService.speak(script, TtsService.Mode.FLUSH)
    }

    // 时长语音化：HH 小时 mm 分 ss 秒；零位省略
    private fun formatDurationForSpeech(seconds: Long): String {
        val safe = if (seconds < 0) 0 else seconds
        val hours = safe / 3_600
        val minutes = (safe % 3_600) / 60
        val secs = safe % 60
        return buildString {
            if (hours > 0) append("$hours 小时")
            if (minutes > 0) append("$minutes 分")
            if (secs > 0 || (hours == 0L && minutes == 0L)) append("$secs 秒")
        }
    }

    override fun onDestroy() {
        ttsService.stop()
        super.onDestroy()
    }

    private fun goBackToMain() {
        val intent = Intent(this, RunnerMainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }
}
