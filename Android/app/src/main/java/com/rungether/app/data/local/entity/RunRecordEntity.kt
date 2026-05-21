package com.rungether.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 跑步记录本地实体
 *
 * 字段与需求文档「跑步记录与历史 - 记录字段」对齐：日期、总时长、总距离、平均配速。
 * 额外包含远端同步用的 remote_id 与本机生成的角色标记 owner_role。
 * track_json 存储自绘轨迹的坐标序列 JSON，避免引入额外关联表。
 */
@Entity(tableName = "run_record")
data class RunRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "remote_id")
    val remoteId: String? = null,

    @ColumnInfo(name = "owner_role")
    val ownerRole: String,

    @ColumnInfo(name = "started_at")
    val startedAt: Long,

    @ColumnInfo(name = "duration_sec")
    val durationSec: Long,

    @ColumnInfo(name = "distance_m")
    val distanceM: Double,

    @ColumnInfo(name = "average_pace")
    val averagePace: Double,

    @ColumnInfo(name = "track_json")
    val trackJson: String = "[]"
)
