package com.rungether.app.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rungether.app.data.local.entity.EmergencyContactEntity
import com.rungether.app.data.repository.RepositoryProvider
import com.rungether.app.databinding.ActivityRunnerContactsBinding
import com.rungether.app.service.tts.TtsService
import com.rungether.app.ui.common.BaseActivity
import kotlinx.coroutines.launch

/**
 * 盲人端紧急联系人管理页
 *
 * 与陪跑员端共享同一份联系人数据：订阅 Repository 暴露的本地 Flow，
 * 进入页面时触发一次远端拉取，所有写操作通过 Repository 完成本地与远端同步。
 * 顶部加号弹出底部抽屉新增；列表项「编辑」复用同一抽屉、「删除」弹出居中确认。
 * 每一步关键操作都伴随 TTS 反馈，保障无障碍体验。
 */
class RunnerContactsActivity : BaseActivity<ActivityRunnerContactsBinding>() {

    private val repository by lazy { RepositoryProvider.emergencyContact(applicationContext) }
    private val ttsService by lazy { TtsService.from(applicationContext) }

    private val adapter = RunnerContactAdapter(
        onEdit = { showFormSheet(it) },
        onDelete = { showDeleteDialog(it) }
    )

    private var editing: EmergencyContactEntity? = null
    private var pendingDelete: EmergencyContactEntity? = null

    override fun inflateBinding(inflater: LayoutInflater): ActivityRunnerContactsBinding =
        ActivityRunnerContactsBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnAddContact.setOnClickListener { showFormSheet(null) }

        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = adapter

        binding.formSheetRoot.setOnClickListener { hideFormSheet() }
        binding.btnFormClose.setOnClickListener { hideFormSheet() }
        binding.btnFormCancel.setOnClickListener { hideFormSheet() }
        binding.btnFormSave.setOnClickListener { saveForm() }

        binding.deleteDialogRoot.setOnClickListener { hideDeleteDialog() }
        binding.btnDeleteCancel.setOnClickListener { hideDeleteDialog() }
        binding.btnDeleteConfirm.setOnClickListener { confirmDelete() }
    }

    override fun initObserver() {
        super.initObserver()
        repository.observeAll().collectOnStarted { contacts ->
            adapter.submitList(contacts)
            val empty = contacts.isEmpty()
            binding.emptyState.visibility = if (empty) View.VISIBLE else View.GONE
            binding.rvContacts.visibility = if (empty) View.GONE else View.VISIBLE
        }
    }

    override fun initData() {
        super.initData()
        lifecycleScope.launch { repository.refreshFromRemote() }
        ttsService.speak("紧急联系人管理", TtsService.Mode.FLUSH)
    }

    override fun onDestroy() {
        ttsService.stop()
        super.onDestroy()
    }

    // 展示底部抽屉表单：传入 null 表示新增，否则进入编辑模式预填字段
    private fun showFormSheet(contact: EmergencyContactEntity?) {
        editing = contact
        if (contact == null) {
            binding.tvFormTitle.setText(com.rungether.app.R.string.runner_contacts_form_add_title)
            ttsService.speak("添加紧急联系人", TtsService.Mode.FLUSH)
        } else {
            binding.tvFormTitle.setText(com.rungether.app.R.string.runner_contacts_form_edit_title)
            ttsService.speak("编辑紧急联系人 ${contact.name}", TtsService.Mode.FLUSH)
        }
        binding.etContactName.setText(contact?.name.orEmpty())
        binding.etContactPhone.setText(contact?.phone.orEmpty())
        binding.formSheetRoot.visibility = View.VISIBLE
    }

    // 收起底部抽屉表单并清空表单状态
    private fun hideFormSheet() {
        binding.formSheetRoot.visibility = View.GONE
        binding.etContactName.text?.clear()
        binding.etContactPhone.text?.clear()
        editing = null
    }

    // 保存表单：基础校验后写入 Repository
    private fun saveForm() {
        val name = binding.etContactName.text?.toString()?.trim().orEmpty()
        val phoneInput = binding.etContactPhone.text?.toString()?.trim().orEmpty()
        val phoneDigits = phoneInput.filter { it.isDigit() }
        if (name.isEmpty()) {
            ttsService.speak("请填写联系人姓名", TtsService.Mode.FLUSH)
            toast("请填写联系人姓名")
            return
        }
        if (phoneDigits.length != 11) {
            ttsService.speak("请输入 11 位手机号", TtsService.Mode.FLUSH)
            toast("请输入 11 位手机号")
            return
        }
        val current = editing
        lifecycleScope.launch {
            if (current == null) {
                repository.saveLocallyThenSync(
                    EmergencyContactEntity(name = name, phone = phoneDigits)
                )
                ttsService.speak("已添加联系人 $name", TtsService.Mode.FLUSH)
                toast("已添加联系人")
            } else {
                repository.update(current.copy(name = name, phone = phoneDigits))
                ttsService.speak("已更新联系人 $name", TtsService.Mode.FLUSH)
                toast("已更新联系人")
            }
            hideFormSheet()
        }
    }

    // 弹出居中删除确认弹窗
    private fun showDeleteDialog(contact: EmergencyContactEntity) {
        pendingDelete = contact
        binding.tvDeleteDialogTitle.text = "确认删除联系人 ${contact.name}？"
        binding.deleteDialogRoot.visibility = View.VISIBLE
        ttsService.speak("确认删除联系人 ${contact.name}？", TtsService.Mode.FLUSH)
    }

    // 关闭删除确认弹窗
    private fun hideDeleteDialog() {
        binding.deleteDialogRoot.visibility = View.GONE
        pendingDelete = null
    }

    // 执行删除：通过 Repository 同步删除本地与远端
    private fun confirmDelete() {
        val target = pendingDelete ?: run {
            hideDeleteDialog()
            return
        }
        hideDeleteDialog()
        lifecycleScope.launch {
            repository.delete(target)
            ttsService.speak("已删除联系人 ${target.name}", TtsService.Mode.FLUSH)
            toast("已删除联系人")
        }
    }
}
