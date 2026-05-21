package com.rungether.app.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rungether.app.data.repository.RepositoryProvider
import com.rungether.app.databinding.ActivityGuideHistoryDetailBinding
import com.rungether.app.ui.common.BaseActivity
import com.rungether.app.util.DateFormatter
import com.rungether.app.util.PaceFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 陪跑员历史详情页
 *
 * 接收列表页传入的本地记录主键，从 Repository 读取完整数据。
 * 顶部展示日期与星期，中部以三栏卡片呈现距离、时长、平均配速，
 * 下方使用 TrackView 复原本次跑步轨迹（自绘，无街道底图）。
 */
class GuideHistoryDetailActivity : BaseActivity<ActivityGuideHistoryDetailBinding>() {

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
    }

    private val repository by lazy { RepositoryProvider.runRecord(applicationContext) }

    override fun inflateBinding(inflater: LayoutInflater): ActivityGuideHistoryDetailBinding =
        ActivityGuideHistoryDetailBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
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
        }
    }

    private fun renderRecord(
        startedAt: Long,
        durationSec: Long,
        distanceM: Double,
        averagePace: Double,
        trackJson: String
    ) {
        binding.tvDetailDate.text = formatDateWithWeekdayTime(startedAt)
        binding.tvDetailDistance.text =
            String.format(Locale.CHINA, "%.2f", distanceM / 1_000.0)
        binding.tvDetailDuration.text = formatDuration(durationSec)
        binding.tvDetailPace.text = PaceFormatter.formatForDisplay(averagePace)
        val points = parseTrack(trackJson)
        if (points.isNotEmpty()) {
            binding.trackView.setPoints(points)
        }
    }

    private fun renderEmpty() {
        binding.tvDetailDate.text = formatDateWithWeekdayTime(System.currentTimeMillis())
        binding.tvDetailDistance.text = "0.00"
        binding.tvDetailDuration.text = "00:00"
        binding.tvDetailPace.text = "—'—''"
        toast("未取到该条跑步记录")
    }

    private fun parseTrack(json: String): List<DoubleArray> {
        if (json.isBlank()) return emptyList()
        val type = object : TypeToken<List<DoubleArray>>() {}.type
        return runCatching {
            Gson().fromJson<List<DoubleArray>>(json, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    // 详情页日期格式：yyyy-MM-dd · 周X HH:mm
    private fun formatDateWithWeekdayTime(timestampMs: Long): String {
        val date = Date(timestampMs)
        val datePart = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(date)
        val weekdayPart = SimpleDateFormat("EEEE", Locale.CHINA).format(date)
        val timePart = SimpleDateFormat("HH:mm", Locale.CHINA).format(date)
        return "$datePart · $weekdayPart $timePart"
    }

    // 详情卡片显示用：超过 1 小时显示 H:MM:SS，否则 MM:SS
    private fun formatDuration(seconds: Long): String {
        val safe = if (seconds < 0) 0 else seconds
        return if (safe >= 3_600) {
            DateFormatter.formatDuration(safe)
        } else {
            String.format(Locale.CHINA, "%d:%02d", safe / 60, safe % 60)
        }
    }
}
