package com.rungether.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rungether.app.R
import com.rungether.app.data.prefs.TtsSpeed
import com.rungether.app.data.prefs.UserPreferences
import com.rungether.app.data.prefs.UserRole
import com.rungether.app.data.repository.RepositoryProvider
import com.rungether.app.databinding.ActivityRunnerSettingsBinding
import com.rungether.app.service.tts.TtsService
import com.rungether.app.ui.about.RunnerAboutActivity
import com.rungether.app.ui.common.BaseActivity
import com.rungether.app.ui.contacts.RunnerContactsActivity
import com.rungether.app.ui.role.RoleSelectActivity
import com.rungether.app.util.VersionInfo
import kotlinx.coroutines.launch

/**
 * 盲人端设置页
 *
 * 顶部展示当前角色卡片与切换入口（带确认弹窗）；
 * 语音速度三档分段实时切换 TtsService 语速并播报示例文案；
 * 震动反馈开关写入偏好，由 VibrationService 在下次触发时按新值生效；
 * 「紧急联系人」副标题动态展示已添加人数；底部「关于」入口与版本号取自构建配置。
 * 所有操作均伴随 TTS 反馈，保障无障碍体验。
 */
class RunnerSettingsActivity : BaseActivity<ActivityRunnerSettingsBinding>() {

    private val preferences by lazy { UserPreferences.from(applicationContext) }
    private val contactRepository by lazy { RepositoryProvider.emergencyContact(applicationContext) }
    private val ttsService by lazy { TtsService.from(applicationContext) }

    override fun inflateBinding(inflater: LayoutInflater): ActivityRunnerSettingsBinding =
        ActivityRunnerSettingsBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnSwitchRole.setOnClickListener { showSwitchDialog() }
        binding.btnSwitchCancel.setOnClickListener { hideSwitchDialog() }
        binding.btnSwitchConfirm.setOnClickListener { confirmSwitchRole() }
        binding.switchRoleRoot.setOnClickListener { hideSwitchDialog() }

        binding.segTtsSlow.setOnClickListener { selectTtsSpeed(TtsSpeed.SLOW) }
        binding.segTtsNormal.setOnClickListener { selectTtsSpeed(TtsSpeed.NORMAL) }
        binding.segTtsFast.setOnClickListener { selectTtsSpeed(TtsSpeed.FAST) }

        binding.rowVibration.setOnClickListener { toggleVibration() }
        binding.toggleVibration.setOnClickListener { toggleVibration() }

        binding.rowContacts.setOnClickListener {
            ttsService.speak("打开紧急联系人", TtsService.Mode.FLUSH)
            startActivity(Intent(this, RunnerContactsActivity::class.java))
        }
        binding.rowAbout.setOnClickListener {
            ttsService.speak("打开关于页", TtsService.Mode.FLUSH)
            startActivity(Intent(this, RunnerAboutActivity::class.java))
        }

        binding.tvVersionFooter.text = "RunGether v${VersionInfo.versionName(this)}"
    }

    override fun initObserver() {
        super.initObserver()
        preferences.observeTtsSpeed().collectOnStarted { renderTtsSpeed(it) }
        preferences.observeVibrationEnabled().collectOnStarted { renderVibration(it) }
        contactRepository.observeAll().collectOnStarted { contacts ->
            binding.tvContactsSubtitle.text = "已添加 ${contacts.size} 人"
        }
    }

    override fun initData() {
        super.initData()
        lifecycleScope.launch { contactRepository.refreshFromRemote() }
    }

    override fun onDestroy() {
        ttsService.stop()
        super.onDestroy()
    }

    // 显示角色切换确认弹窗
    private fun showSwitchDialog() {
        binding.switchRoleRoot.visibility = View.VISIBLE
        ttsService.speak("确认切换角色？", TtsService.Mode.FLUSH)
    }

    private fun hideSwitchDialog() {
        binding.switchRoleRoot.visibility = View.GONE
    }

    // 确认切换角色：清空当前角色偏好后跳转到角色选择页
    private fun confirmSwitchRole() {
        hideSwitchDialog()
        preferences.currentRole = UserRole.UNSELECTED
        ttsService.speak("已切换角色", TtsService.Mode.FLUSH)
        val intent = Intent(this, RoleSelectActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    // 选择 TTS 档位：写入偏好、切换底层引擎、再用新档位播报示例文案
    private fun selectTtsSpeed(speed: TtsSpeed) {
        preferences.ttsSpeed = speed
        ttsService.applySpeed(speed)
        val label = when (speed) {
            TtsSpeed.SLOW -> "慢速"
            TtsSpeed.NORMAL -> "正常"
            TtsSpeed.FAST -> "快速"
        }
        ttsService.speak("语音速度已切换到 $label", TtsService.Mode.FLUSH)
    }

    // 切换震动开关：写入偏好后由 TTS 立即反馈状态
    private fun toggleVibration() {
        val next = !preferences.vibrationEnabled
        preferences.vibrationEnabled = next
        ttsService.speak(if (next) "震动已开启" else "震动已关闭", TtsService.Mode.FLUSH)
    }

    private fun renderTtsSpeed(speed: TtsSpeed) {
        styleSegment(binding.segTtsSlow, speed == TtsSpeed.SLOW)
        styleSegment(binding.segTtsNormal, speed == TtsSpeed.NORMAL)
        styleSegment(binding.segTtsFast, speed == TtsSpeed.FAST)
    }

    private fun styleSegment(view: TextView, selected: Boolean) {
        if (selected) {
            view.setBackgroundResource(R.drawable.bg_runner_seg_active)
            view.setTextColor(ContextCompat.getColor(this, R.color.runner_bg))
        } else {
            view.setBackgroundResource(R.drawable.bg_runner_seg_inactive)
            view.setTextColor(ContextCompat.getColor(this, R.color.runner_high))
        }
    }

    private fun renderVibration(enabled: Boolean) {
        binding.toggleVibration.setBackgroundResource(
            if (enabled) R.drawable.bg_runner_toggle_on else R.drawable.bg_runner_toggle_off
        )
        binding.toggleVibrationDot.setBackgroundResource(
            if (enabled) R.drawable.bg_runner_toggle_dot_on else R.drawable.bg_runner_toggle_dot_off
        )
        val params = binding.toggleVibrationDot.layoutParams as FrameLayout.LayoutParams
        params.gravity = if (enabled) {
            Gravity.END or Gravity.CENTER_VERTICAL
        } else {
            Gravity.START or Gravity.CENTER_VERTICAL
        }
        binding.toggleVibrationDot.layoutParams = params
    }
}
