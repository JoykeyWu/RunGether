package com.rungether.app.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rungether.app.data.repository.RepositoryProvider
import com.rungether.app.databinding.ActivityRunnerHistoryDetailBinding
import com.rungether.app.service.tts.TtsService
import com.rungether.app.ui.common.BaseActivity
import com.rungether.app.util.DateFormatter
import com.rungether.app.util.DistanceFormatter
import com.rungether.app.util.PaceFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 盲人端历史详情页
 *
 * 接收列表页传入的本地记录主键，从 Repository 读取完整数据并以超大字号呈现。
 * 顶部展示日期，中部三组分割线分隔时长、距离、平均配速；下方使用 TrackView 复原轨迹。
 * 进入页面后立即用 TTS 完整播报本次跑步详情，与设计稿语音条同步。
 */
class RunnerHistoryDetailActivity : BaseActivity<ActivityRunnerHistoryDetailBinding>() {

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
    }

    private val repository by lazy { RepositoryProvider.runRecord(applicationContext) }
    private val ttsService by lazy { TtsService.from(applicationContext) }

    override fun inflateBinding(inflater: LayoutInflater): ActivityRunnerHistoryDetailBinding =
        ActivityRunnerHistoryDetailBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.trackView.applyDarkPalette()
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
                averagePace = record.averagePace,
                trackJson = record.trackJson
            )
            speakSummary(record.startedAt, record.durationSec, record.distanceM, record.averagePace)
        }
    }

    override fun onDestroy() {
        ttsService.stop()
        super.onDestroy()
    }

    private fun renderRecord(
        startedAt: Long,
        durationSec: Long,
        distanceM: Double,
        averagePace: Double,
        trackJson: String
    ) {
        binding.tvDetailDate.text =
            SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(startedAt))
        binding.tvDetailDuration.text = DateFormatter.formatDuration(durationSec)
        binding.tvDetailDistance.text =
            String.format(Locale.CHINA, "%.2f 公里", distanceM / 1_000.0)
        binding.tvDetailPace.text =
            "${PaceFormatter.formatForDisplay(averagePace)} / 公里"
        val points = parseTrack(trackJson)
        if (points.isNotEmpty()) {
            binding.trackView.setPoints(points)
        }
    }

    private fun renderEmpty() {
        binding.tvDetailDate.text =
            SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
        binding.tvDetailDuration.text = "00:00:00"
        binding.tvDetailDistance.text = "0.00 公里"
        binding.tvDetailPace.text = "—'—'' / 公里"
        ttsService.speak("未取到该条跑步记录", TtsService.Mode.FLUSH)
    }

    // 解析自绘轨迹 JSON
    private fun parseTrack(json: String): List<DoubleArray> {
        if (json.isBlank()) return emptyList()
        val type = object : TypeToken<List<DoubleArray>>() {}.type
        return runCatching {
            Gson().fromJson<List<DoubleArray>>(json, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    // 完整播报本次跑步详情
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
        val script = "跑步详情。日期，${dateText}。总时长，${durationText}。" +
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
}
