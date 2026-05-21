package com.rungether.app.ui.guide

import com.rungether.app.R
import com.rungether.app.ui.about.GuideAboutActivity
import com.rungether.app.ui.bluetooth.BluetoothPairActivity
import com.rungether.app.ui.common.NavItem
import com.rungether.app.ui.common.PlaceholderActivity
import com.rungether.app.ui.history.GuideHistoryListActivity
import com.rungether.app.ui.settings.GuideSettingsActivity
import com.rungether.app.ui.summary.GuideSummaryActivity

/**
 * 陪跑员引导主界面占位符
 *
 * 真实实现包含待开始/跑步中双态、实时数据面板、方向摇杆、轨迹图等，
 * 此处仅提供可路由的入口按钮，便于联调阶段访问下游页面。
 */
class GuideMainActivity : PlaceholderActivity() {
    override fun placeholderNavItems(): List<NavItem> = listOf(
        NavItem("蓝牙配对", BluetoothPairActivity::class.java, R.drawable.bg_guide_card, R.color.guide_navy),
        NavItem("历史记录", GuideHistoryListActivity::class.java, R.drawable.bg_guide_card, R.color.guide_navy),
        NavItem("跑步结算（演示）", GuideSummaryActivity::class.java, R.drawable.bg_guide_card, R.color.guide_navy),
        NavItem("设置", GuideSettingsActivity::class.java, R.drawable.bg_guide_card, R.color.guide_navy),
        NavItem("关于", GuideAboutActivity::class.java, R.drawable.bg_guide_card, R.color.guide_navy)
    )
}
