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

    // 当前目录下数据
    private val _localAudios = MutableStateFlow<UIState<List<LocalAudio>>>(UIState.Empty)
    val localAudios = _localAudios.asStateFlow()

    private val mediaService: IMediaService = MediaRepository.getService()
    private val playControlService: IPlayControlService = MediaRepository.getService()

    init {
        playError.onEach {
            if (hasNextItem()) {
                skipToNext()
            }
        }.launchIn(viewModelScope)
    }

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

    /**
     * 自动播放
     */
    fun autoPlay(bucketPath: String, localAudio: LocalAudio? = null, autoPlay: Boolean = true) {
        viewModelScope.launchSingle("autoPlay") {
            LogUtil.logD(TAG, "autoPlay: $bucketPath, $localAudio, $autoPlay")
            if (bucketPath.isBlank()) return@launchSingle
            runCatchSilent({
                // 拉取数据
                val audios = mediaService.fetchLocalAudios(bucketPath, true)
                _localAudios.emit(UIState.Content(audios))
                LogUtil.logD(TAG, "autoPlay audios: ${audios.size}")
                // 播放会话
                val playSession = playControlService.fetchPlaySession()
                LogUtil.logD(TAG, "autoPlay playSession: $playSession")
                val newPlaySession = if (localAudio != null) {
                    if (localAudio.path == playSession?.path) {
                        playSession
                    } else {
                        PlaySession.create(localAudio)
                    }
                } else {
                    playSession
                }
                // 设置播放列表
                val startIndex = audios.indexOfFirst { it.path == newPlaySession?.path }
                if (startIndex >= 0) {
                    setMediaItems(audios, startIndex, newPlaySession?.position?: 0)
                    if (autoPlay) {
                        play(startIndex)
                    }
                } else {
                    setMediaItems(audios)
                    if (autoPlay) {
                        play(0)
                    }
                }
                LogUtil.logD(TAG, "autoPlay success: $newPlaySession")
            }, {
                LogUtil.logE(TAG, "autoPlay error: ${Log.getStackTraceString(it)}")
                _localAudios.emit(UIState.Error(it))
            })
        }
    }
}