package com.rungether.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 紧急联系人本地实体
 *
 * 字段与需求文档「紧急联系人」对齐：仅保留姓名与手机号；
 * remote_id 字段用于与远端 Mock 服务保持同步映射。
 */
@Entity(tableName = "emergency_contact")
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "remote_id")
    val remoteId: String? = null,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "phone")
    val phone: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
