package com.rungether.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rungether.app.data.local.entity.RunRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 跑步记录 DAO
 *
 * 仅声明骨架方法；ViewModel 不得直接调用 DAO，所有外部访问必须经过 Repository。
 */
@Dao
interface RunRecordDao {

    // 按开始时间倒序观察所有跑步记录
    @Query("SELECT * FROM run_record ORDER BY started_at DESC")
    fun observeAll(): Flow<List<RunRecordEntity>>

    // 按主键查询单条记录
    @Query("SELECT * FROM run_record WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): RunRecordEntity?

    // 按远端 ID 查询单条记录，用于本地与远端的去重映射
    @Query("SELECT * FROM run_record WHERE remote_id = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): RunRecordEntity?

    // 插入记录，主键冲突时替换
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: RunRecordEntity): Long

    // 批量插入记录，主键冲突时替换
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<RunRecordEntity>): List<Long>

    // 更新记录
    @Update
    suspend fun update(record: RunRecordEntity)

    // 按主键删除记录
    @Query("DELETE FROM run_record WHERE id = :id")
    suspend fun deleteById(id: Long)

    // 清空全部跑步记录
    @Query("DELETE FROM run_record")
    suspend fun clear()

    // 删除所有来自远端的镜像行，离线创建的（remote_id 为 NULL）保留
    @Query("DELETE FROM run_record WHERE remote_id IS NOT NULL")
    suspend fun deleteAllRemote()

    // 统计当前记录总数
    @Query("SELECT COUNT(*) FROM run_record")
    suspend fun count(): Int
}
