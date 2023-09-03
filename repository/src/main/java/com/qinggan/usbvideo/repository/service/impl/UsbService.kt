package com.qinggan.usbvideo.repository.service.impl

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import com.qinggan.usbvideo.repository.UsbRepository
import com.qinggan.usbvideo.repository.entity.UsbAudio
import com.qinggan.usbvideo.repository.entity.UsbFile
import com.qinggan.usbvideo.repository.entity.UsbFolder
import com.qinggan.usbvideo.repository.entity.UsbImage
import com.qinggan.usbvideo.repository.entity.UsbVideo
import com.qinggan.usbvideo.repository.entity.VolumeInfo
import com.qinggan.usbvideo.repository.service.api.IUsbService
import com.qinggan.usbvideo.repository.store.db.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File


/**
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class UsbService(private val context: Context) : IUsbService {

    companion object {
        private const val TAG = "UsbService"
    }

    private val usbAudioDao by lazy { DatabaseManager.getDatabase().getUsbAudioDao() }
    private val usbImageDao by lazy { DatabaseManager.getDatabase().getUsbImageDao() }
    private val usbVideoDao by lazy { DatabaseManager.getDatabase().getUsbVideoDao() }
    private val usbFolderDao by lazy { DatabaseManager.getDatabase().getUsbFolderDao() }

    override suspend fun fetchUsbVolumes(): List<VolumeInfo> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "fetchUsbVolumes start")
            val result = mutableListOf<VolumeInfo>()
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            storageManager.storageVolumes
                .filter {
                    // 过滤Removable(包含外置sd卡，usb等)且已经装载时
                    it.isRemovable && it.state == Environment.MEDIA_MOUNTED
            }
                .forEach { volume ->

                    Log.d(TAG, "volume: $volume")

                try {
                    val volumeClass = Class.forName(volume.javaClass.name)
                    val getPath = volumeClass.getDeclaredMethod("getPath", *emptyArray())
                    getPath.isAccessible = true
                    val path = getPath.invoke(volume) as String?
                    if (!path.isNullOrBlank()) {
                        val volumeInfo = VolumeInfo(
                            volume.uuid, path, volume.getDescription(context), volume.state, volume.isRemovable
                        )
                        result.add(volumeInfo)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // 测试用
            val volumeInfo = VolumeInfo(
                null, Environment.getExternalStorageDirectory().path,
                "内部存储",
                Environment.MEDIA_MOUNTED,
                true
            )
            result.add(volumeInfo)
            Log.d(TAG, "fetchUsbVolumes end: $result")

            return@withContext result
        }
    }

    override suspend fun syncUsbFiles(
        volumePath: String,
        includeAudio: Boolean,
        includeImage: Boolean,
        includeVideo: Boolean
    ) {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "syncUsbFiles start: $volumePath")
            // 查询媒体数据库
            val usbAudios = mutableListOf<UsbAudio>()
            val usbImages = mutableListOf<UsbImage>()
            val usbVideos = mutableListOf<UsbVideo>()
            val usbFolders = mutableMapOf<String, UsbFolder>()
            val folderFun = object : ((UsbFile) -> UsbFolder?) {
                override fun invoke(item: UsbFile): UsbFolder? {
                    val itemPath = item.path
                    if (itemPath.isBlank()) return null
                    val path = itemPath.substringBeforeLast(File.separatorChar)
                    val displayName = path.substringAfterLast(File.separatorChar)
                    val bucketPath = path.substringBeforeLast(File.separatorChar)
                    if (!usbFolders.contains(path)) {
                        usbFolders[path] = UsbFolder(1, path, displayName, bucketPath)
                    }
                    return usbFolders[path]
                }
            }
            if (includeAudio) {
                usbAudios.addAll(fetchUsbAudiosFromDevice(volumePath))
                usbAudios.forEach { item ->
                    folderFun.invoke(item)?.apply { audioCount++ }
                }
            }
            if (includeImage) {
                usbImages.addAll(fetchUsbImagesFromDevice(volumePath))
                usbImages.forEach { item ->
                    folderFun.invoke(item)?.apply { imageCount++ }
                }
            }
            if (includeVideo) {
                usbVideos.addAll(fetchUsbVideosFromDevice(volumePath))
                usbVideos.forEach { item ->
                    folderFun.invoke(item)?.apply { videoCount++ }
                }
            }
            // 缓存至应用数据库
            usbAudioDao.insertAll(usbAudios)
            usbImageDao.insertAll(usbImages)
            usbVideoDao.insertAll(usbVideos)
            usbFolderDao.insertAll(usbFolders.values.toList())

            Log.d(TAG, "syncUsbFiles success: " +
                    "folders(${usbFolders.values.size}), " +
                    "audios(${usbAudios.size}), " +
                    "images(${usbImages.size}), " +
                    "videos(${usbVideos.size})")
        }
    }

    override suspend  fun clear(volumePath: String?) {
        Log.d(TAG, "clear: $volumePath")
        if (volumePath.isNullOrBlank()) {
            usbAudioDao.deleteAll()
            usbImageDao.deleteAll()
            usbVideoDao.deleteAll()
            usbFolderDao.deleteAll()
        } else {
            usbAudioDao.deleteAllByVolume(volumePath)
            usbImageDao.deleteAllByVolume(volumePath)
            usbVideoDao.deleteAllByVolume(volumePath)
            usbFolderDao.deleteAllByVolume(volumePath)
        }
    }

    override suspend fun fetchUsbFilesByBucket(
        bucketPath: String,
        includeFolder: Boolean,
        includeAudio: Boolean,
        includeImage: Boolean,
        includeVideo: Boolean
    ): List<UsbFile> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "fetchUsbFilesByBucket: $bucketPath")
            val result = mutableListOf<UsbFile>()
            if (includeFolder) {
                result.addAll(usbFolderDao.getAllByBucket(bucketPath))
            }
            if (includeAudio) {
                result.addAll(usbAudioDao.getAllByBucket(bucketPath))
            }
            if (includeImage) {
                result.addAll(usbImageDao.getAllByBucket(bucketPath))
            }
            if (includeVideo) {
                result.addAll(usbVideoDao.getAllByBucket(bucketPath))
            }
            Log.d(TAG, "fetchUsbFilesByBucket success: ${result.size}")
            return@withContext result
        }
    }

    override suspend fun fetchUsbFilesByVolume(
        volumePath: String,
        includeFolder: Boolean,
        includeAudio: Boolean,
        includeImage: Boolean,
        includeVideo: Boolean
    ): List<UsbFile> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "fetchUsbFilesByVolume: $volumePath")
            val result = mutableListOf<UsbFile>()
            if (includeFolder) {
                result.addAll(usbFolderDao.getAllByVolume(volumePath))
            }
            if (includeAudio) {
                result.addAll(usbAudioDao.getAllByVolume(volumePath))
            }
            if (includeImage) {
                result.addAll(usbImageDao.getAllByVolume(volumePath))
            }
            if (includeVideo) {
                result.addAll(usbVideoDao.getAllByVolume(volumePath))
            }
            Log.d(TAG, "fetchUsbFilesByVolume success: ${result.size}")
            return@withContext result
        }
    }

    /**
     * 从媒体数据库拉取U盘音频数据
     *
     * @param volumePath 挂载盘目录。默认空，不区分。
     */
    private suspend fun fetchUsbAudiosFromDevice(volumePath: String? = null): List<UsbAudio> {
        return withContext(Dispatchers.IO) {
            val context = UsbRepository.getApplication()
            val result = mutableListOf<UsbAudio>()
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.MIME_TYPE
            )
            val selection = if (volumePath.isNullOrBlank()) {
                null
            } else {
                "${MediaStore.Audio.Media.DATA} like ?"
            }
            val selectionArgs = if (volumePath.isNullOrBlank()) {
                null
            } else {
                arrayOf("$volumePath${File.separatorChar}%")
            }
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                while (isActive && cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                    val data = cursor.getString(dataColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)

                    if (displayName.isNotEmpty() && data.isNotEmpty() && mimeType.isNotEmpty()) {
                        val usbAudio = UsbAudio(
                            id, data, displayName, data.substringBeforeLast(File.separatorChar), mimeType
                        )
                        result.add(usbAudio)
                        Log.d(TAG, "Add audio: $usbAudio")
                    } else {
                        Log.w(TAG, "Add audio error: $id, $displayName, $data, $mimeType")
                    }
                }
            }
            Log.d(TAG, "Add audio success: ${result.size}")
            return@withContext result
        }
    }

    /**
     * 从媒体数据库拉取U盘图片数据
     *
     * @param volumePath 挂载盘目录。默认空，不区分。
     */
    private suspend fun fetchUsbImagesFromDevice(volumePath: String? = null): List<UsbImage> {
        return withContext(Dispatchers.IO) {
            val context = UsbRepository.getApplication()
            val result = mutableListOf<UsbImage>()
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.MIME_TYPE
            )
            val selection = if (volumePath.isNullOrBlank()) {
                null
            } else {
                "${MediaStore.Images.Media.DATA} like ?"
            }
            val selectionArgs = if (volumePath.isNullOrBlank()) {
                null
            } else {
                arrayOf("$volumePath${File.separatorChar}%")
            }
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                while (isActive && cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                    val data = cursor.getString(dataColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)

                    if (displayName.isNotEmpty() && data.isNotEmpty() && mimeType.isNotEmpty()) {
                        val usbImage = UsbImage(
                            id, data, displayName, data.substringBeforeLast(File.separatorChar), mimeType
                        )
                        result.add(usbImage)
                        Log.d(TAG, "Add image: $usbImage")
                    } else {
                        Log.w(TAG, "Add image error: $id, $displayName, $data, $mimeType")
                    }
                }
            }
            Log.d(TAG, "Add image success: ${result.size}")
            return@withContext result
        }
    }

    /**
     * 从媒体数据库拉取U盘视频数据
     *
     * @param volumePath 挂载盘目录。默认空，不区分。
     */
    private suspend fun fetchUsbVideosFromDevice(volumePath: String? = null): List<UsbVideo> {
        return withContext(Dispatchers.IO) {
            val context = UsbRepository.getApplication()
            val result = mutableListOf<UsbVideo>()
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.MIME_TYPE
            )
            val selection = if (volumePath.isNullOrBlank()) {
                null
            } else {
                "${MediaStore.Video.Media.DATA} like ?"
            }
            val selectionArgs = if (volumePath.isNullOrBlank()) {
                null
            } else {
                arrayOf("$volumePath${File.separatorChar}%")
            }
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                while (isActive && cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                    val data = cursor.getString(dataColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)

                    if (displayName.isNotEmpty() && data.isNotEmpty() && mimeType.isNotEmpty()) {
                        val usbVideo = UsbVideo(
                            id, data, displayName, data.substringBeforeLast(File.separatorChar), mimeType
                        )
                        result.add(usbVideo)
                        Log.d(TAG, "Add video: $usbVideo")
                    } else {
                        Log.w(TAG, "Add video error: $id, $displayName, $data, $mimeType")
                    }
                }
            }
            Log.d(TAG, "Add video success: ${result.size}")
            return@withContext result
        }
    }
}