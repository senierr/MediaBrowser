package com.qinggan.usbvideo.repository.store.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.qinggan.usbvideo.repository.entity.UsbAudio

/**
 * U盘音乐
 *
 * @author senierr_zhou
 * @date 2021/07/15
 */
@Dao
interface UsbAudioDao {
    @Query("SELECT * FROM UsbAudio WHERE id = :id")
    suspend fun get(id: String): UsbAudio?

    @Query("SELECT * FROM UsbAudio")
    suspend fun getAll(): List<UsbAudio>

    @Query("SELECT * FROM UsbAudio WHERE bucketPath = :bucketPath")
    suspend fun getAllByBucket(bucketPath: String): List<UsbAudio>

    @Query("SELECT * FROM UsbAudio WHERE bucketPath like :volumePath||'%'")
    suspend fun getAllByVolume(volumePath: String): List<UsbAudio>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(usbAudio: UsbAudio)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(usbAudios: List<UsbAudio>)

    @Query("DELETE FROM UsbAudio WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM UsbAudio")
    suspend fun deleteAll()

    @Query("DELETE FROM UsbAudio WHERE bucketPath like :volumePath||'%'")
    suspend fun deleteAllByVolume(volumePath: String)
}