package com.senierr.media.local.repository.store.db

import android.content.Context
import androidx.room.Room

/**
 * 数据库管理
 *
 * @author senierr_zhou
 * @date 2021/07/15
 */
object DatabaseManager {

    private const val DB_NAME = "media_repository.db" // 数据库名
    const val DB_VERSION = 1    // 数据库版本

    private lateinit var database: AppDatabase

    /**
     * 初始化
     */
    fun initialize(context: Context) {
        database = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .build()
    }

    /**
     * 获取数据库
     */
    fun getDatabase(): AppDatabase {
        return database
    }
}