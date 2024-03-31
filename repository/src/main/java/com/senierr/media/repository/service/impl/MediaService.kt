package com.senierr.media.repository.service.impl

import android.annotation.SuppressLint
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
import android.widget.Toast
import com.senierr.media.repository.entity.LocalAudio
import com.senierr.media.repository.entity.LocalFolder
import com.senierr.media.repository.entity.LocalImage
import com.senierr.media.repository.entity.LocalVideo
import com.senierr.media.repository.service.api.IMediaService
import com.senierr.media.repository.entity.UsbStatus
import com.senierr.media.repository.entity.VolumeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
@SuppressLint("UnspecifiedRegisterReceiverFlag")
class MediaService(private val context: Context) : IMediaService {

    companion object {
        private const val TAG = "UsbService"
    }

    // 盘符状态
    private val _volumeStatus = MutableSharedFlow<VolumeInfo>()

    // USB挂载监听广播
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "usbReceiver - action: ${intent?.action}, path: ${intent?.data?.path}")
            val path = intent?.data?.path?: return
            _volumeStatus.tryEmit(VolumeInfo(path, intent.action?: Intent.ACTION_MEDIA_CHECKING))
            Toast.makeText(context, intent.action, Toast.LENGTH_SHORT).show()
        }
    }
    // 媒体文件变更监听
    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            if (MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString() == uri?.toString()) {
                Log.d(TAG, "ContentObserver - onChange: $selfChange, $uri")
                // 媒体更新，通知刷新当前文件夹数据
//                _volumeStatus.tryEmit(VolumeInfo(UsbStatus.ACTION_CONTENT_CHANGED, null))
            }
        }
    }

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

    override suspend fun fetchVolumeInfoList(): List<VolumeInfo> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<VolumeInfo>()
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA
            )
            val selection = "${MediaStore.Video.Media.DATA} not like ?"
            val selectionArgs = arrayOf("${Environment.getExternalStorageDirectory().path}%")
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                while (isActive && cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                    val data = cursor.getString(dataColumn)

                    if (displayName.isNotEmpty() && data.isNotEmpty()) {
//                        val volumeInfo = VolumeInfo(
//                            id, data, displayName, data.substringBeforeLast(File.separatorChar), mimeType
//                        )
//                        result.add(volumeInfo)
                        Log.d(TAG, "Add image: $data")
                    } else {
                        Log.w(TAG, "Add image error: $id, $displayName, $data")
                    }
                }
            }
            Log.d(TAG, "Add image success: ${result.size}")
            return@withContext result
        }
    }

    override suspend fun observeVolumeStatus(): SharedFlow<VolumeInfo> {
        return _volumeStatus.asSharedFlow()
    }

    override suspend fun fetchLocalFoldersWithImage(bucketPath: String): List<LocalFolder> {
        return withContext(Dispatchers.IO) {
            val localImages = fetchLocalImages(bucketPath, true)
            val localFolders = mutableMapOf<String, LocalFolder>()
            localImages.filter { it.path.isNotBlank() } // 过滤无效数据
                .map { it.path.substringBeforeLast(File.separatorChar) } // 转换为文件夹路径
                .filter { it != bucketPath } // 移除查询文件夹本身
                .forEach { path ->
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
        return withContext(Dispatchers.IO) {
            val localAudios = fetchLocalAudios(bucketPath, true)
            val localFolders = mutableMapOf<String, LocalFolder>()
            localAudios.filter { it.path.isNotBlank() } // 过滤无效数据
                .map { it.path.substringBeforeLast(File.separatorChar) } // 转换为文件夹路径
                .filter { it != bucketPath } // 移除查询文件夹本身
                .forEach { path ->
                    if (!localFolders.contains(path)) {
                        val displayName = path.substringAfterLast(File.separatorChar)
                        val folderBucketPath = path.substringBeforeLast(File.separatorChar)
                        localFolders[path] = LocalFolder(path, displayName, folderBucketPath)
                    }
                    localFolders[path]?.apply { audioCount++ }
                }
            return@withContext localFolders.values.toList()
        }
    }

    override suspend fun fetchLocalFoldersWithVideo(bucketPath: String): List<LocalFolder> {
        return withContext(Dispatchers.IO) {
            val localVideos = fetchLocalVideos(bucketPath, true)
            val localFolders = mutableMapOf<String, LocalFolder>()
            localVideos.filter { it.path.isNotBlank() } // 过滤无效数据
                .map { it.path.substringBeforeLast(File.separatorChar) } // 转换为文件夹路径
                .filter { it != bucketPath } // 移除查询文件夹本身
                .forEach { path ->
                    if (!localFolders.contains(path)) {
                        val displayName = path.substringAfterLast(File.separatorChar)
                        val folderBucketPath = path.substringBeforeLast(File.separatorChar)
                        localFolders[path] = LocalFolder(path, displayName, folderBucketPath)
                    }
                    localFolders[path]?.apply { videoCount++ }
                }
            return@withContext localFolders.values.toList()
        }
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