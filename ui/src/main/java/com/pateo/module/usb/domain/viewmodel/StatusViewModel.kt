package com.pateo.module.usb.domain.viewmodel

import android.app.Application
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
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 车机状态
 *
 * @author senierr_zhou
 * @date 2021/07/13
 */
class StatusViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "StatusViewModel"

        const val USB_STATUS_MOUNTED = 0
        const val USB_STATUS_MEDIA_SCAN_STARTED = 1
        const val USB_STATUS_MEDIA_SCAN_FINISHED = 2
        const val USB_STATUS_MEDIA_CONTENT_CHANGED = 3
        const val USB_STATUS_EJECT = 4

        const val QG_ACTION_SCANNER_STARTED = "QG.android.intent.action.MEDIA_SCANNER_STARTED"
        const val QG_ACTION_SCANNER_FINISHED ="QG.android.intent.action.MEDIA_SCANNER_FINISHED"
    }

    // 点击事件
    data class ShortClick(val keyCode: Int, val keyEvent: KeyEvent?)
    // 长按事件
    data class LongPress(val keyCode: Int, val keyEvent: KeyEvent?, val start: Boolean)

    // U盘状态
    private val _usbStatus = MutableSharedFlow<Int>(replay = 1)
    val usbStatus = _usbStatus.asSharedFlow()
    // 待机
    private val _standby = MutableSharedFlow<Boolean>(replay = 1)
    val standby = _standby.asSharedFlow()
    // 熄屏
    private val _screenOff = MutableSharedFlow<Boolean>(replay = 1)
    val screenOff = _screenOff.asSharedFlow()
    // 360全景
    private val _panoramic = MutableSharedFlow<Boolean>(replay = 1)
    val panoramic = _panoramic.asSharedFlow()
    // 行车速度
    private val _drivingSpeed = MutableSharedFlow<Int>(replay = 1)
    val drivingSpeed = _drivingSpeed.asSharedFlow()
    // 电压状态(true: 正常; false: 异常)
    private val _batteryState = MutableSharedFlow<Boolean>()
    val batteryState = _batteryState.asSharedFlow()
    // 投屏
    private val _castDisplay = MutableSharedFlow<Boolean>()
    val castDisplay = _castDisplay.asSharedFlow()
    // 方控按键
    private val _shortClick = MutableSharedFlow<ShortClick>()
    val shortClick = _shortClick.asSharedFlow()
    private val _longPress = MutableSharedFlow<LongPress>()
    val longPress = _longPress.asSharedFlow()

    // 是否忽略行车警告
    var ignoreDrivingWarning = false

    // USB挂载监听广播
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "usbReceiver - action: ${intent?.action}, path: ${intent?.data?.path}")
            when (intent?.action) {
                Intent.ACTION_MEDIA_MOUNTED -> {
                    _usbStatus.tryEmit(USB_STATUS_MOUNTED)
                }
                QG_ACTION_SCANNER_STARTED -> {
                    _usbStatus.tryEmit(USB_STATUS_MEDIA_SCAN_STARTED)
                }
                QG_ACTION_SCANNER_FINISHED -> {
                    _usbStatus.tryEmit(USB_STATUS_MEDIA_SCAN_FINISHED)
                }
                Intent.ACTION_MEDIA_EJECT -> {
                    _usbStatus.tryEmit(USB_STATUS_EJECT)
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
                _usbStatus.tryEmit(USB_STATUS_MEDIA_CONTENT_CHANGED)
            }
        }
    }
    // 音量控制
    // 亮度调节
    // 系统事件监听
    // 360全景监听
    // 车速监听
    // 投屏

    // 方控按键

    init {
        Log.d(TAG, "init")
        // 注册U盘状态监听
        val usbFilter = IntentFilter()
        usbFilter.addAction(Intent.ACTION_MEDIA_MOUNTED)
        usbFilter.addAction(QG_ACTION_SCANNER_STARTED)
        usbFilter.addAction(QG_ACTION_SCANNER_FINISHED)
//        usbFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED)
//        usbFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED)
        usbFilter.addAction(Intent.ACTION_MEDIA_EJECT)
        usbFilter.addDataScheme("file")
        application.registerReceiver(usbReceiver, usbFilter)
        // 媒体文件变更监听
        application.contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, contentObserver)
        // 音量
        // 灯光
        // 注册待机监听
        // 注册熄屏监听
        // 注册电压监听
        // 360全景监听
        // 车速监听
        // 投屏
        // 方控按键
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared")
        val context = getApplication<Application>()
        context.unregisterReceiver(usbReceiver)
        context.contentResolver.unregisterContentObserver(contentObserver)
        super.onCleared()
    }

    /**
     * 音量调节
     */
    fun adjustVolume(isIncrease: Boolean) {

    }

    /**
     * 亮度调节
     */
    fun adjustBrightness(isIncrease: Boolean) {

    }

    /**
     * 获取当前车速
     */
    fun getCarSpeed(): Int {
        return 0
    }
}