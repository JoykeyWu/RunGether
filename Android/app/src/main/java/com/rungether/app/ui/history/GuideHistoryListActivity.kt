package com.rungether.app.ui.history

import com.rungether.app.R
import com.rungether.app.ui.common.NavItem
import com.rungether.app.ui.common.PlaceholderActivity

/**
 * 陪跑员历史记录列表页占位符
 *
 * 真实实现按时间倒序展示历史卡片，并支持长按删除与点击进入详情。
 */
class GuideHistoryListActivity : PlaceholderActivity() {
    override fun placeholderNavItems(): List<NavItem> = listOf(
        NavItem(
            title = "查看示例历史详情",
            target = GuideHistoryDetailActivity::class.java,
            backgroundRes = R.drawable.bg_guide_card,
            textColorRes = R.color.guide_navy
        )
    )
}
