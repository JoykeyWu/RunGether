package com.rungether.app.ui.runner

import com.rungether.app.R
import com.rungether.app.ui.about.RunnerAboutActivity
import com.rungether.app.ui.common.NavItem
import com.rungether.app.ui.common.PlaceholderActivity
import com.rungether.app.ui.history.RunnerHistoryListActivity
import com.rungether.app.ui.settings.RunnerSettingsActivity
import com.rungether.app.ui.sos.RunnerSosActivity
import com.rungether.app.ui.summary.RunnerSummaryActivity

/**
 * 盲人端主界面占位符
 *
 * 真实实现包含开始/结束跑步超大按钮、紧急求助入口、TTS 与震动反馈联动。
 */
class RunnerMainActivity : PlaceholderActivity() {
    override fun placeholderNavItems(): List<NavItem> = listOf(
        NavItem("紧急求助", RunnerSosActivity::class.java, R.drawable.bg_runner_card, R.color.runner_high),
        NavItem("历史记录", RunnerHistoryListActivity::class.java, R.drawable.bg_runner_card, R.color.runner_high),
        NavItem("跑步结算（演示）", RunnerSummaryActivity::class.java, R.drawable.bg_runner_card, R.color.runner_high),
        NavItem("设置", RunnerSettingsActivity::class.java, R.drawable.bg_runner_card, R.color.runner_high),
        NavItem("关于", RunnerAboutActivity::class.java, R.drawable.bg_runner_card, R.color.runner_high)
    )
}
