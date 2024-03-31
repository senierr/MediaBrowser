package com.senierr.media.domain.home.viewmodel

import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.senierr.base.support.arch.viewmodel.BaseViewModel
import com.senierr.base.support.arch.viewmodel.state.UIState
import com.senierr.base.support.ktx.runCatchSilent
import com.senierr.base.util.LogUtil
import com.senierr.media.SessionApplication
import com.senierr.media.repository.MediaRepository
import com.senierr.media.repository.entity.LocalFile
import com.senierr.media.repository.service.api.IMediaService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 图片首页
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class ImageViewModel : BaseViewModel() {

    // 当前目录
    private val _currentFolder = MutableStateFlow<String>(Environment.getExternalStorageDirectory().path)
    val currentFolder = _currentFolder.asStateFlow()
    // 当前目录下文件夹+数据
    private val _localFiles = MutableStateFlow<UIState<List<LocalFile>>>(UIState.Empty)
    val localFiles = _localFiles.asStateFlow()

    private val mediaService: IMediaService = MediaRepository.getService()

    // 媒体文件变更监听
    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            if (MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString() == uri?.toString()) {
                Log.d(TAG, "ContentObserver - onChange: $selfChange, $uri")
            }
        }
    }

    init {
        LogUtil.logD(TAG, "init")
        // 媒体文件变更监听
        SessionApplication.getInstance().contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, contentObserver)
    }

    override fun onCleared() {
        super.onCleared()
        SessionApplication.getInstance().contentResolver.unregisterContentObserver(contentObserver)
    }

    /**
     * 获取根目录地址
     */
    fun getRootFolderPath(): String? {
        return Environment.getExternalStorageDirectory().path
    }

    /**
     * 拉取媒体数据（文件夹 + 数据）
     */
    fun fetchLocalFiles(bucketPath: String) {
        viewModelScope.launchSingle("fetchLocalFiles") {
            LogUtil.logD(TAG, "fetchLocalFiles: $bucketPath")
            if (bucketPath.isBlank()) return@launchSingle
            runCatchSilent({
                _currentFolder.emit(bucketPath)
                val images = mediaService.fetchLocalImages(bucketPath, false)
                val folders = mediaService.fetchLocalFoldersWithImage(bucketPath)
                LogUtil.logD(TAG, "fetchLocalFiles success: ${folders.size} + ${images.size}")
                _localFiles.emit(UIState.Content(folders + images))
            }, {
                LogUtil.logE(TAG, "fetchLocalFiles error: ${Log.getStackTraceString(it)}")
                _localFiles.emit(UIState.Error(it))
            })
        }
    }
}