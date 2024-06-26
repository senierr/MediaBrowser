package com.senierr.media.repository.store.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.senierr.media.repository.entity.PlaySession

/**
 * 播放会话
 *
 * @author senierr_zhou
 * @date 2021/07/15
 */
@Dao
interface PlaySessionDao {
    @Query("SELECT * FROM PlaySession WHERE id = :id")
    suspend fun get(id: Long): PlaySession?

    @Query("SELECT * FROM PlaySession")
    suspend fun getAll(): List<PlaySession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(playSession: PlaySession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(playSessions: List<PlaySession>)

    @Query("DELETE FROM PlaySession WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM PlaySession")
    suspend fun deleteAll()
}