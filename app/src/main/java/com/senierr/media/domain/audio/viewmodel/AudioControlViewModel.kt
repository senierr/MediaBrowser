package com.senierr.media.domain.audio.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.senierr.base.support.arch.viewmodel.BaseViewModel
import com.senierr.base.support.arch.viewmodel.state.UIState
import com.senierr.base.util.LogUtil
import com.senierr.media.SessionApplication
import com.senierr.media.repository.entity.LocalFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    // 播放异常
    private val _playError = MutableSharedFlow<Throwable>()
    val playError = _playError.asSharedFlow()

    // 媒体控制器
    private var mediaController: Player? = null

    init {
        initialize()
    }

    override fun onCleared() {
        super.onCleared()
        LogUtil.logD(TAG, "onCleared")
        mediaController?.release()
    }

    /**
     * 初始化
     */
    private fun initialize() {
        val context = SessionApplication.getInstance()
        mediaController = ExoPlayer.Builder(context).build()
        mediaController?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                LogUtil.logD(TAG, "onMediaItemTransition: ${mediaItem?.mediaMetadata?.title}")
                _playingItem.tryEmit(mediaItem)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                LogUtil.logD(TAG, "onIsPlayingChanged: $isPlaying")
                _playStatus.tryEmit(isPlaying)
                if (isPlaying) { setupProgressListener() }
            }

            override fun onPlayerError(error: PlaybackException) {
                LogUtil.logD(TAG, "onPlayerError: $error")
                _playError.tryEmit(error)
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
                _progress.emit(Progress(position, duration))
                // 延迟刷新
                delay(800)
            }
        }
    }

    /**
     * 获取媒体控制器
     */
    fun getMediaController(): Player? = mediaController
}