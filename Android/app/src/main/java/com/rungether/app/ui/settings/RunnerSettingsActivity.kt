package com.rungether.app.ui.settings

import com.rungether.app.R
import com.rungether.app.ui.about.RunnerAboutActivity
import com.rungether.app.ui.common.NavItem
import com.rungether.app.ui.common.PlaceholderActivity
import com.rungether.app.ui.contacts.RunnerContactsActivity
import com.rungether.app.ui.role.RoleSelectActivity

/**
 * 盲人端设置页占位符
 *
 * 真实实现包含角色卡片、语音速度三档、震动开关、紧急联系人、关于与版本号。
 */
class RunnerSettingsActivity : PlaceholderActivity() {
    override fun placeholderNavItems(): List<NavItem> = listOf(
        NavItem("紧急联系人", RunnerContactsActivity::class.java, R.drawable.bg_runner_card, R.color.runner_high),
        NavItem("关于", RunnerAboutActivity::class.java, R.drawable.bg_runner_card, R.color.runner_high),
        NavItem("切换角色", RoleSelectActivity::class.java, R.drawable.bg_runner_card, R.color.runner_high)
    )
}
