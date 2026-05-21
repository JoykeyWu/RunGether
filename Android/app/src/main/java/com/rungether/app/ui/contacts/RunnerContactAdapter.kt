package com.rungether.app.ui.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rungether.app.data.local.entity.EmergencyContactEntity
import com.rungether.app.databinding.ItemRunnerContactBinding

/**
 * 盲人端紧急联系人列表 Adapter
 *
 * 与 GuideContactAdapter 共用同一组实体；视觉上采用超大字号、高对比度配色，
 * 编辑与删除两个动作通过回调上抛给 Activity。
 */
class RunnerContactAdapter(
    private val onEdit: (EmergencyContactEntity) -> Unit,
    private val onDelete: (EmergencyContactEntity) -> Unit
) : ListAdapter<EmergencyContactEntity, RunnerContactAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRunnerContactBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        val params = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = (parent.context.resources.displayMetrics.density * 12).toInt()
        }
        binding.root.layoutParams = params
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemRunnerContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // 绑定一条联系人到列表卡片
        fun bind(contact: EmergencyContactEntity) {
            binding.tvContactName.text = contact.name
            binding.tvContactPhone.text = formatPhone(contact.phone)
            binding.btnContactEdit.setOnClickListener { onEdit(contact) }
            binding.btnContactDelete.setOnClickListener { onDelete(contact) }
        }
    }

    // 11 位手机号以 3-4-4 分组展示更易读
    private fun formatPhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length == 11) {
            "${digits.substring(0, 3)} ${digits.substring(3, 7)} ${digits.substring(7)}"
        } else {
            raw
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<EmergencyContactEntity>() {
            override fun areItemsTheSame(
                old: EmergencyContactEntity,
                new: EmergencyContactEntity
            ): Boolean = old.id == new.id

            override fun areContentsTheSame(
                old: EmergencyContactEntity,
                new: EmergencyContactEntity
            ): Boolean = old == new
        }
    }
}
