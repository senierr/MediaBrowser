package com.pateo.module.usb.domain.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pateo.module.usb.UsbApplication
import com.senierr.base.util.LogUtil
import com.senierr.media.repository.MediaRepository
import com.senierr.media.repository.entity.LocalFile
import com.senierr.media.repository.entity.UsbStatus
import com.senierr.media.repository.entity.VolumeInfo
import com.senierr.media.repository.service.api.IMediaService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * U盘首页
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class UsbMainViewModel : ViewModel() {

    companion object {
        private const val TAG = "UsbMainViewModel"
    }

    // 当前挂载盘状态
    private val _usbStatus = MutableSharedFlow<UsbStatus>(1)
    val usbStatus = _usbStatus.asSharedFlow()
    // 当前浏览目录
    private val _currentFolderPath = MutableSharedFlow<String?>(1)
    val currentFolderPath = _currentFolderPath.asSharedFlow()
    // 当前目录下数据
    private val _usbFiles = MutableSharedFlow<List<LocalFile>>(1)
    val usbFiles = _usbFiles.asSharedFlow()

    private val mediaService: IMediaService = MediaRepository.getService()

    // 当前挂载盘
    private var currentVolumeInfo: VolumeInfo? = null

    // 拉取U盘数据任务
    private var fetchUsbFilesJob: Job? = null

    // USB挂载监听广播
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "usbReceiver - action: ${intent?.action}, path: ${intent?.data?.path}")
            when (intent?.action) {
                Intent.ACTION_MEDIA_MOUNTED -> {
                    _usbStatus.tryEmit(UsbStatus(UsbStatus.ACTION_MOUNTED, ""))
                }
                Intent.ACTION_MEDIA_SCANNER_STARTED -> {
                    if (currentVolumeInfo != null) {
                        _usbStatus.tryEmit(UsbStatus(UsbStatus.ACTION_SCANNER_STARTED, ""))
                    }
                }
                Intent.ACTION_MEDIA_SCANNER_FINISHED -> {
                    if (currentVolumeInfo != null) {
                        _usbStatus.tryEmit(UsbStatus(UsbStatus.ACTION_SCANNER_FINISHED, ""))
                    }
                }
                Intent.ACTION_MEDIA_EJECT -> {
                    _usbStatus.tryEmit(UsbStatus(UsbStatus.ACTION_EJECT, ""))
                    // 先取消正在执行的任务
                    fetchUsbFilesJob?.cancel()
                    currentVolumeInfo = null
                    _currentFolderPath.tryEmit(null)
                    _usbFiles.tryEmit(emptyList())
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
            }
        }
    }

    init {
        LogUtil.logD(TAG, "init")
        // 注册U盘状态监听
        val usbFilter = IntentFilter()
        usbFilter.addAction(Intent.ACTION_MEDIA_MOUNTED)
        usbFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED)
        usbFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED)
        usbFilter.addAction(Intent.ACTION_MEDIA_EJECT)
        usbFilter.addDataScheme("file")
        UsbApplication.getContext().registerReceiver(usbReceiver, usbFilter)
        // 媒体文件变更监听
        UsbApplication.getContext()
            .contentResolver
            .registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, contentObserver)
    }

    override fun onCleared() {
        super.onCleared()
        UsbApplication.getContext().unregisterReceiver(usbReceiver)
        UsbApplication.getContext().contentResolver.unregisterContentObserver(contentObserver)
    }

    /**
     * 获取当前挂载盘信息
     */
    fun getVolume(): VolumeInfo? = currentVolumeInfo

    /**
     * 拉取U盘数据
     */
    fun fetchUsbFiles(folderPath: String) {
        fetchUsbFilesJob?.cancel()
        fetchUsbFilesJob = viewModelScope.launch {
            val volumePath = currentVolumeInfo?.path
            LogUtil.logD(TAG, "fetchUsbFiles: $folderPath, $volumePath")
            if (folderPath.isBlank() || volumePath.isNullOrBlank()) return@launch
            _currentFolderPath.emit(folderPath)
//            if (folderPath == volumePath) {
//                // 当前是U盘根目录
//                val usbFolders = usbService.fetchUsbFilesByVolume(
//                    volumePath, true, false, false, false
//                ).sort { it.displayName }
//                val usbMedias = usbService.fetchUsbFilesByBucket(volumePath, false)
//                    .sort { it.displayName }
//                // 封装返回数据
//                val usbFiles = usbFolders + usbMedias
//                LogUtil.logD(TAG, "fetchUsbFilesByVolume success: ${usbFiles.size}")
//                _usbFiles.emit(usbFiles)
//            } else {
//                // 当前是子目录
//                val usbMedias = usbService.fetchUsbFilesByBucket(folderPath, false).sort { it.displayName }
//                LogUtil.logD(TAG, "fetchUsbFilesByBucket success: ${usbMedias.size}")
//                _usbFiles.emit(usbMedias)
//            }
        }
    }
}