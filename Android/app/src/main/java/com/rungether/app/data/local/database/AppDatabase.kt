package com.rungether.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rungether.app.data.local.dao.EmergencyContactDao
import com.rungether.app.data.local.dao.RunRecordDao
import com.rungether.app.data.local.entity.EmergencyContactEntity
import com.rungether.app.data.local.entity.RunRecordEntity

/**
 * 本地 Room 数据库单例
 *
 * 双端共用同一份 schema，跑步记录区分 owner_role 字段；
 * 版本变更需保留迁移路径，禁止直接清库导致用户历史丢失。
 */
@Database(
    entities = [
        RunRecordEntity::class,
        EmergencyContactEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun runRecordDao(): RunRecordDao

    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        private const val DATABASE_NAME = "rungether.db"

        @Volatile
        private var instance: AppDatabase? = null

        // 获取数据库单例
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        // 仅供单元测试使用：基于内存的实例
        fun buildInMemory(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }

        private fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
