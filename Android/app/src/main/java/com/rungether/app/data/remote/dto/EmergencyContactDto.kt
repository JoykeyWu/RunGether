package com.rungether.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 紧急联系人的远端 DTO
 *
 * JSON 字段与 mockapi.io 服务端约定保持 snake_case 一致；
 * 与本地 EmergencyContactEntity 字段一一对应，转换由 Repository 完成。
 */
data class EmergencyContactDto(

    @SerializedName("id")
    val id: String? = null,

    @SerializedName("name")
    val name: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("created_at")
    val createdAt: Long
)
