package com.rungether.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rungether.app.data.local.entity.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow

/**
 * 紧急联系人 DAO
 *
 * 同样仅声明骨架方法，外部访问必须经过 Repository 入口。
 */
@Dao
interface EmergencyContactDao {

    // 按创建时间正序观察所有联系人
    @Query("SELECT * FROM emergency_contact ORDER BY created_at ASC")
    fun observeAll(): Flow<List<EmergencyContactEntity>>

    // 按主键查询单条联系人
    @Query("SELECT * FROM emergency_contact WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): EmergencyContactEntity?

    // 插入联系人，主键冲突时替换
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: EmergencyContactEntity): Long

    // 批量插入联系人
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<EmergencyContactEntity>): List<Long>

    // 更新联系人
    @Update
    suspend fun update(contact: EmergencyContactEntity)

    // 按主键删除联系人
    @Query("DELETE FROM emergency_contact WHERE id = :id")
    suspend fun deleteById(id: Long)

    // 清空全部联系人
    @Query("DELETE FROM emergency_contact")
    suspend fun clear()
}
