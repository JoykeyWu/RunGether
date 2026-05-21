package com.rungether.app.data.repository

import android.content.Context
import com.rungether.app.data.local.database.AppDatabase
import com.rungether.app.data.remote.ApiClient

/**
 * 仓库实例提供方
 *
 * 集中管理 Repository 单例的创建依赖，避免在 Application/ViewModel 中重复装配；
 * 单元测试可绕过本对象直接以构造方式注入假实现。
 */
object RepositoryProvider {

    @Volatile
    private var runRecordRepository: RunRecordRepository? = null

    @Volatile
    private var emergencyContactRepository: EmergencyContactRepository? = null

    // 获取跑步记录仓库单例
    fun runRecord(context: Context): RunRecordRepository {
        return runRecordRepository ?: synchronized(this) {
            runRecordRepository ?: RunRecordRepository(
                dao = AppDatabase.getInstance(context).runRecordDao(),
                api = ApiClient.runRecordApi
            ).also { runRecordRepository = it }
        }
    }

    // 获取紧急联系人仓库单例
    fun emergencyContact(context: Context): EmergencyContactRepository {
        return emergencyContactRepository ?: synchronized(this) {
            emergencyContactRepository ?: EmergencyContactRepository(
                dao = AppDatabase.getInstance(context).emergencyContactDao(),
                api = ApiClient.emergencyContactApi
            ).also { emergencyContactRepository = it }
        }
    }
}
