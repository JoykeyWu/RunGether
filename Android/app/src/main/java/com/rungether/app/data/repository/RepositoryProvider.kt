package com.rungether.app.data.repository

import android.content.Context
import androidx.room.withTransaction
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
            runRecordRepository ?: AppDatabase.getInstance(context).let { db ->
                RunRecordRepository(
                    dao = db.runRecordDao(),
                    api = ApiClient.runRecordApi,
                    transaction = { block -> db.withTransaction { block() } }
                )
            }.also { runRecordRepository = it }
        }
    }

    // 获取紧急联系人仓库单例
    fun emergencyContact(context: Context): EmergencyContactRepository {
        return emergencyContactRepository ?: synchronized(this) {
            emergencyContactRepository ?: AppDatabase.getInstance(context).let { db ->
                EmergencyContactRepository(
                    dao = db.emergencyContactDao(),
                    api = ApiClient.emergencyContactApi,
                    transaction = { block -> db.withTransaction { block() } }
                )
            }.also { emergencyContactRepository = it }
        }
    }
}
