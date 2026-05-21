package com.rungether.app.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import com.rungether.app.databinding.ActivityRunnerAboutBinding
import com.rungether.app.ui.common.BaseActivity
import com.rungether.app.util.VersionInfo

/**
 * 盲人端关于页
 *
 * 静态展示 Logo、副标题、版本徽章、App 简介、开发团队与版权信息；
 * 版本号通过 PackageManager 读取，与构建配置保持一致。
 */
class RunnerAboutActivity : BaseActivity<ActivityRunnerAboutBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivityRunnerAboutBinding =
        ActivityRunnerAboutBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.tvAboutVersion.text = "v${VersionInfo.versionName(this)}"
    }
}
