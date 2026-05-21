package com.rungether.app.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rungether.app.data.local.entity.RunRecordEntity
import com.rungether.app.data.repository.RepositoryProvider
import com.rungether.app.databinding.ActivityRunnerHistoryListBinding
import com.rungether.app.service.tts.TtsService
import com.rungether.app.ui.common.BaseActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 盲人端历史记录列表页
 *
 * 黑底超大字号还原视障用户友好风格；订阅 Repository 暴露的本地 Flow，
 * 进入页面时触发一次远端拉取并由 Flow 自动驱动 UI 更新。
 * 每次列表数据到位都通过 TTS 完整播报总数。
 * 点击列表项进入详情；长按弹出居中确认弹窗，确认后同步删除本地与远端。
 */
class RunnerHistoryListActivity : BaseActivity<ActivityRunnerHistoryListBinding>() {

    private val repository by lazy { RepositoryProvider.runRecord(applicationContext) }
    private val ttsService by lazy { TtsService.from(applicationContext) }

    private val adapter = RunnerHistoryAdapter(
        onItemClick = { openDetail(it) },
        onItemLongClick = { showDeleteDialog(it) }
    )

    private var pendingDelete: RunRecordEntity? = null
    private var lastSpokenCount: Int = Int.MIN_VALUE

    override fun inflateBinding(inflater: LayoutInflater): ActivityRunnerHistoryListBinding =
        ActivityRunnerHistoryListBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
        binding.deleteDialogRoot.setOnClickListener { hideDeleteDialog() }
        binding.btnDeleteCancel.setOnClickListener { hideDeleteDialog() }
        binding.btnDeleteConfirm.setOnClickListener { confirmDelete() }
    }

    override fun initObserver() {
        super.initObserver()
        repository.observeAll().collectOnStarted { records ->
            adapter.submitList(records)
            renderHeader(records.size)
            announceCount(records.size)
        }
    }

    override fun initData() {
        super.initData()
        lifecycleScope.launch { repository.refreshFromRemote() }
    }

    override fun onDestroy() {
        ttsService.stop()
        super.onDestroy()
    }

    // 顶部语音条与空态切换
    private fun renderHeader(count: Int) {
        if (count <= 0) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
            binding.tvLongPressHint.visibility = View.GONE
            binding.tvSpeechChip.text = "暂无跑步记录"
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvHistory.visibility = View.VISIBLE
            binding.tvLongPressHint.visibility = View.VISIBLE
            binding.tvSpeechChip.text = getString(
                com.rungether.app.R.string.runner_history_speech_prefix
            ) + " $count " + getString(
                com.rungether.app.R.string.runner_history_speech_suffix
            )
        }
    }

    // 数量变化时触发 TTS 播报，避免短时间重复播报
    private fun announceCount(count: Int) {
        if (count == lastSpokenCount) return
        lastSpokenCount = count
        val script = if (count <= 0) {
            "暂无跑步记录"
        } else {
            "历史记录共 $count 条，长按记录可删除"
        }
        ttsService.speak(script, TtsService.Mode.FLUSH)
    }

    private fun openDetail(record: RunRecordEntity) {
        val intent = Intent(this, RunnerHistoryDetailActivity::class.java).apply {
            putExtra(RunnerHistoryDetailActivity.EXTRA_RECORD_ID, record.id)
        }
        startActivity(intent)
    }

    // 显示居中删除确认弹窗
    private fun showDeleteDialog(record: RunRecordEntity) {
        pendingDelete = record
        val dateText = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(record.startedAt))
        binding.tvDeleteSubtitle.text = "确认删除 $dateText\n的跑步记录？"
        binding.deleteDialogRoot.visibility = View.VISIBLE
        ttsService.speak("确认删除 $dateText 的跑步记录？", TtsService.Mode.FLUSH)
    }

    // 关闭删除确认弹窗
    private fun hideDeleteDialog() {
        binding.deleteDialogRoot.visibility = View.GONE
        pendingDelete = null
    }

    // 执行删除：先删本地立即更新 UI，再异步同步远端
    private fun confirmDelete() {
        val target = pendingDelete ?: run {
            hideDeleteDialog()
            return
        }
        hideDeleteDialog()
        lifecycleScope.launch {
            repository.delete(target)
            ttsService.speak("已删除该条记录", TtsService.Mode.FLUSH)
            toast("已删除该条记录")
        }
    }
}
