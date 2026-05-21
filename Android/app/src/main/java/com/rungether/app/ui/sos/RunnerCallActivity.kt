package com.rungether.app.ui.sos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.AccelerateInterpolator
import com.rungether.app.databinding.ActivityRunnerCallBinding
import com.rungether.app.service.tts.TtsService
import com.rungether.app.ui.common.BaseActivity

/**
 * 盲人端模拟呼叫页
 *
 * 受 Android 系统对自动拨号的高敏感权限限制，本页面以「模拟演示」方式呈现：
 * 不触发真实拨号、不写入通话记录；展示头像、姓名、电话与呼叫状态动画，
 * TTS 同步播报「正在呼叫」并提示模拟说明；挂断按钮直接 finish 返回上一页。
 */
class RunnerCallActivity : BaseActivity<ActivityRunnerCallBinding>() {

    companion object {
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        const val EXTRA_CONTACT_PHONE = "extra_contact_phone"
    }

    private val ttsService by lazy { TtsService.from(applicationContext) }

    override fun inflateBinding(inflater: LayoutInflater): ActivityRunnerCallBinding =
        ActivityRunnerCallBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        val name = intent.getStringExtra(EXTRA_CONTACT_NAME).orEmpty().ifBlank { "紧急联系人" }
        val phone = intent.getStringExtra(EXTRA_CONTACT_PHONE).orEmpty().ifBlank { "号码未登记" }
        binding.tvCallName.text = name
        binding.tvCallPhone.text = phone
        binding.btnHangup.setOnClickListener { finish() }
        startPulse(binding.pulseRing1, startDelay = 0L)
        startPulse(binding.pulseRing2, startDelay = 1_000L)
    }

    override fun initData() {
        super.initData()
        val name = binding.tvCallName.text.toString()
        ttsService.speak(
            "正在模拟呼叫紧急联系人，${name}。受手机安全策略限制，自动拨打功能为模拟演示",
            TtsService.Mode.FLUSH
        )
    }

    private fun startPulse(view: android.view.View, startDelay: Long) {
        view.scaleX = 1f
        view.scaleY = 1f
        view.alpha = 0.6f
        view.animate().cancel()
        view.animate()
            .setStartDelay(startDelay)
            .scaleX(1.8f)
            .scaleY(1.8f)
            .alpha(0f)
            .setDuration(2_000L)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                if (!isFinishing && !isDestroyed) startPulse(view, startDelay = 0L)
            }
            .start()
    }

    override fun onDestroy() {
        binding.pulseRing1.animate().cancel()
        binding.pulseRing2.animate().cancel()
        ttsService.stop()
        super.onDestroy()
    }
}
