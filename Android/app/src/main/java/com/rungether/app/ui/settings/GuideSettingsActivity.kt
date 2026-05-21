package com.rungether.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rungether.app.R
import com.rungether.app.bluetooth.BluetoothModule
import com.rungether.app.bluetooth.connection.ConnectionState
import com.rungether.app.data.prefs.TtsSpeed
import com.rungether.app.data.prefs.UserPreferences
import com.rungether.app.data.prefs.UserRole
import com.rungether.app.data.repository.RepositoryProvider
import com.rungether.app.databinding.ActivityGuideSettingsBinding
import com.rungether.app.service.tts.TtsService
import com.rungether.app.ui.about.GuideAboutActivity
import com.rungether.app.ui.bluetooth.BluetoothPairActivity
import com.rungether.app.ui.common.BaseActivity
import com.rungether.app.ui.contacts.GuideContactsActivity
import com.rungether.app.ui.role.RoleSelectActivity
import com.rungether.app.util.VersionInfo
import kotlinx.coroutines.launch

/**
 * 陪跑员设置页
 *
 * 顶部展示当前角色卡片与切换入口（带确认弹窗）；
 * 中部「播报与反馈」分段切换 TTS 语速并实时调用 TtsService 播报示例文案，
 * 震动开关实时写入偏好；「安全」分组里展示当前联系人数与蓝牙连接副标题，
 * 入口分别跳转紧急联系人与蓝牙配对管理；底部「关于」入口与版本号取自构建配置。
 */
class GuideSettingsActivity : BaseActivity<ActivityGuideSettingsBinding>() {

    private val preferences by lazy { UserPreferences.from(applicationContext) }
    private val contactRepository by lazy { RepositoryProvider.emergencyContact(applicationContext) }
    private val ttsService by lazy { TtsService.from(applicationContext) }
    private val connectionManager by lazy { BluetoothModule.connectionManager(applicationContext) }

    override fun inflateBinding(inflater: LayoutInflater): ActivityGuideSettingsBinding =
        ActivityGuideSettingsBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnSwitchRole.setOnClickListener { showSwitchRoleDialog() }
        binding.btnSwitchCancel.setOnClickListener { binding.switchRoleRoot.visibility = View.GONE }
        binding.btnSwitchConfirm.setOnClickListener { confirmSwitchRole() }
        binding.switchRoleRoot.setOnClickListener { binding.switchRoleRoot.visibility = View.GONE }

        binding.segTtsSlow.setOnClickListener { selectTtsSpeed(TtsSpeed.SLOW) }
        binding.segTtsNormal.setOnClickListener { selectTtsSpeed(TtsSpeed.NORMAL) }
        binding.segTtsFast.setOnClickListener { selectTtsSpeed(TtsSpeed.FAST) }

        binding.rowVibration.setOnClickListener { toggleVibration() }
        binding.toggleVibration.setOnClickListener { toggleVibration() }

        binding.rowContacts.setOnClickListener {
            startActivity(Intent(this, GuideContactsActivity::class.java))
        }
        binding.rowBluetooth.setOnClickListener {
            startActivity(Intent(this, BluetoothPairActivity::class.java))
        }
        binding.rowAbout.setOnClickListener {
            startActivity(Intent(this, GuideAboutActivity::class.java))
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
        connectionManager.state.collectOnStarted { renderConnection(it) }
    }

    override fun initData() {
        super.initData()
        lifecycleScope.launch { contactRepository.refreshFromRemote() }
    }

    // 弹出角色切换确认弹窗
    private fun showSwitchRoleDialog() {
        binding.switchRoleRoot.visibility = View.VISIBLE
    }

    // 确认切换角色：清空当前角色偏好后跳转到角色选择页
    private fun confirmSwitchRole() {
        binding.switchRoleRoot.visibility = View.GONE
        preferences.currentRole = UserRole.UNSELECTED
        val intent = Intent(this, RoleSelectActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    // 选择 TTS 档位：写入偏好、切换底层引擎、再用新档位播报一段示例文案
    private fun selectTtsSpeed(speed: TtsSpeed) {
        preferences.ttsSpeed = speed
        ttsService.applySpeed(speed)
        ttsService.speak("开始跑步", TtsService.Mode.FLUSH)
    }

    // 切换震动开关：直接写入偏好，VibrationService 在下次触发时按新值生效
    private fun toggleVibration() {
        preferences.vibrationEnabled = !preferences.vibrationEnabled
    }

    private fun renderTtsSpeed(speed: TtsSpeed) {
        styleSegment(binding.segTtsSlow, speed == TtsSpeed.SLOW)
        styleSegment(binding.segTtsNormal, speed == TtsSpeed.NORMAL)
        styleSegment(binding.segTtsFast, speed == TtsSpeed.FAST)
    }

    private fun styleSegment(view: TextView, selected: Boolean) {
        if (selected) {
            view.setBackgroundResource(R.drawable.bg_seg_active)
            view.setTextColor(ContextCompat.getColor(this, R.color.color_white))
        } else {
            view.setBackgroundResource(R.drawable.bg_seg_inactive)
            view.setTextColor(ContextCompat.getColor(this, R.color.guide_navy))
        }
    }

    private fun renderVibration(enabled: Boolean) {
        binding.toggleVibration.setBackgroundResource(
            if (enabled) R.drawable.bg_toggle_on else R.drawable.bg_toggle_off
        )
        val params = binding.toggleVibrationDot.layoutParams as FrameLayout.LayoutParams
        params.gravity = if (enabled) {
            android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
        } else {
            android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
        }
        binding.toggleVibrationDot.layoutParams = params
    }

    private fun renderConnection(state: ConnectionState) {
        binding.tvBluetoothSubtitle.text = when (state) {
            is ConnectionState.Connected -> {
                val name = state.deviceName?.takeIf { it.isNotBlank() } ?: "盲人端设备"
                "$name · 已连接"
            }
            is ConnectionState.Connecting -> {
                val name = state.deviceName?.takeIf { it.isNotBlank() } ?: state.deviceAddress
                "$name · 连接中"
            }
            is ConnectionState.Disconnected -> "尚未连接盲人端"
            is ConnectionState.Error -> "连接异常 · 点击重试"
            ConnectionState.Idle, ConnectionState.Scanning -> "尚未连接盲人端"
        }
    }
}
