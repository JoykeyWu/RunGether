package com.rungether.app.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rungether.app.data.local.entity.RunRecordEntity
import com.rungether.app.data.repository.RepositoryProvider
import com.rungether.app.databinding.ActivityGuideHistoryListBinding
import com.rungether.app.ui.common.BaseActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 陪跑员历史记录列表页
 *
 * 直接订阅 Repository 暴露的本地数据库 Flow，按起跑时间倒序展示卡片；
 * 进入页面时触发一次远端拉取，远端记录回写本地后由 Flow 自动驱动 UI 刷新。
 * 点击列表项进入历史详情；长按弹出底部抽屉确认删除，确认后同步删除本地与远端。
 */
class GuideHistoryListActivity : BaseActivity<ActivityGuideHistoryListBinding>() {

    private val repository by lazy { RepositoryProvider.runRecord(applicationContext) }

    private val adapter = GuideHistoryAdapter(
        onItemClick = { openDetail(it) },
        onItemLongClick = { showDeleteSheet(it) }
    )

    private var pendingDeleteTarget: RunRecordEntity? = null

    override fun inflateBinding(inflater: LayoutInflater): ActivityGuideHistoryListBinding =
        ActivityGuideHistoryListBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
        binding.deleteSheetRoot.setOnClickListener { hideDeleteSheet() }
        binding.btnDeleteCancel.setOnClickListener { hideDeleteSheet() }
        binding.btnDeleteConfirm.setOnClickListener { confirmDelete() }
    }

    override fun initObserver() {
        super.initObserver()
        repository.observeAll().collectOnStarted { records ->
            adapter.submitList(records)
            val empty = records.isEmpty()
            binding.emptyState.visibility = if (empty) View.VISIBLE else View.GONE
            binding.rvHistory.visibility = if (empty) View.GONE else View.VISIBLE
        }
    }

    override fun initData() {
        super.initData()
        lifecycleScope.launch { repository.refreshFromRemote() }
    }

    // 点击列表项跳转详情页
    private fun openDetail(record: RunRecordEntity) {
        val intent = Intent(this, GuideHistoryDetailActivity::class.java).apply {
            putExtra(GuideHistoryDetailActivity.EXTRA_RECORD_ID, record.id)
        }
        startActivity(intent)
    }

    // 长按列表项弹出底部抽屉确认删除
    private fun showDeleteSheet(record: RunRecordEntity) {
        pendingDeleteTarget = record
        val dateText = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(record.startedAt))
        binding.tvDeleteSubtitle.text = "确认删除 $dateText 的跑步记录？删除后无法恢复。"
        binding.deleteSheetRoot.visibility = View.VISIBLE
    }

    // 收起删除确认抽屉
    private fun hideDeleteSheet() {
        binding.deleteSheetRoot.visibility = View.GONE
        pendingDeleteTarget = null
    }

    // 执行删除：本地立即落库，再异步同步到远端
    private fun confirmDelete() {
        val target = pendingDeleteTarget ?: run {
            hideDeleteSheet()
            return
        }
        hideDeleteSheet()
        lifecycleScope.launch {
            repository.delete(target)
            toast("已删除该条记录")
        }
    }
}
