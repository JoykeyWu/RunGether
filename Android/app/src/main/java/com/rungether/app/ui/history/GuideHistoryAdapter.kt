package com.rungether.app.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rungether.app.data.local.entity.RunRecordEntity
import com.rungether.app.databinding.ItemGuideHistoryBinding
import com.rungether.app.util.PaceFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 陪跑员历史记录列表 Adapter
 *
 * 使用 ListAdapter + DiffUtil 与 RecyclerView 配合，保证数据库变更后局部刷新而非整页重绘。
 * 列表项展示日期、距离、时长、配速；右上角依据起跑时间相对今天计算「今天 / N 天前」。
 * 点击事件用于跳转详情，长按事件用于触发删除确认抽屉。
 */
class GuideHistoryAdapter(
    private val onItemClick: (RunRecordEntity) -> Unit,
    private val onItemLongClick: (RunRecordEntity) -> Unit
) : ListAdapter<RunRecordEntity, GuideHistoryAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGuideHistoryBinding.inflate(
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
        private val binding: ItemGuideHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // 绑定一条跑步记录到列表卡片
        fun bind(record: RunRecordEntity) {
            binding.tvItemDate.text = DATE_FORMAT.format(Date(record.startedAt))
            binding.tvItemDistance.text = String.format(Locale.CHINA, "%.2f", record.distanceM / 1_000.0)
            binding.tvItemDuration.text = formatDurationShort(record.durationSec)
            binding.tvItemPace.text = PaceFormatter.formatForDisplay(record.averagePace)
            binding.tvItemChip.text = formatRelativeDay(record.startedAt)
            binding.root.setOnClickListener { onItemClick(record) }
            binding.root.setOnLongClickListener {
                onItemLongClick(record)
                true
            }
        }
    }

    // 列表展示用：超过 1 小时显示 HH:MM:SS，否则 MM:SS
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

    // 相对日期：今天 / N 天前
    private fun formatRelativeDay(timestampMs: Long): String {
        val today = todayStartMillis()
        val itemDay = dayStartMillis(timestampMs)
        val diffDays = TimeUnit.MILLISECONDS.toDays(today - itemDay)
        return when {
            diffDays <= 0L -> "今天"
            diffDays == 1L -> "昨天"
            diffDays in 2..30 -> "$diffDays 天前"
            else -> SimpleDateFormat("yyyy-MM", Locale.CHINA).format(Date(timestampMs))
        }
    }

    private fun todayStartMillis(): Long = dayStartMillis(System.currentTimeMillis())

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
