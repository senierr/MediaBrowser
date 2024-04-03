package com.senierr.media.domain.audio.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.senierr.base.support.arch.viewmodel.state.UIState
import com.senierr.base.support.ktx.runCatchSilent
import com.senierr.base.util.LogUtil
import com.senierr.media.repository.MediaRepository
import com.senierr.media.repository.entity.LocalAudio
import com.senierr.media.repository.entity.PlaySession
import com.senierr.media.repository.service.api.IMediaService
import com.senierr.media.repository.service.api.IPlayControlService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 音乐控制
 *
 * @author senierr
 * @date 2024/3/31
 */
class AudioControlViewModel : BaseControlViewModel<LocalAudio>() {

    enum class PlayType {
        NOT_PLAY,   // 不播放
        AUTO_PLAY,  // 根据之前状态播放
        FORCE_PLAY  // 强制播放
    }

    // 当前目录下数据
    private val _localAudios = MutableStateFlow<UIState<List<LocalAudio>>>(UIState.Empty)
    val localAudios = _localAudios.asStateFlow()

    private val mediaService: IMediaService = MediaRepository.getService()
    private val playControlService: IPlayControlService = MediaRepository.getService()
    // 当前会话
    private var currentPlaySession: PlaySession = PlaySession()

    override fun onItemCovertToMediaItem(item: LocalAudio): MediaItem {
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
                viewModelScope.launchSingle("autoSkipToNext") {
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
        viewModelScope.launchSingle("restore") {
            runCatchSilent({
                LogUtil.logD(TAG, "restore")
                // 播放会话
                val playSession = playControlService.fetchPlaySession()
                LogUtil.logD(TAG, "restore playSession: $playSession")
                if (playSession == null) return@runCatchSilent
                play(playSession.bucketPath, playType = PlayType.NOT_PLAY)
            }, {
                LogUtil.logE(TAG, "restore error: ${Log.getStackTraceString(it)}")
            })
        }
    }

    /**
     * 自动播放
     */
    fun autoPlay() {
        viewModelScope.launchSingle("autoPlay") {
            runCatchSilent({
                LogUtil.logD(TAG, "autoPlay")
                // 播放会话
                val playSession = playControlService.fetchPlaySession()
                LogUtil.logD(TAG, "autoPlay playSession: $playSession")
                if (playSession == null) return@runCatchSilent
                play(playSession.bucketPath, playType = PlayType.AUTO_PLAY)
            }, {
                LogUtil.logE(TAG, "autoPlay error: ${Log.getStackTraceString(it)}")
            })
        }
    }

    /**
     * 播放
     */
    fun play(bucketPath: String, localAudio: LocalAudio? = null, playType: PlayType = PlayType.FORCE_PLAY) {
        viewModelScope.launchSingle("play") {
            runCatchSilent({
                LogUtil.logD(TAG, "play: $bucketPath, $localAudio, $playType")
                if (bucketPath.isBlank()) return@runCatchSilent
                // 拉取数据
                val audios = mediaService.fetchLocalAudios(bucketPath, true)
                _localAudios.emit(UIState.Content(audios))
                LogUtil.logD(TAG, "play audios: ${audios.size}")
                // 若无数据，返回
                if (audios.isEmpty()) return@runCatchSilent
                // 播放会话
                val playSession = playControlService.fetchPlaySession()
                LogUtil.logD(TAG, "play playSession: $playSession")
                currentPlaySession = if (localAudio != null) {
                    if (localAudio.path == playSession?.path) {
                        // 有对应播放会话记录，使用历史会话
                        playSession
                    } else {
                        // 没有对应播放会话记录，创建新会话
                        PlaySession().apply {
                            this.path = localAudio.path
                            this.bucketPath = bucketPath
                            this.mediaType = PlaySession.MEDIA_TYPE_AUDIO
                        }
                    }
                } else {
                    // 有会话记录使用会话记录，没有则创建第一首会话记录
                    playSession?: PlaySession().apply {
                        this.path = audios.first().path
                        this.bucketPath = bucketPath
                        this.mediaType = PlaySession.MEDIA_TYPE_AUDIO
                    }
                }
                savePlaySession()
                // 设置播放列表
                val startIndex = audios.indexOfFirst { it.path == currentPlaySession.path }
                setMediaItems(audios, startIndex, currentPlaySession.position)
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
                LogUtil.logD(TAG, "play success: $currentPlaySession")
            }, {
                LogUtil.logE(TAG, "play error: ${Log.getStackTraceString(it)}")
                _localAudios.emit(UIState.Error(it))
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
        viewModelScope.launchSingle("savePlaySession") {
            runCatchSilent({
                if (currentPlaySession.bucketPath.isBlank() || currentPlaySession.path.isBlank()) {
                    return@runCatchSilent
                }
                playControlService.savePlaySession(currentPlaySession)
                if (enableLog) {
                    LogUtil.logD(TAG, "savePlaySession success: $currentPlaySession")
                }
            }, {
                LogUtil.logE(TAG, "savePlaySession error: $it")
            })
        }
    }
}