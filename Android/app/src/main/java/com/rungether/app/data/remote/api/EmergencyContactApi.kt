package com.rungether.app.data.remote.api

import com.rungether.app.data.remote.dto.EmergencyContactDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT

/**
 * 紧急联系人远端接口
 *
 * 对应 mockapi.io 的 RESTful 资源 /emergency_contacts；
 * 仅声明骨架方法，具体业务调用由 Repository 协调。
 */
interface EmergencyContactApi {

    // 拉取全部联系人
    @GET("emergency_contacts")
    suspend fun listAll(): List<EmergencyContactDto>

    // 创建联系人
    @POST("emergency_contacts")
    suspend fun create(@Body dto: EmergencyContactDto): EmergencyContactDto

    // 更新联系人
    @PUT("emergency_contacts/{id}")
    suspend fun update(@Path("id") id: String, @Body dto: EmergencyContactDto): EmergencyContactDto

    // 删除联系人
    @DELETE("emergency_contacts/{id}")
    suspend fun delete(@Path("id") id: String)
}
