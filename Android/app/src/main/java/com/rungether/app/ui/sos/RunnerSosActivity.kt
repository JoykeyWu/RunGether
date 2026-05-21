package com.rungether.app.ui.sos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.rungether.app.R
import com.rungether.app.data.local.entity.EmergencyContactEntity
import com.rungether.app.databinding.ActivityRunnerSosBinding
import com.rungether.app.ui.common.BaseActivity

/**
 * 盲人端紧急求助页
 *
 * 进入即触发四路反馈：TTS 播报、手电筒闪烁、警报声循环、蓝牙下行 SOS；
 * 顶部红色标题区配呼吸光圈；中部显示三项反馈状态；下方展示紧急联系人列表，
 * 点击「呼叫」进入模拟呼叫页。「解除求助」按钮停止所有反馈并返回主界面。
 */
class RunnerSosActivity : BaseActivity<ActivityRunnerSosBinding>() {

    private val viewModel: RunnerSosViewModel by viewModels()
    private val adapter by lazy {
        RunnerSosContactAdapter { contact -> openCall(contact) }
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityRunnerSosBinding =
        ActivityRunnerSosBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = adapter
        binding.btnDismiss.setOnClickListener { dismissAndFinish() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                dismissAndFinish()
            }
        })
        startPulseAnimation()
    }

    override fun initObserver() {
        super.initObserver()
        viewModel.torchActive.collectOnStarted { renderStatusDot(binding.dotStatusTorch, it) }
        viewModel.alarmActive.collectOnStarted { renderStatusDot(binding.dotStatusAlarm, it) }
        viewModel.notifyDelivered.collectOnStarted { delivered ->
            binding.tvStatusNotify.text = if (delivered) {
                getString(R.string.runner_sos_status_notify)
            } else {
                "未连接陪跑员，已记录求助"
            }
        }
        viewModel.contacts.collectOnStarted { renderContacts(it) }
    }

    override fun initData() {
        super.initData()
        viewModel.trigger()
    }

    private fun renderContacts(items: List<EmergencyContactEntity>) {
        adapter.submitList(items)
        val empty = items.isEmpty()
        binding.tvContactsEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.rvContacts.visibility = if (empty) View.GONE else View.VISIBLE
    }

    // 用呼吸光圈强调求助态：缩放 + 透明度循环
    private fun startPulseAnimation() {
        binding.pulseRing.scaleX = 1f
        binding.pulseRing.scaleY = 1f
        binding.pulseRing.alpha = 0.6f
        binding.pulseRing.animate().cancel()
        binding.pulseRing.animate()
            .scaleX(1.6f)
            .scaleY(1.6f)
            .alpha(0f)
            .setDuration(1_500L)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { repeatPulse() }
            .start()
    }

    private fun repeatPulse() {
        if (isFinishing || isDestroyed) return
        startPulseAnimation()
    }

    private fun renderStatusDot(view: View, active: Boolean) {
        view.setBackgroundResource(
            if (active) R.drawable.bg_runner_dot_warn
            else R.drawable.bg_runner_dot_success
        )
        view.alpha = if (active) 1f else 0.3f
    }

    private fun openCall(contact: EmergencyContactEntity) {
        val intent = Intent(this, RunnerCallActivity::class.java).apply {
            putExtra(RunnerCallActivity.EXTRA_CONTACT_NAME, contact.name)
            putExtra(RunnerCallActivity.EXTRA_CONTACT_PHONE, contact.phone)
        }
        startActivity(intent)
    }

    private fun dismissAndFinish() {
        viewModel.dismiss()
        finish()
    }

    override fun onDestroy() {
        binding.pulseRing.animate().cancel()
        super.onDestroy()
    }
}
