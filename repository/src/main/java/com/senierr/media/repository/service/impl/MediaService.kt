package com.senierr.media.repository.service.impl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import com.senierr.media.local.repository.entity.LocalAudio
import com.senierr.media.local.repository.entity.LocalFolder
import com.senierr.media.local.repository.entity.LocalImage
import com.senierr.media.local.repository.entity.LocalVideo
import com.senierr.media.local.repository.service.api.IMediaService
import com.senierr.media.repository.MediaRepository
import com.senierr.media.repository.entity.UsbStatus
import com.senierr.media.repository.entity.VolumeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class MediaService(private val context: Context) : IMediaService {

    companion object {
        private const val TAG = "UsbService"
    }

    // U盘状态
    private val _usbStatus = MutableStateFlow(UsbStatus(UsbStatus.ACTION_EJECT, null))

    // USB挂载监听广播
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "usbReceiver - action: ${intent?.action}, path: ${intent?.data?.path}")
            val path = intent?.data?.path
            when (intent?.action) {
                Intent.ACTION_MEDIA_MOUNTED -> {
                    _usbStatus.tryEmit(UsbStatus(UsbStatus.ACTION_MOUNTED, path))
                }
                Intent.ACTION_MEDIA_SCANNER_STARTED -> {
                    _usbStatus.tryEmit(UsbStatus(UsbStatus.ACTION_SCANNER_STARTED, path))
                }
                Intent.ACTION_MEDIA_SCANNER_FINISHED -> {
                    _usbStatus.tryEmit(UsbStatus(UsbStatus.ACTION_SCANNER_FINISHED, path))
                }
                Intent.ACTION_MEDIA_EJECT -> {
                    _usbStatus.tryEmit(UsbStatus(UsbStatus.ACTION_EJECT, path))
                }
            }
        }
    }
    // 媒体文件变更监听
    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            if (MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString() == uri?.toString()) {
                Log.d(TAG, "ContentObserver - onChange: $selfChange, $uri")
                // 媒体更新，通知刷新当前文件夹数据
                _usbStatus.tryEmit(UsbStatus(UsbStatus.ACTION_CONTENT_CHANGED, null))
            }
        }
    }

    private var syncUsbFilesJob: Job? = null

    init {
        // 注册U盘状态监听
        val usbFilter = IntentFilter()
        usbFilter.addAction(Intent.ACTION_MEDIA_MOUNTED)
        usbFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED)
        usbFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED)
        usbFilter.addAction(Intent.ACTION_MEDIA_EJECT)
        usbFilter.addDataScheme("file")
        context.registerReceiver(usbReceiver, usbFilter)
        // 媒体文件变更监听
        context.contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, contentObserver)
    }

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

    override suspend fun fetchLocalFoldersWithImage(bucketPath: String): List<LocalFolder> {
        return withContext(Dispatchers.IO) {
            val localImages = fetchLocalImages(bucketPath)
            val localFolders = mutableMapOf<String, LocalFolder>()
            localImages.filter {
                it.path.isNotBlank()
            }.forEach {
                val path = it.path.substringBeforeLast(File.separatorChar)
                if (!localFolders.contains(path)) {
                    val displayName = path.substringAfterLast(File.separatorChar)
                    val folderBucketPath = path.substringBeforeLast(File.separatorChar)
                    localFolders[path] = LocalFolder(path, displayName, folderBucketPath)
                }
                localFolders[path]?.apply { imageCount++ }
            }
            return@withContext localFolders.values.toList()
        }
    }

    override suspend fun fetchLocalFoldersWithAudio(bucketPath: String): List<LocalFolder> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchLocalFoldersWithVideo(bucketPath: String): List<LocalFolder> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchLocalImages(bucketPath: String, includeSubfolder: Boolean): List<LocalImage> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<LocalImage>()
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.MIME_TYPE
            )
            val selection = if (includeSubfolder) {
                "${MediaStore.Images.Media.DATA} like ?"
            } else {
                "${MediaStore.Images.Media.DATA} like ? and ${MediaStore.Images.Media.DATA} not like ?"
            }
            val selectionArgs = if (includeSubfolder) {
                arrayOf("$bucketPath${File.separatorChar}%")
            } else {
                arrayOf("$bucketPath${File.separatorChar}%", "$bucketPath${File.separatorChar}%${File.separatorChar}%")
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
                        val localImage = LocalImage(
                            id, data, displayName, data.substringBeforeLast(File.separatorChar), mimeType
                        )
                        result.add(localImage)
                        Log.d(TAG, "Add image: $localImage")
                    } else {
                        Log.w(TAG, "Add image error: $id, $displayName, $data, $mimeType")
                    }
                }
            }
            Log.d(TAG, "Add image success: ${result.size}")
            return@withContext result
        }
    }

    override suspend fun fetchLocalAudios(bucketPath: String, includeSubfolder: Boolean): List<LocalAudio> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<LocalAudio>()
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.MIME_TYPE
            )
            val selection = if (includeSubfolder) {
                "${MediaStore.Audio.Media.DATA} like ?"
            } else {
                "${MediaStore.Audio.Media.DATA} like ? and ${MediaStore.Audio.Media.DATA} not like ?"
            }
            val selectionArgs = if (includeSubfolder) {
                arrayOf("$bucketPath${File.separatorChar}%")
            } else {
                arrayOf("$bucketPath${File.separatorChar}%", "$bucketPath${File.separatorChar}%${File.separatorChar}%")
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
                        val localAudio = LocalAudio(
                            id, data, displayName, data.substringBeforeLast(File.separatorChar), mimeType
                        )
                        result.add(localAudio)
                        Log.d(TAG, "Add audio: $localAudio")
                    } else {
                        Log.w(TAG, "Add audio error: $id, $displayName, $data, $mimeType")
                    }
                }
            }
            Log.d(TAG, "Add audio success: ${result.size}")
            return@withContext result
        }
    }

    override suspend fun fetchLocalVideos(bucketPath: String, includeSubfolder: Boolean): List<LocalVideo> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<LocalVideo>()
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.MIME_TYPE
            )
            val selection = if (includeSubfolder) {
                "${MediaStore.Video.Media.DATA} like ?"
            } else {
                "${MediaStore.Video.Media.DATA} like ? and ${MediaStore.Video.Media.DATA} not like ?"
            }
            val selectionArgs = if (includeSubfolder) {
                arrayOf("$bucketPath${File.separatorChar}%")
            } else {
                arrayOf("$bucketPath${File.separatorChar}%", "$bucketPath${File.separatorChar}%${File.separatorChar}%")
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
                        val localVideo = LocalVideo(
                            id, data, displayName, data.substringBeforeLast(File.separatorChar), mimeType
                        )
                        result.add(localVideo)
                        Log.d(TAG, "Add video: $localVideo")
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