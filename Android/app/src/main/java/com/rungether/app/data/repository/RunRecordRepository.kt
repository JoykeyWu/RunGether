package com.rungether.app.data.repository

import com.rungether.app.data.local.dao.RunRecordDao
import com.rungether.app.data.local.entity.RunRecordEntity
import com.rungether.app.data.remote.api.RunRecordApi
import com.rungether.app.data.remote.dto.RunRecordDto
import kotlinx.coroutines.flow.Flow

/**
 * 跑步记录仓库
 *
 * 严格遵循「先读本地 Room、异步请求远端、回写本地、由 Flow 通知 UI」的单一数据源约定。
 * ViewModel 不得直接调用 DAO 或 Retrofit API，所有跑步记录访问都通过本仓库。
 */
class RunRecordRepository(
    private val dao: RunRecordDao,
    private val api: RunRecordApi,
    private val transaction: suspend (block: suspend () -> Unit) -> Unit = { block -> block() }
) {

    // 观察本地全部跑步记录，按开始时间倒序
    fun observeAll(): Flow<List<RunRecordEntity>> = dao.observeAll()

    // 按本地主键读取单条跑步记录
    suspend fun findById(id: Long): RunRecordEntity? = dao.findById(id)

    // 刷新远端记录到本地：以远端列表为唯一镜像，事务内先清掉旧镜像再写入新数据，
    // 离线手工生成的（remote_id 为 NULL）保留不动
    suspend fun refreshFromRemote() {
        val remote = runCatching { api.listAll() }.getOrElse { return }
        transaction {
            dao.deleteAllRemote()
            if (remote.isNotEmpty()) {
                dao.insertAll(remote.map { it.toEntity() })
            }
        }
    }

    // 保存本次跑步：先落本地拿到本地主键，再异步上传远端并回写 remote_id
    suspend fun saveLocallyThenSync(entity: RunRecordEntity): Long {
        val localId = dao.insert(entity)
        runCatching { api.create(entity.copy(id = localId).toDto()) }
            .onSuccess { remote ->
                val remoteId = remote.id ?: return@onSuccess
                val existed = dao.findById(localId) ?: return@onSuccess
                dao.update(existed.copy(remoteId = remoteId))
            }
        return localId
    }

    // 删除指定记录：先删本地立即更新 UI，然后异步删除远端
    suspend fun delete(entity: RunRecordEntity) {
        dao.deleteById(entity.id)
        val remoteId = entity.remoteId ?: return
        runCatching { api.delete(remoteId) }
    }

    // 本地实体转远端 DTO
    private fun RunRecordEntity.toDto(): RunRecordDto = RunRecordDto(
        id = remoteId,
        ownerRole = ownerRole,
        startedAt = startedAt,
        durationSec = durationSec,
        distanceM = distanceM,
        averagePace = averagePace,
        trackJson = trackJson
    )

    // 远端 DTO 转本地实体（保留远端 ID 作为去重映射）
    private fun RunRecordDto.toEntity(): RunRecordEntity = RunRecordEntity(
        remoteId = id,
        ownerRole = ownerRole,
        startedAt = startedAt,
        durationSec = durationSec,
        distanceM = distanceM,
        averagePace = averagePace,
        trackJson = trackJson
    )
}
