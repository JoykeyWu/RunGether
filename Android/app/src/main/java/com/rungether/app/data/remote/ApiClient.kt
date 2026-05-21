package com.rungether.app.data.remote

import com.rungether.app.constant.NetworkConstants
import com.rungether.app.data.remote.api.EmergencyContactApi
import com.rungether.app.data.remote.api.RunRecordApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 客户端
 *
 * 集中构建 OkHttpClient 与 Retrofit 实例，统一超时、日志拦截与根地址；
 * 单例由 ApiClient.runRecordApi 等惰性属性提供，避免重复创建。
 */
object ApiClient {

    private val loggingInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(NetworkConstants.CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(NetworkConstants.READ_TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(NetworkConstants.WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    // 默认 Retrofit 实例
    val retrofit: Retrofit by lazy { build(NetworkConstants.BASE_URL) }

    // 跑步记录接口
    val runRecordApi: RunRecordApi by lazy { retrofit.create(RunRecordApi::class.java) }

    // 紧急联系人接口
    val emergencyContactApi: EmergencyContactApi by lazy {
        retrofit.create(EmergencyContactApi::class.java)
    }

    // 单元测试用：以指定 baseUrl 构建 Retrofit 实例
    fun build(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
