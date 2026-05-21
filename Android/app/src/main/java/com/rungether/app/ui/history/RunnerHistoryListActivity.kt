package com.rungether.app.ui.history

import com.rungether.app.R
import com.rungether.app.ui.common.NavItem
import com.rungether.app.ui.common.PlaceholderActivity

/**
 * 盲人端历史记录列表页占位符
 *
 * 真实实现倒序展示历史卡片，长按删除并支持点击进入详情，进入时 TTS 播报总数。
 */
class RunnerHistoryListActivity : PlaceholderActivity() {
    override fun placeholderNavItems(): List<NavItem> = listOf(
        NavItem(
            title = "查看示例历史详情",
            target = RunnerHistoryDetailActivity::class.java,
            backgroundRes = R.drawable.bg_runner_card,
            textColorRes = R.color.runner_high
        )
    )
}
