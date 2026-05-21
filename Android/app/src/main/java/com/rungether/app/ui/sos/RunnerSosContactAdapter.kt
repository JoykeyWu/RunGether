package com.rungether.app.ui.sos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rungether.app.data.local.entity.EmergencyContactEntity
import com.rungether.app.databinding.ItemRunnerSosContactBinding

/**
 * 紧急联系人列表适配器
 *
 * 仅展示姓名与脱敏前的手机号，并提供「呼叫」入口；
 * 长按或编辑能力位于联系人管理页，本列表只读，避免在紧急场景出现误操作。
 */
class RunnerSosContactAdapter(
    private val onCall: (EmergencyContactEntity) -> Unit
) : ListAdapter<EmergencyContactEntity, RunnerSosContactAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRunnerSosContactBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemRunnerSosContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // 绑定单条联系人数据
        fun bind(item: EmergencyContactEntity) {
            binding.tvContactName.text = item.name
            binding.tvContactPhone.text = item.phone
            binding.btnCallContact.setOnClickListener { onCall(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<EmergencyContactEntity>() {
            override fun areItemsTheSame(
                oldItem: EmergencyContactEntity,
                newItem: EmergencyContactEntity
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: EmergencyContactEntity,
                newItem: EmergencyContactEntity
            ): Boolean = oldItem == newItem
        }
    }
}
