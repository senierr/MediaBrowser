package com.qinggan.usbvideo.repository.store.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.qinggan.usbvideo.repository.entity.UsbImage

/**
 * U盘图片
 *
 * @author senierr_zhou
 * @date 2021/07/15
 */
@Dao
interface UsbImageDao {
    @Query("SELECT * FROM UsbImage WHERE id = :id")
    suspend fun get(id: String): UsbImage?

    @Query("SELECT * FROM UsbImage")
    suspend fun getAll(): List<UsbImage>

    @Query("SELECT * FROM UsbImage WHERE bucketPath = :bucketPath")
    suspend fun getAllByBucket(bucketPath: String): List<UsbImage>

    @Query("SELECT * FROM UsbImage WHERE bucketPath like :volumePath||'%'")
    suspend fun getAllByVolume(volumePath: String): List<UsbImage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(usbImage: UsbImage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(usbImages: List<UsbImage>)

    @Query("DELETE FROM UsbImage WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM UsbImage")
    suspend fun deleteAll()

    @Query("DELETE FROM UsbImage WHERE bucketPath like :volumePath||'%'")
    suspend fun deleteAllByVolume(volumePath: String)
}