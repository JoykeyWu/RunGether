package com.rungether.app.data.repository

import com.rungether.app.data.local.dao.RunRecordDao
import com.rungether.app.data.local.entity.RunRecordEntity
import com.rungether.app.data.remote.api.RunRecordApi
import com.rungether.app.data.remote.dto.RunRecordDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

/**
 * 跑步记录 Repository 单元测试
 *
 * 用纯 JVM 假实现替代 Room DAO 与 Retrofit API，验证 Repository 履行
 * 「先落本地、再异步上传远端、远端 ID 回写本地」的契约。
 */
class RunRecordRepositoryTest {

    // 校验保存本地后能调用远端创建接口并将 remote_id 回写本地
    @Test
    fun saveLocallyThenSync_writesRemoteIdBack() = runBlocking {
        val fakeDao = FakeRunRecordDao()
        val fakeApi = FakeRunRecordApi(assignedId = "remote-99")
        val repository = RunRecordRepository(fakeDao, fakeApi)

        val entity = RunRecordEntity(
            id = 0L,
            remoteId = null,
            ownerRole = "GUIDE",
            startedAt = 1747700000000L,
            durationSec = 1800L,
            distanceM = 5000.0,
            averagePace = 6.0
        )
        val localId = repository.saveLocallyThenSync(entity)
        val stored = fakeDao.findById(localId)
        assertNotNull(stored)
        assertEquals("remote-99", stored?.remoteId)
        assertEquals(1, fakeApi.createCallCount)
    }

    // 校验删除本地后会触发远端删除接口
    @Test
    fun delete_removesLocalAndCallsRemote() = runBlocking {
        val fakeDao = FakeRunRecordDao()
        val fakeApi = FakeRunRecordApi(assignedId = "remote-1")
        val repository = RunRecordRepository(fakeDao, fakeApi)
        val entity = RunRecordEntity(
            id = 0L,
            remoteId = "remote-1",
            ownerRole = "RUNNER",
            startedAt = 1L,
            durationSec = 1L,
            distanceM = 1.0,
            averagePace = 1.0
        )
        val savedId = fakeDao.insert(entity)

        repository.delete(entity.copy(id = savedId))
        assertEquals(null, fakeDao.findById(savedId))
        assertEquals(1, fakeApi.deleteCallCount)
    }
}

private class FakeRunRecordDao : RunRecordDao {

    private val store: MutableMap<Long, RunRecordEntity> = LinkedHashMap()
    private val idSeq = AtomicLong(0L)

    override fun observeAll(): Flow<List<RunRecordEntity>> = flowOf(store.values.toList())

    override suspend fun findById(id: Long): RunRecordEntity? = store[id]

    override suspend fun findByRemoteId(remoteId: String): RunRecordEntity? =
        store.values.firstOrNull { it.remoteId == remoteId }

    override suspend fun insert(record: RunRecordEntity): Long {
        val id = if (record.id == 0L) idSeq.incrementAndGet() else record.id
        store[id] = record.copy(id = id)
        return id
    }

    override suspend fun insertAll(records: List<RunRecordEntity>): List<Long> =
        records.map { insert(it) }

    override suspend fun update(record: RunRecordEntity) {
        store[record.id] = record
    }

    override suspend fun deleteById(id: Long) {
        store.remove(id)
    }

    override suspend fun clear() {
        store.clear()
    }

    override suspend fun count(): Int = store.size
}

private class FakeRunRecordApi(private val assignedId: String) : RunRecordApi {

    var createCallCount: Int = 0
        private set
    var deleteCallCount: Int = 0
        private set

    override suspend fun listAll(): List<RunRecordDto> = emptyList()

    override suspend fun findById(id: String): RunRecordDto = error("未实现")

    override suspend fun create(dto: RunRecordDto): RunRecordDto {
        createCallCount += 1
        return dto.copy(id = assignedId)
    }

    override suspend fun update(id: String, dto: RunRecordDto): RunRecordDto = dto.copy(id = id)

    override suspend fun delete(id: String) {
        deleteCallCount += 1
    }
}
