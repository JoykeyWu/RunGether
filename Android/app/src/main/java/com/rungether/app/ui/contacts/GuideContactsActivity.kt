package com.rungether.app.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rungether.app.data.local.entity.EmergencyContactEntity
import com.rungether.app.data.repository.RepositoryProvider
import com.rungether.app.databinding.ActivityGuideContactsBinding
import com.rungether.app.ui.common.BaseActivity
import kotlinx.coroutines.launch

/**
 * 陪跑员紧急联系人管理页
 *
 * 直接订阅 Repository 暴露的本地联系人 Flow，进入页面时触发一次远端拉取并回写本地。
 * 顶部加号弹出底部抽屉用于新增；列表项「编辑」复用同一抽屉，「删除」弹出居中确认弹窗。
 * 所有写入操作经 Repository 同步本地与远端，UI 由 Flow 自动驱动刷新。
 */
class GuideContactsActivity : BaseActivity<ActivityGuideContactsBinding>() {

    private val repository by lazy { RepositoryProvider.emergencyContact(applicationContext) }

    private val adapter = GuideContactAdapter(
        onEdit = { showFormSheet(it) },
        onDelete = { showDeleteDialog(it) }
    )

    private var editing: EmergencyContactEntity? = null
    private var pendingDelete: EmergencyContactEntity? = null

    override fun inflateBinding(inflater: LayoutInflater): ActivityGuideContactsBinding =
        ActivityGuideContactsBinding.inflate(inflater)

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
    }

    // 展示底部抽屉表单：传入 null 表示新增，否则进入编辑模式预填字段
    private fun showFormSheet(contact: EmergencyContactEntity?) {
        editing = contact
        binding.tvFormTitle.text = if (contact == null) "添加紧急联系人" else "编辑紧急联系人"
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
            toast("请填写联系人姓名")
            return
        }
        if (phoneDigits.length != 11) {
            toast("请输入 11 位手机号")
            return
        }
        val current = editing
        lifecycleScope.launch {
            if (current == null) {
                repository.saveLocallyThenSync(
                    EmergencyContactEntity(name = name, phone = phoneDigits)
                )
                toast("已添加联系人")
            } else {
                repository.update(current.copy(name = name, phone = phoneDigits))
                toast("已更新联系人")
            }
            hideFormSheet()
        }
    }

    // 弹出删除确认弹窗
    private fun showDeleteDialog(contact: EmergencyContactEntity) {
        pendingDelete = contact
        binding.tvDeleteDialogTitle.text = "确认删除联系人 ${contact.name}？"
        binding.deleteDialogRoot.visibility = View.VISIBLE
    }

    // 收起删除确认弹窗
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
            toast("已删除联系人")
        }
    }
}
