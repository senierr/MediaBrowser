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
import com.senierr.media.repository.service.api.IMediaService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    override fun onItemCovertToMediaItem(item: LocalAudio): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(item.displayName)
//            .setArtist(title)
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
     * 拉取媒体数据
     */
    fun fetchLocalAudios(bucketPath: String) {
        viewModelScope.launchSingle("fetchLocalAudios") {
            LogUtil.logD(TAG, "fetchLocalAudios: $bucketPath")
            if (bucketPath.isBlank()) return@launchSingle
            runCatchSilent({
                val audios = mediaService.fetchLocalAudios(bucketPath, false)
                LogUtil.logD(TAG, "fetchLocalAudios success: ${audios.size}")
                setMediaItems(audios)
                _localAudios.emit(UIState.Content(audios))
            }, {
                LogUtil.logE(TAG, "fetchLocalAudios error: ${Log.getStackTraceString(it)}")
                _localAudios.emit(UIState.Error(it))
            })
        }
    }
}