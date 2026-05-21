package com.rungether.app.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import com.rungether.app.databinding.ActivityGuideAboutBinding
import com.rungether.app.ui.common.BaseActivity
import com.rungether.app.util.VersionInfo

/**
 * 陪跑员关于页
 *
 * 静态展示 Logo、App 简介、核心功能四项、开发团队、版权与版本号；
 * 版本号通过 PackageManager 读取，保持与 Gradle 构建配置一致。
 */
class GuideAboutActivity : BaseActivity<ActivityGuideAboutBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivityGuideAboutBinding =
        ActivityGuideAboutBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.tvAboutVersion.text = "v${VersionInfo.versionName(this)}"
    }
}
