package com.senierr.media.domain.audio.viewmodel

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.exoplayer.ExoPlayer
import com.senierr.base.support.arch.viewmodel.BaseViewModel
import com.senierr.base.util.LogUtil
import com.senierr.media.SessionApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive

/**
 * 播放器控制
 *
 * @author senierr
 * @date 2024/3/31
 */
abstract class BaseControlViewModel<T> : BaseViewModel() {

    data class Progress(val position: Long, val duration: Long)

    sealed interface PlayMode {
        object ONE : PlayMode
        object LIST : PlayMode
        object ALL : PlayMode
        object SHUFFLE : PlayMode
    }

    // 当前播放音频
    private val _playingItem = MutableStateFlow<T?>(null)
    val playingItem = _playingItem.asStateFlow()
    // 播放列表
    private val _playingList = MutableStateFlow<List<T>>(emptyList())
    val playingList = _playingList.asStateFlow()
    // 播放状态
    private val _playStatus = MutableStateFlow(false)
    val playStatus = _playStatus.asStateFlow()
    // 播放进度
    private val _progress = MutableStateFlow(Progress(0, 100))
    val progress = _progress.asStateFlow()
    // 播放模式
    private val _playMode = MutableStateFlow<PlayMode>(PlayMode.LIST)
    val playMode = _playMode.asStateFlow()
    // 播放异常
    private val _playError = MutableSharedFlow<Throwable>()
    val playError = _playError.asSharedFlow()

    // 媒体控制器
    protected lateinit var mediaController: Player

    // 音频焦点状态
    private var audioFocusResult = AudioManager.AUDIOFOCUS_REQUEST_FAILED
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    // 音频焦点变更监听
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        LogUtil.logD(TAG, "onAudioFocusChange: $focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                audioFocusResult = AudioManager.AUDIOFOCUS_REQUEST_FAILED
                pause(false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                audioFocusResult = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                pause(false)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                audioFocusResult = AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                play()
            }
        }
    }

    init {
        initialize()
    }

    /**
     * 实体转为播放项
     */
    abstract fun onItemCovertToMediaItem(item: T): MediaItem

    override fun onCleared() {
        super.onCleared()
        LogUtil.logD(TAG, "onCleared")
        mediaController.release()
    }

    /**
     * 初始化
     */
    private fun initialize() {
        val context = SessionApplication.getInstance()
        // 初始化音频焦点
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        // 初始化播放器
        mediaController = ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.DEFAULT, false)
            .build()
        mediaController.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                LogUtil.logD(TAG, "onMediaItemTransition: ${mediaItem?.mediaMetadata?.title}, $reason")
                viewModelScope.launchSingle("onMediaItemTransition") {
                    val item = playingList.value.find { onItemCovertToMediaItem(it).mediaId == mediaItem?.mediaId }
                    _playingItem.emit(item)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                LogUtil.logD(TAG, "onPlaybackStateChanged: $playbackState")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                LogUtil.logD(TAG, "onIsPlayingChanged: $isPlaying")
                _playStatus.tryEmit(isPlaying)
                if (isPlaying) { setupProgressListener() }
            }

            override fun onPlayerError(error: PlaybackException) {
                LogUtil.logW(TAG, "onPlayerError: $error")
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    mediaController.seekToDefaultPosition()
                    mediaController.playWhenReady = true
                    mediaController.prepare()
                } else {
                    _playError.tryEmit(error)
                }
            }
        })
    }

    /**
     * 启动进度监听
     */
    private fun setupProgressListener() {
        viewModelScope.launchSingle("setupProgressListener") {
            while (isActive && mediaController.isPlaying) {
                val position = mediaController.currentPosition
                val duration = mediaController.duration
                _progress.emit(Progress(position, duration))
                // 延迟刷新
                delay(500)
            }
        }
    }

    /**
     * 设置播放列表
     */
    fun setMediaItems(items: List<T>) {
        LogUtil.logD(TAG, "setMediaItems: ${items.size}")
        val newMediaItems = items.map { onItemCovertToMediaItem(it) }
        val index = newMediaItems.indexOfFirst { it.mediaId == mediaController.currentMediaItem?.mediaId }
        if (index < 0) {
            mediaController.setMediaItems(newMediaItems)
        } else {
            mediaController.removeMediaItems(0, mediaController.currentMediaItemIndex)
            mediaController.removeMediaItems(mediaController.currentMediaItemIndex + 1, mediaController.mediaItemCount)
            mediaController.addMediaItems(0, newMediaItems.subList(0, index))
            mediaController.addMediaItems(newMediaItems.subList(index + 1, newMediaItems.size))
        }
        _playingList.tryEmit(items)
    }

    /**
     * 播放指定项
     */
    fun play(position: Int) {
        LogUtil.logD(TAG, "play: $position")
        // 申请音频焦点
        if (!requestAudioFocus()) {
            LogUtil.logD(TAG, "requestAudioFocus: false")
            return
        }
        if (mediaController.currentMediaItemIndex != position) {
            mediaController.seekToDefaultPosition(position)
        }
        if (mediaController.playbackState == Player.STATE_READY) {
            mediaController.play()
        } else {
            mediaController.playWhenReady = true
            mediaController.prepare()
        }
    }

    /**
     * 播放
     */
    fun play() {
        LogUtil.logD(TAG, "play")
        // 申请音频焦点
        if (!requestAudioFocus()) {
            LogUtil.logD(TAG, "requestAudioFocus: false")
            return
        }
        if (mediaController.playbackState == Player.STATE_READY) {
            mediaController.play()
        } else {
            mediaController.playWhenReady = true
            mediaController.prepare()
        }
    }

    /**
     * 暂停播放
     */
    fun pause(fromUser: Boolean) {
        LogUtil.logD(TAG, "pause: $fromUser")
        mediaController.pause()
    }

    /**
     * 重定位
     *
     * @param position 进度
     */
    fun seekTo(position: Long): Boolean {
        LogUtil.logD(TAG, "seekTo: $position")
        // 申请音频焦点
        if (!requestAudioFocus()) {
            LogUtil.logD(TAG, "requestAudioFocus: false")
            return false
        }
        mediaController.seekTo(position)
        if (mediaController.playbackState == Player.STATE_READY) {
            mediaController.play()
        } else {
            mediaController.playWhenReady = true
            mediaController.prepare()
        }
        return true
    }

    /**
     * 是否有上一首
     */
    fun hasPreviousItem(): Boolean {
        return mediaController.hasPreviousMediaItem()
    }

    /**
     * 上一首
     */
    fun skipToPrevious() {
        LogUtil.logD(TAG, "skipToPrevious")
        // 申请音频焦点
        if (!requestAudioFocus()) {
            LogUtil.logD(TAG, "requestAudioFocus: false")
            return
        }
        mediaController.seekToPreviousMediaItem()
        if (!mediaController.isPlaying) {
            if (mediaController.playbackState == Player.STATE_READY) {
                mediaController.play()
            } else {
                mediaController.playWhenReady = true
                mediaController.prepare()
            }
        }
    }

    /**
     * 是否有下一首
     */
    fun hasNextItem(): Boolean {
        return mediaController.hasNextMediaItem()
    }

    /**
     * 下一首
     */
    fun skipToNext() {
        LogUtil.logD(TAG, "skipToNext")
        // 申请音频焦点
        if (!requestAudioFocus()) {
            LogUtil.logD(TAG, "requestAudioFocus: false")
            return
        }
        mediaController.seekToNextMediaItem()
        if (!mediaController.isPlaying) {
            if (mediaController.playbackState == Player.STATE_READY) {
                mediaController.play()
            } else {
                mediaController.playWhenReady = true
                mediaController.prepare()
            }
        }
    }

    /**
     * 设置播放模式
     */
    fun setPlayMode(playMode: PlayMode) {
        LogUtil.logD(TAG, "setPlayMode: $playMode")
        when (playMode) {
            PlayMode.ONE -> {
                mediaController.repeatMode = REPEAT_MODE_ONE
                mediaController.shuffleModeEnabled = false
            }
            PlayMode.LIST -> {
                mediaController.repeatMode = REPEAT_MODE_OFF
                mediaController.shuffleModeEnabled = false
            }
            PlayMode.ALL -> {
                mediaController.repeatMode = REPEAT_MODE_ALL
                mediaController.shuffleModeEnabled = false
            }
            PlayMode.SHUFFLE -> {
                mediaController.repeatMode = REPEAT_MODE_ALL
                mediaController.shuffleModeEnabled = true
            }
        }
        _playMode.tryEmit(playMode)
    }

    /**
     * 重置播放
     */
    fun reset() {
        LogUtil.logD(TAG, "reset")
        mediaController.clearMediaItems()
        _playingItem.tryEmit(null)
        _playStatus.tryEmit(false)
        _progress.tryEmit(Progress(0, 100))
    }

    /**
     * 是否正在播放
     */
    fun isPlaying(): Boolean {
        return mediaController.isPlaying
    }

    /**
     * 当前播放索引
     */
    fun currentMediaItemIndex(): Int {
        return mediaController.currentMediaItemIndex
    }

    /**
     * 音频焦点申请
     */
    private fun requestAudioFocus(): Boolean {
        if (audioFocusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // 已经申请过，无需再申请
            LogUtil.logD(TAG, "requestAudioFocus: already granted")
            return true
        }

        val audioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .setAcceptsDelayedFocusGain(true)
            .build()
        val request = audioFocusRequest?: return false
        audioFocusResult = audioManager?.requestAudioFocus(request)?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        LogUtil.logD(TAG, "requestAudioFocus: $audioFocusResult")
        return audioFocusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    /**
     * 音频焦点释放
     */
    private fun abandonAudioFocusRequest() {
        val request = audioFocusRequest?: return
        LogUtil.logD(TAG, "abandonAudioFocusRequest: $request")
        audioManager?.abandonAudioFocusRequest(request)
        audioFocusResult = AudioManager.AUDIOFOCUS_REQUEST_FAILED
    }
}