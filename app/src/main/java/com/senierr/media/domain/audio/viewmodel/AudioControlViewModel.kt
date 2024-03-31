package com.senierr.media.domain.audio.viewmodel

import android.content.ComponentName
import android.service.media.MediaBrowserService
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.senierr.base.support.arch.viewmodel.BaseViewModel
import com.senierr.base.support.arch.viewmodel.state.UIState
import com.senierr.base.util.LogUtil
import com.senierr.media.SessionApplication
import com.senierr.media.repository.entity.LocalFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive

/**
 * 音乐控制
 *
 * @author senierr
 * @date 2024/3/31
 */
class AudioControlViewModel : BaseViewModel() {

    data class Progress(val position: Long, val duration: Long)

    // 当前播放音频
    private val _playingItem = MutableStateFlow<MediaItem?>(null)
    val playingItem = _playingItem.asStateFlow()
    // 播放列表
    private val _playingList = MutableStateFlow<UIState<List<LocalFile>>>(UIState.Empty)
    val playingList = _playingList.asStateFlow()
    // 播放状态
    private val _playStatus = MutableStateFlow(false)
    val playStatus = _playStatus.asStateFlow()
    // 播放进度
    private val _progress = MutableStateFlow(Progress(0, 100))
    val progress = _progress.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    // 媒体控制器
    private var mediaController: MediaController? = null

    override fun onCleared() {
        super.onCleared()
        LogUtil.logD(TAG, "onCleared")
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }

    /**
     * 初始化
     */
    fun initialize() {
        val context = SessionApplication.getInstance()
        val sessionToken = SessionToken(context, ComponentName(context, MediaBrowserService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            LogUtil.logD(TAG, "onConnected")
            mediaController = controllerFuture?.get()
            mediaController?.let { initMediaController(it) }
        }, MoreExecutors.directExecutor())
    }

    /**
     * 初始化媒体控制器
     */
    private fun initMediaController(controller: MediaController) {
        LogUtil.logD(TAG, "initMediaController")
        controller.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                LogUtil.logD(TAG, "onMediaItemTransition: ${mediaItem?.mediaMetadata?.title}")
                _playingItem.tryEmit(mediaItem)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                LogUtil.logD(TAG, "onIsPlayingChanged: $isPlaying")
                _playStatus.tryEmit(isPlaying)
                if (isPlaying) { setupProgressListener() }
            }
        })
    }

    /**
     * 启动进度监听
     */
    private fun setupProgressListener() {
        viewModelScope.launchSingle("setupProgressListener") {
            val controller = mediaController
            while (isActive && controller != null && controller.isPlaying) {
                val position = controller.currentPosition
                val duration = controller.duration
                _progress.tryEmit(Progress(position, duration))
                // 延迟刷新
                delay(800)
            }
        }
    }

    /**
     * 播放指定项
     */
    fun play(position: Int) {
        LogUtil.logD(TAG, "play: $position")
        mediaController?.seekToDefaultPosition(position)
    }

    /**
     * 播放
     */
    fun play() {
        LogUtil.logD(TAG, "play")
        mediaController?.play()
    }

    /**
     * 暂停播放
     */
    fun pause() {
        LogUtil.logD(TAG, "pause")
        mediaController?.pause()
    }

    /**
     * 重定位
     *
     * @param position 进度
     */
    fun seekTo(position: Long) {
        LogUtil.logD(TAG, "seekTo: $position")
        mediaController?.seekTo(position)
    }

    /**
     * 是否有上一首
     */
    fun hasPreviousItem(): Boolean {
        return mediaController?.hasPreviousMediaItem()?: false
    }

    /**
     * 上一首
     */
    fun skipToPrevious() {
        LogUtil.logD(TAG, "skipToPrevious")
        mediaController?.seekToPreviousMediaItem()
    }

    /**
     * 是否有下一首
     */
    fun hasNextItem(): Boolean {
        return mediaController?.hasNextMediaItem()?: false
    }

    /**
     * 下一首
     */
    fun skipToNext() {
        LogUtil.logD(TAG, "skipToNext")
        mediaController?.seekToNextMediaItem()
    }

    /**
     * 是否正在播放
     */
    fun isPlaying(): Boolean {
        return mediaController?.isPlaying?: false
    }
}