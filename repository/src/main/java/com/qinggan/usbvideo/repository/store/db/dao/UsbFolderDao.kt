package com.qinggan.usbvideo.repository.store.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.qinggan.usbvideo.repository.entity.UsbFolder

/**
 * U盘视频
 *
 * @author senierr_zhou
 * @date 2021/07/15
 */
@Dao
interface UsbFolderDao {
    @Query("SELECT * FROM UsbFolder WHERE path = :path")
    suspend fun get(path: String): UsbFolder?

    @Query("SELECT * FROM UsbFolder")
    suspend fun getAll(): List<UsbFolder>

    @Query("SELECT * FROM UsbFolder WHERE bucketPath = :bucketPath")
    suspend fun getAllByBucket(bucketPath: String): List<UsbFolder>

    @Query("SELECT * FROM UsbFolder WHERE bucketPath like :volumePath||'%'")
    suspend fun getAllByVolume(volumePath: String): List<UsbFolder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(usbVideo: UsbFolder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(usbBuckets: List<UsbFolder>)

    @Query("DELETE FROM UsbFolder WHERE path = :path")
    suspend fun deleteById(path: String)

    @Query("DELETE FROM UsbFolder")
    suspend fun deleteAll()

    @Query("DELETE FROM UsbFolder WHERE bucketPath like :volumePath||'%'")
    suspend fun deleteAllByVolume(volumePath: String)
}