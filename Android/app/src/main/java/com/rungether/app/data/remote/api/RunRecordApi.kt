package com.rungether.app.data.remote.api

import com.rungether.app.data.remote.dto.RunRecordDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT

/**
 * 跑步记录远端接口
 *
 * 对应 mockapi.io 的 RESTful 资源 /run_records；
 * 仅声明骨架方法，具体业务调用由 Repository 协调。
 */
interface RunRecordApi {

    // 拉取全部跑步记录
    @GET("run_records")
    suspend fun listAll(): List<RunRecordDto>

    // 按 ID 拉取单条记录
    @GET("run_records/{id}")
    suspend fun findById(@Path("id") id: String): RunRecordDto

    // 上传新跑步记录
    @POST("run_records")
    suspend fun create(@Body dto: RunRecordDto): RunRecordDto

    // 更新跑步记录
    @PUT("run_records/{id}")
    suspend fun update(@Path("id") id: String, @Body dto: RunRecordDto): RunRecordDto

    // 删除跑步记录
    @DELETE("run_records/{id}")
    suspend fun delete(@Path("id") id: String)
}
