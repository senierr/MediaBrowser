package com.senierr.media.domain.audio.widget.viewmodel

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.ConnectionCallback
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.senierr.base.support.arch.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 播控
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
abstract class BaseControlViewModel : BaseViewModel() {

    companion object {
        // 重连时间间隔
        private const val INTERNAL_TRY_CONNECT = 3 * 1000L
    }

    data class Progress(val position: Long, val duration: Long)

    // 播放项
    private val _playingItem = MutableStateFlow<MediaMetadataCompat?>(null)
    val playingItem = _playingItem.asStateFlow()
    // 播放状态
    private val _playStatus = MutableStateFlow(false)
    val playStatus = _playStatus.asStateFlow()
    // 播放进度
    private val _progress = MutableStateFlow(Progress(0, 100))
    val progress = _progress.asStateFlow()

    @SuppressLint("StaticFieldLeak")
    private var context: Context? = null
    // 是否已初始化
    private var isInitialized = false
    // 服务是否已连接
    private var isConnected = false
    // 重连次数
    private var tryConnectCount = 0

    private var mediaBrowser: MediaBrowserCompat? = null
    protected var mediaController: MediaControllerCompat? = null
    private val controlCallback = object : MediaControllerCompat.Callback() {
        override fun onSessionReady() {
            onHandleSessionReady()
            onMetadataChanged(mediaController?.metadata)
            onPlaybackStateChanged(mediaController?.playbackState)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            onHandlePlaybackStateChanged(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            onHandleMetadataChanged(metadata)
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            onHandleSessionEvent(event, extras)
        }
    }

    /**
     * 获取组件名
     */
    abstract fun getComponentName(): ComponentName

    /**
     * 初始化
     */
    @Synchronized
    open fun initialize(context: Context) {
        this.context = context
        if (!isInitialized) {
            mediaBrowser = MediaBrowserCompat(context, getComponentName(), object : ConnectionCallback() {
                override fun onConnected() {
                    Log.d(TAG, "onConnected")
                    if (mediaBrowser?.isConnected == true) {
                        val parentId = mediaBrowser?.root ?: return
                        mediaBrowser?.unsubscribe(parentId)
                        val token = mediaBrowser?.sessionToken ?: return
                        isConnected = true
                        mediaController = MediaControllerCompat(context, token)
                        mediaController?.registerCallback(controlCallback, Handler(Looper.getMainLooper()))
                    }
                }

                override fun onConnectionSuspended() {
                    Log.d(TAG, "onConnectionSuspended")
                    isConnected = false
                    tryConnect()
                }

                override fun onConnectionFailed() {
                    Log.d(TAG, "onConnectionFailed")
                    isConnected = false
                    tryConnect()
                }
            }, null)
            isInitialized = true
            tryConnect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared")
        runCatching {
            mediaController?.unregisterCallback(controlCallback)
            mediaBrowser?.disconnect()
        }
        context = null
    }

    /**
     * 连接服务。可能会失败，失败后会自动检测，并重新连接
     */
    private fun tryConnect() {
        Log.d(TAG, "tryConnect: $tryConnectCount")
        viewModelScope.launchSingle("tryConnect") {
            runCatching {
                delay(INTERNAL_TRY_CONNECT * tryConnectCount)
                tryConnectCount++
                withContext(Dispatchers.IO) {
                    mediaBrowser?.connect()
                }
                // 连接次数最大为10，超过重置
                if (tryConnectCount > 10) {
                    tryConnectCount = 1
                }
            }
        }
    }

    protected open fun onHandleSessionReady() {
        Log.d(TAG, "onHandleSessionReady")
    }

    /**
     * 处理播放媒体变更
     */
    protected open fun onHandleMetadataChanged(metadata: MediaMetadataCompat?) {
        Log.d(TAG, "onHandleMetadataChanged: ${metadata?.description?.title}")
        _playingItem.tryEmit(metadata)
        _progress.tryEmit(Progress(mediaController?.playbackState?.position?: 0, metadata?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)?: 0))
    }

    /**
     * 处理播放状态变更
     */
    protected open fun onHandlePlaybackStateChanged(state: PlaybackStateCompat?) {
        _playStatus.tryEmit(state?.state == PlaybackStateCompat.STATE_PLAYING)
        _progress.tryEmit(Progress(state?.position?: 0, mediaController?.metadata?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)?: 0))
    }

    /**
     * 处理自定义事件
     */
    protected open fun onHandleSessionEvent(event: String?, extras: Bundle?) {
        Log.d(TAG, "onHandleSessionEvent: $event, $extras")
    }

    /**
     * 播放
     */
    open fun play() {
        Log.d(TAG, "play")
        mediaController?.transportControls?.play()
    }

    /**
     * 暂停播放
     */
    open fun pause() {
        Log.d(TAG, "pause")
        mediaController?.transportControls?.pause()
    }

    /**
     * 上一首
     */
    open fun skipToPrevious() {
        Log.d(TAG, "skipToPrevious")
        mediaController?.transportControls?.skipToPrevious()
    }

    /**
     * 下一首
     */
    open fun skipToNext() {
        Log.d(TAG, "skipToNext")
        mediaController?.transportControls?.skipToNext()
    }

    /**
     * 是否正在播放
     */
    fun isPlaying(): Boolean {
        return _playStatus.value
    }

    /**
     * 发送自定义指令
     */
    fun sendCustomAction(action: String, args: Bundle) {
        Log.d(TAG, "sendCustomAction: $action, $args")
        mediaController?.transportControls?.sendCustomAction(action, args)
    }

    /**
     * 获取点击意图
     */
    fun getSessionActivityIntent(): PendingIntent? {
        return mediaController?.sessionActivity
    }
}