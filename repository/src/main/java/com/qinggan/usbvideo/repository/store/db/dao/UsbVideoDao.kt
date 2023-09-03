package com.qinggan.usbvideo.repository.store.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.qinggan.usbvideo.repository.entity.UsbVideo

/**
 * U盘视频
 *
 * @author senierr_zhou
 * @date 2021/07/15
 */
@Dao
interface UsbVideoDao {
    @Query("SELECT * FROM UsbVideo WHERE id = :id")
    suspend fun get(id: String): UsbVideo?

    @Query("SELECT * FROM UsbVideo")
    suspend fun getAll(): List<UsbVideo>

    @Query("SELECT * FROM UsbVideo WHERE bucketPath = :bucketPath")
    suspend fun getAllByBucket(bucketPath: String): List<UsbVideo>

    @Query("SELECT * FROM UsbVideo WHERE bucketPath like :volumePath||'%'")
    suspend fun getAllByVolume(volumePath: String): List<UsbVideo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(usbVideo: UsbVideo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(usbVideos: List<UsbVideo>)

    @Query("DELETE FROM UsbVideo WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM UsbVideo")
    suspend fun deleteAll()

    @Query("DELETE FROM UsbVideo WHERE bucketPath like :volumePath||'%'")
    suspend fun deleteAllByVolume(volumePath: String)
}