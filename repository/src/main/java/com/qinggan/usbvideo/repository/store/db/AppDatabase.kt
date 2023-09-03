package com.qinggan.usbvideo.repository.store.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.qinggan.usbvideo.repository.store.db.dao.UsbAudioDao
import com.qinggan.usbvideo.repository.store.db.dao.UsbFolderDao
import com.qinggan.usbvideo.repository.store.db.dao.UsbImageDao
import com.qinggan.usbvideo.repository.store.db.dao.UsbVideoDao
import com.qinggan.usbvideo.repository.entity.UsbAudio
import com.qinggan.usbvideo.repository.entity.UsbFolder
import com.qinggan.usbvideo.repository.entity.UsbImage
import com.qinggan.usbvideo.repository.entity.UsbVideo

/**
 * 数据库入口
 *
 * @author senierr_zhou
 * @date 2021/07/15
 */
@Database(entities = [UsbAudio::class, UsbImage::class, UsbVideo::class, UsbFolder::class],
    version = DatabaseManager.DB_VERSION, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getUsbAudioDao(): UsbAudioDao
    abstract fun getUsbImageDao(): UsbImageDao
    abstract fun getUsbVideoDao(): UsbVideoDao
    abstract fun getUsbFolderDao(): UsbFolderDao
}