package com.rungether.app.data.repository

import com.rungether.app.data.local.dao.EmergencyContactDao
import com.rungether.app.data.local.entity.EmergencyContactEntity
import com.rungether.app.data.remote.api.EmergencyContactApi
import com.rungether.app.data.remote.dto.EmergencyContactDto
import kotlinx.coroutines.flow.Flow

/**
 * 紧急联系人仓库
 *
 * 同样遵循「本地 → 远端 → 回写 → 通知」的单一数据源约定，
 * 与 RunRecordRepository 共享对外契约结构。
 */
class EmergencyContactRepository(
    private val dao: EmergencyContactDao,
    private val api: EmergencyContactApi,
    private val transaction: suspend (block: suspend () -> Unit) -> Unit = { block -> block() }
) {

    // 观察本地全部联系人
    fun observeAll(): Flow<List<EmergencyContactEntity>> = dao.observeAll()

    // 刷新远端联系人到本地：以远端列表为唯一镜像，事务内先清掉旧镜像再写入新数据，
    // 离线手工新增的（remote_id 为 NULL）保留不动
    suspend fun refreshFromRemote() {
        val remote = runCatching { api.listAll() }.getOrElse { return }
        transaction {
            dao.deleteAllRemote()
            if (remote.isNotEmpty()) {
                dao.insertAll(remote.map { it.toEntity() })
            }
        }
    }

    // 保存联系人到本地后同步远端
    suspend fun saveLocallyThenSync(entity: EmergencyContactEntity): Long {
        val localId = dao.insert(entity)
        runCatching { api.create(entity.toDto()) }
            .onSuccess { remote ->
                val remoteId = remote.id ?: return@onSuccess
                val existed = dao.findById(localId) ?: return@onSuccess
                dao.update(existed.copy(remoteId = remoteId))
            }
        return localId
    }

    // 更新联系人
    suspend fun update(entity: EmergencyContactEntity) {
        dao.update(entity)
        val remoteId = entity.remoteId ?: return
        runCatching { api.update(remoteId, entity.toDto()) }
    }

    // 删除联系人
    suspend fun delete(entity: EmergencyContactEntity) {
        dao.deleteById(entity.id)
        val remoteId = entity.remoteId ?: return
        runCatching { api.delete(remoteId) }
    }

    private fun EmergencyContactEntity.toDto(): EmergencyContactDto = EmergencyContactDto(
        id = remoteId,
        name = name,
        phone = phone,
        createdAt = createdAt
    )

    private fun EmergencyContactDto.toEntity(): EmergencyContactEntity = EmergencyContactEntity(
        remoteId = id,
        name = name,
        phone = phone,
        createdAt = createdAt
    )
}
