package com.rungether.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rungether.app.data.prefs.UserPreferences
import com.rungether.app.data.prefs.UserRole
import com.rungether.app.databinding.ActivityMainBinding
import com.rungether.app.ui.guide.GuideMainActivity
import com.rungether.app.ui.role.RoleSelectActivity
import com.rungether.app.ui.runner.RunnerMainActivity

// 应用启动入口
// 根据偏好存储中持久化的角色，将用户分流到角色选择页或对应主界面
// 当前角色为未选择时进入 RoleSelectActivity，已选择则直接跳到对应端的主界面
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        routeByRole()
    }

    // 读取偏好中的角色并跳转到对应入口
    private fun routeByRole() {
        val nextIntent = when (UserPreferences.from(this).currentRole) {
            UserRole.GUIDE -> Intent(this, GuideMainActivity::class.java)
            UserRole.RUNNER -> Intent(this, RunnerMainActivity::class.java)
            UserRole.UNSELECTED -> Intent(this, RoleSelectActivity::class.java)
        }
        nextIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(nextIntent)
        finish()
    }
}
