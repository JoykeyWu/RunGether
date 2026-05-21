package com.rungether.app.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rungether.app.data.local.entity.RunRecordEntity
import com.rungether.app.databinding.ItemRunnerHistoryBinding
import com.rungether.app.util.PaceFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 盲人端历史记录列表 Adapter
 *
 * 列表项以超大字号、黑底白边复刻设计稿；右上角根据起跑时间显示「今天」徽章或相对天数。
 * 点击进入详情，长按触发删除确认；DiffUtil 保证数据变更后局部刷新。
 */
class RunnerHistoryAdapter(
    private val onItemClick: (RunRecordEntity) -> Unit,
    private val onItemLongClick: (RunRecordEntity) -> Unit
) : ListAdapter<RunRecordEntity, RunnerHistoryAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRunnerHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        val params = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = (parent.context.resources.displayMetrics.density * 12).toInt()
        }
        binding.root.layoutParams = params
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemRunnerHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // 绑定一条跑步记录到列表卡片
        fun bind(record: RunRecordEntity) {
            binding.tvItemDate.text = DATE_FORMAT.format(Date(record.startedAt))
            binding.tvItemDistance.text =
                String.format(Locale.CHINA, "%.2f", record.distanceM / 1_000.0)
            binding.tvItemDuration.text = formatDurationShort(record.durationSec)
            binding.tvItemPace.text = PaceFormatter.formatForDisplay(record.averagePace)
            val isToday = isSameDayAsToday(record.startedAt)
            if (isToday) {
                binding.tvItemChip.visibility = View.VISIBLE
                binding.tvItemChip.text = "今天"
                binding.tvItemRelative.visibility = View.GONE
            } else {
                binding.tvItemChip.visibility = View.GONE
                binding.tvItemRelative.visibility = View.VISIBLE
                binding.tvItemRelative.text = formatRelativeDay(record.startedAt)
            }
            binding.root.setOnClickListener { onItemClick(record) }
            binding.root.setOnLongClickListener {
                onItemLongClick(record)
                true
            }
        }
    }

    // 时长格式：≥1 小时显示 H:MM:SS，否则 MM:SS
    private fun formatDurationShort(seconds: Long): String {
        val safe = if (seconds < 0) 0 else seconds
        return if (safe >= 3_600) {
            String.format(
                Locale.CHINA, "%d:%02d:%02d",
                safe / 3_600, (safe % 3_600) / 60, safe % 60
            )
        } else {
            String.format(Locale.CHINA, "%d:%02d", safe / 60, safe % 60)
        }
    }

    // 是否与今天同一天
    private fun isSameDayAsToday(timestampMs: Long): Boolean {
        return dayStartMillis(System.currentTimeMillis()) == dayStartMillis(timestampMs)
    }

    // 相对日期：昨天 / N 天前 / 月份
    private fun formatRelativeDay(timestampMs: Long): String {
        val today = dayStartMillis(System.currentTimeMillis())
        val itemDay = dayStartMillis(timestampMs)
        val diffDays = TimeUnit.MILLISECONDS.toDays(today - itemDay)
        return when {
            diffDays == 1L -> "昨天"
            diffDays in 2..30 -> "$diffDays 天前"
            else -> SimpleDateFormat("yyyy-MM", Locale.CHINA).format(Date(timestampMs))
        }
    }

    private fun dayStartMillis(timestampMs: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestampMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

        private val DIFF = object : DiffUtil.ItemCallback<RunRecordEntity>() {
            override fun areItemsTheSame(old: RunRecordEntity, new: RunRecordEntity): Boolean =
                old.id == new.id

            override fun areContentsTheSame(old: RunRecordEntity, new: RunRecordEntity): Boolean =
                old == new
        }
    }
}
