package com.rungether.app.ui.summary

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.rungether.app.data.repository.RepositoryProvider
import com.rungether.app.databinding.ActivityGuideSummaryBinding
import com.rungether.app.ui.common.BaseActivity
import com.rungether.app.ui.guide.GuideMainActivity
import com.rungether.app.util.DateFormatter
import com.rungether.app.util.PaceFormatter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 陪跑员跑步结算页
 *
 * 自动加载主界面落库后的本次记录：距离/时长/平均配速摘要 + 自绘轨迹图；
 * 远端同步由 Repository 在保存时异步触发，本页只负责展示结果与返回主界面的导航。
 */
class GuideSummaryActivity : BaseActivity<ActivityGuideSummaryBinding>() {

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
    }

    private val repository by lazy { RepositoryProvider.runRecord(applicationContext) }

    override fun inflateBinding(inflater: LayoutInflater): ActivityGuideSummaryBinding =
        ActivityGuideSummaryBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.btnReturn.setOnClickListener { goBackToMain() }
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
        binding.tvDate.text = formatDateWithWeekday(startedAt)
        binding.tvDistance.text = String.format(Locale.CHINA, "%.2f", distanceM / 1_000.0)
        binding.tvDuration.text = DateFormatter.formatDuration(durationSec)
        binding.tvPace.text = PaceFormatter.formatForDisplay(averagePace)
        binding.tvSaveState.text = "记录已自动保存并上传"
        val points = parseTrack(trackJson)
        if (points.isNotEmpty()) {
            binding.trackView.setPoints(points)
        }
    }

    private fun renderEmpty() {
        binding.tvSaveState.text = "未取到跑步记录"
        binding.tvDistance.text = "0.00"
        binding.tvDuration.text = "00:00:00"
        binding.tvPace.text = "—'—''"
        binding.tvDate.text = formatDateWithWeekday(System.currentTimeMillis())
    }

    private fun parseTrack(json: String): List<DoubleArray> {
        if (json.isBlank()) return emptyList()
        val type = object : TypeToken<List<DoubleArray>>() {}.type
        return runCatching {
            Gson().fromJson<List<DoubleArray>>(json, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun formatDateWithWeekday(timestampMs: Long): String {
        val date = Date(timestampMs)
        val datePart = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(date)
        val weekdayPart = SimpleDateFormat("EEEE", Locale.CHINA).format(date)
        return "$datePart · $weekdayPart"
    }

    private fun goBackToMain() {
        val intent = Intent(this, GuideMainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }
}
