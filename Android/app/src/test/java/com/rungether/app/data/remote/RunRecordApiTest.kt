package com.rungether.app.data.remote

import com.rungether.app.data.remote.api.RunRecordApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * 跑步记录网络接口的 MockWebServer 单元测试
 *
 * 验证 Retrofit + Gson 能正确解析 mockapi.io 约定的 snake_case 字段，
 * 并将 List<RunRecordDto> 反序列化为预期对象数量与字段值。
 */
class RunRecordApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: RunRecordApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val baseUrl = server.url("/api/v1/").toString()
        api = ApiClient.build(baseUrl).create(RunRecordApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // 列表接口：应能正确解析 2 条记录并保留 snake_case 字段
    @Test
    fun listAll_parsesTwoRecordsAndKeepsFields() = runBlocking {
        val body = """
            [
              {
                "id": "r-1",
                "owner_role": "GUIDE",
                "started_at": 1747700000000,
                "duration_sec": 1800,
                "distance_m": 5000.0,
                "average_pace": 6.0,
                "track_json": "[]"
              },
              {
                "id": "r-2",
                "owner_role": "RUNNER",
                "started_at": 1747800000000,
                "duration_sec": 600,
                "distance_m": 2000.0,
                "average_pace": 5.0,
                "track_json": "[]"
              }
            ]
        """.trimIndent()
        server.enqueue(MockResponse().setBody(body).setResponseCode(200))

        val records = api.listAll()
        assertEquals(2, records.size)
        assertEquals("r-1", records[0].id)
        assertEquals("GUIDE", records[0].ownerRole)
        assertEquals(5000.0, records[0].distanceM, 0.0001)
        assertEquals(6.0, records[0].averagePace, 0.0001)
    }
}
