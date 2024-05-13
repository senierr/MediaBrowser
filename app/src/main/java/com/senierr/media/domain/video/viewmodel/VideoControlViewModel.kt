package com.senierr.media.domain.video.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.senierr.base.support.arch.UIState
import com.senierr.base.support.coroutine.CoroutineCompat
import com.senierr.base.support.coroutine.ktx.runCatchSilent
import com.senierr.base.util.LogUtil
import com.senierr.media.domain.common.BaseControlViewModel
import com.senierr.media.repository.MediaRepository
import com.senierr.media.repository.entity.LocalVideo
import com.senierr.media.repository.entity.PlaySession
import com.senierr.media.repository.service.api.IMediaService
import com.senierr.media.repository.service.api.IPlayControlService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 视频控制
 *
 * @author senierr
 * @date 2024/3/31
 */
class VideoControlViewModel : BaseControlViewModel<LocalVideo>() {

    enum class PlayType {
        NOT_PLAY,   // 不播放
        AUTO_PLAY,  // 根据之前状态播放
        FORCE_PLAY  // 强制播放
    }

    private val coroutineCompat = CoroutineCompat(viewModelScope)

    // 当前目录下数据
    private val _localVideos = MutableStateFlow<UIState<List<LocalVideo>>>(UIState.Empty)
    val localVideos = _localVideos.asStateFlow()

    private val mediaService: IMediaService = MediaRepository.getService()
    private val playControlService: IPlayControlService = MediaRepository.getService()
    // 当前会话
    private var currentPlaySession: PlaySession = PlaySession()
    // 是否临时暂停状态
    private var isOnPauseStatus = false

    override fun onItemCovertToMediaItem(item: LocalVideo): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(item.displayName)
            .setArtist(item.artist)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setArtworkUri(item.getUri())
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .build()
        return MediaItem.Builder()
            .setMediaId(item.id.toString())
            .setMediaMetadata(metadata)
            .setUri(item.getUri())
            .setTag(item)
            .build()
    }

    override fun initialize() {
        super.initialize()
        playingItem.onEach {
            currentPlaySession.path = it?.path?: ""
            savePlaySession()
        }.launchIn(viewModelScope)
        playStatus.onEach {
            currentPlaySession.isPlaying = true
            savePlaySession()
        }.launchIn(viewModelScope)
        progress.onEach {
            currentPlaySession.position = it.position
            currentPlaySession.duration = it.duration
            savePlaySession(false)
        }.launchIn(viewModelScope)
        playError.onEach {
            if (hasNextItem()) {
                coroutineCompat.launchSingle("autoSkipToNext") {
                    delay(500)
                    skipToNext()
                }
            }
        }.launchIn(viewModelScope)
    }

    /**
     * 数据恢复
     */
    fun restore() {
        coroutineCompat.launchSingle("restore") {
            runCatchSilent({
                LogUtil.logD(TAG, "restore")
                // 播放会话
                val playSession = playControlService.fetchPlaySession(PlaySession.MEDIA_TYPE_VIDEO)
                LogUtil.logD(TAG, "restore playSession: $playSession")
                if (playSession == null) return@runCatchSilent
                setup(playSession.bucketPath, playType = PlayType.NOT_PLAY)
            }, {
                LogUtil.logE(TAG, "restore error: ${Log.getStackTraceString(it)}")
            })
        }
    }

    /**
     * 自动播放
     */
    fun autoPlay() {
        coroutineCompat.launchSingle("autoPlay") {
            runCatchSilent({
                LogUtil.logD(TAG, "autoPlay")
                // 播放会话
                val playSession = playControlService.fetchPlaySession(PlaySession.MEDIA_TYPE_VIDEO)
                LogUtil.logD(TAG, "autoPlay playSession: $playSession")
                if (playSession == null) return@runCatchSilent
                setup(playSession.bucketPath, playType = PlayType.AUTO_PLAY)
            }, {
                LogUtil.logE(TAG, "autoPlay error: ${Log.getStackTraceString(it)}")
            })
        }
    }

    /**
     * 强制播放
     */
    fun forcePlay(bucketPath: String, localVideo: LocalVideo? = null) {
        LogUtil.logD(TAG, "forcePlay: $bucketPath, $localVideo")
        setup(bucketPath, localVideo, PlayType.FORCE_PLAY)
    }

    /**
     * 启动
     */
    private fun setup(bucketPath: String, localVideo: LocalVideo? = null, playType: PlayType = PlayType.AUTO_PLAY) {
        coroutineCompat.launchSingle("setup") {
            runCatchSilent({
                LogUtil.logD(TAG, "setup: $bucketPath, $localVideo, $playType")
                if (bucketPath.isBlank()) return@runCatchSilent
                // 拉取数据
                val videos = mediaService.fetchLocalVideos(bucketPath, true)
                _localVideos.emit(UIState.Content(videos))
                LogUtil.logD(TAG, "setup videos: ${videos.size}")
                // 若无数据，返回
                if (videos.isEmpty()) return@runCatchSilent
                // 播放会话
                val playSession = playControlService.fetchPlaySession(PlaySession.MEDIA_TYPE_VIDEO)
                LogUtil.logD(TAG, "setup playSession: $playSession")
                currentPlaySession = if (localVideo != null) {
                    if (localVideo.path == playSession?.path) {
                        // 有对应播放会话记录，使用历史会话
                        playSession
                    } else {
                        // 没有对应播放会话记录，创建新会话
                        PlaySession().apply {
                            this.path = localVideo.path
                            this.bucketPath = bucketPath
                        }
                    }
                } else {
                    // 有会话记录使用会话记录，没有则创建第一首会话记录
                    playSession?: PlaySession().apply {
                        this.path = videos.first().path
                        this.bucketPath = bucketPath
                    }
                }
                savePlaySession()
                // 设置播放列表
                val startIndex = videos.indexOfFirst { it.path == currentPlaySession.path }
                setMediaItems(videos, startIndex, currentPlaySession.position)
                // 自动播放策略
                when (playType) {
                    PlayType.NOT_PLAY -> {
                        // ignore
                    }
                    PlayType.AUTO_PLAY -> {
                        if (currentPlaySession.isPlaying) {
                            play(startIndex)
                        }
                    }
                    PlayType.FORCE_PLAY -> {
                        play(startIndex)
                    }
                }
                LogUtil.logD(TAG, "setup success: $currentPlaySession")
            }, {
                LogUtil.logE(TAG, "setup error: ${Log.getStackTraceString(it)}")
                _localVideos.emit(UIState.Error(it))
            })
        }
    }

    override fun pause(fromUser: Boolean) {
        super.pause(fromUser)
        if (fromUser) {
            currentPlaySession.isPlaying = false
            savePlaySession()
        }
    }

    /**
     * 保存播放会话
     */
    private fun savePlaySession(enableLog: Boolean = true) {
        coroutineCompat.launchSingle("savePlaySession") {
            runCatchSilent({
                if (currentPlaySession.bucketPath.isBlank() || currentPlaySession.path.isBlank()) {
                    return@runCatchSilent
                }
                playControlService.savePlaySession(currentPlaySession, PlaySession.MEDIA_TYPE_VIDEO)
                if (enableLog) {
                    LogUtil.logD(TAG, "savePlaySession success: $currentPlaySession")
                }
            }, {
                LogUtil.logE(TAG, "savePlaySession error: $it")
            })
        }
    }

    fun onResume() {
        LogUtil.logD(TAG, "onResume: $isOnPauseStatus")
        if (!isOnPauseStatus) return
        coroutineCompat.launchSingle("autoPlay") {
            runCatchSilent({
                val playSession = playControlService.fetchPlaySession(PlaySession.MEDIA_TYPE_VIDEO)
                LogUtil.logD(TAG, "onResume playSession: $playSession")
                if (playSession?.isPlaying == true) {
                    play()
                }
            }, {
                LogUtil.logE(TAG, "onResume error: ${Log.getStackTraceString(it)}")
            })
        }
    }

    fun onPause() {
        LogUtil.logD(TAG, "onPause")
        pause(false)
        isOnPauseStatus = true
    }
}