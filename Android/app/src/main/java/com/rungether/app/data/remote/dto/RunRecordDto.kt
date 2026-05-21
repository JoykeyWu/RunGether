package com.rungether.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 跑步记录的远端 DTO
 *
 * JSON 字段统一 snake_case 与 mockapi.io 服务端约定保持一致。
 * 与本地 RunRecordEntity 字段一一对应，由 Repository 完成相互转换。
 */
data class RunRecordDto(

    @SerializedName("id")
    val id: String? = null,

    @SerializedName("owner_role")
    val ownerRole: String,

    @SerializedName("started_at")
    val startedAt: Long,

    @SerializedName("duration_sec")
    val durationSec: Long,

    @SerializedName("distance_m")
    val distanceM: Double,

    @SerializedName("average_pace")
    val averagePace: Double,

    @SerializedName("track_json")
    val trackJson: String = "[]"
)
