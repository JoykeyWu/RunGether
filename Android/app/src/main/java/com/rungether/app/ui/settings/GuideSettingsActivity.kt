package com.rungether.app.ui.settings

import com.rungether.app.R
import com.rungether.app.ui.about.GuideAboutActivity
import com.rungether.app.ui.bluetooth.BluetoothPairActivity
import com.rungether.app.ui.common.NavItem
import com.rungether.app.ui.common.PlaceholderActivity
import com.rungether.app.ui.contacts.GuideContactsActivity
import com.rungether.app.ui.role.RoleSelectActivity

/**
 * 陪跑员设置页占位符
 *
 * 真实实现包含角色切换、语音速度三档、震动开关、紧急联系人、蓝牙配对、关于与版本号。
 */
class GuideSettingsActivity : PlaceholderActivity() {
    override fun placeholderNavItems(): List<NavItem> = listOf(
        NavItem("紧急联系人", GuideContactsActivity::class.java, R.drawable.bg_guide_card, R.color.guide_navy),
        NavItem("蓝牙配对", BluetoothPairActivity::class.java, R.drawable.bg_guide_card, R.color.guide_navy),
        NavItem("关于", GuideAboutActivity::class.java, R.drawable.bg_guide_card, R.color.guide_navy),
        NavItem("切换角色", RoleSelectActivity::class.java, R.drawable.bg_guide_card, R.color.guide_navy)
    )
}
