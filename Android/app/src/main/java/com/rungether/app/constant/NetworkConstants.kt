package com.rungether.app.constant

/**
 * 网络层相关常量
 *
 * 集中 Mock 后端的根地址、超时阈值等配置，避免 BuildConfig 之外的硬编码。
 */
object NetworkConstants {

    // 公网 Mock 服务根地址，无鉴权
    const val BASE_URL: String = "https://6a0d90e6769682b8ee766f58.mockapi.io/api/v1/"

    // 连接超时秒
    const val CONNECT_TIMEOUT_SEC: Long = 15L

    // 读超时秒
    const val READ_TIMEOUT_SEC: Long = 20L

    // 写超时秒
    const val WRITE_TIMEOUT_SEC: Long = 20L
}
