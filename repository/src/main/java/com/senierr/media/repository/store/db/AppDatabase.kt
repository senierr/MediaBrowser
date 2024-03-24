package com.senierr.media.local.repository.store.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.senierr.media.local.repository.entity.PlaySession
import com.senierr.media.local.repository.store.db.dao.PlaySessionDao

/**
 * 数据库入口
 *
 * @author senierr_zhou
 * @date 2021/07/15
 */
@Database(entities = [PlaySession::class],
    version = DatabaseManager.DB_VERSION, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getPlaySessionDao(): PlaySessionDao
}