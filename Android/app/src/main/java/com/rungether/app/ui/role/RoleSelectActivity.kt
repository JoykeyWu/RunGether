package com.rungether.app.ui.role

import android.content.Intent
import android.view.LayoutInflater
import com.rungether.app.data.prefs.UserPreferences
import com.rungether.app.data.prefs.UserRole
import com.rungether.app.databinding.ActivityRoleSelectBinding
import com.rungether.app.ui.common.BaseActivity
import com.rungether.app.ui.guide.GuideMainActivity
import com.rungether.app.ui.runner.RunnerMainActivity

/**
 * 角色选择页
 *
 * 双角色卡片入口：选择后写入偏好并跳转对应主界面。
 * 首次启动强制进入本页，设置页的角色切换也复用本入口。
 */
class RoleSelectActivity : BaseActivity<ActivityRoleSelectBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivityRoleSelectBinding =
        ActivityRoleSelectBinding.inflate(inflater)

    override fun initView(savedInstanceState: android.os.Bundle?) {
        super.initView(savedInstanceState)
        binding.btnRoleRunner.setOnClickListener { selectRole(UserRole.RUNNER) }
        binding.btnRoleGuide.setOnClickListener { selectRole(UserRole.GUIDE) }
    }

    // 写入角色偏好并跳转到对应主界面
    private fun selectRole(role: UserRole) {
        UserPreferences.from(this).currentRole = role
        val target = when (role) {
            UserRole.GUIDE -> GuideMainActivity::class.java
            UserRole.RUNNER -> RunnerMainActivity::class.java
            UserRole.UNSELECTED -> return
        }
        startActivity(
            Intent(this, target).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
        finish()
    }
}
