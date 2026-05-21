package com.rungether.app.ui.sos

import com.rungether.app.R
import com.rungether.app.ui.common.NavItem
import com.rungether.app.ui.common.PlaceholderActivity

/**
 * 盲人端紧急求助界面占位符
 *
 * 真实实现包含 TTS 播报、手电筒闪烁、警报声、蓝牙下行 SOS 与紧急联系人列表。
 */
class RunnerSosActivity : PlaceholderActivity() {
    override fun placeholderNavItems(): List<NavItem> = listOf(
        NavItem(
            title = "模拟呼叫紧急联系人",
            target = RunnerCallActivity::class.java,
            backgroundRes = R.drawable.bg_runner_card,
            textColorRes = R.color.runner_high
        )
    )
}
