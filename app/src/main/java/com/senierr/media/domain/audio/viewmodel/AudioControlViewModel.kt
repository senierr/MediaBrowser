package com.senierr.media.domain.audio.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.senierr.base.support.arch.viewmodel.state.UIState
import com.senierr.media.domain.home.viewmodel.AudioViewModel
import com.senierr.media.ktx.applicationViewModel
import com.senierr.media.repository.entity.LocalAudio
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 音乐控制
 *
 * @author senierr
 * @date 2024/3/31
 */
class AudioControlViewModel : BaseControlViewModel<LocalAudio>() {

    private val audioViewModel: AudioViewModel by applicationViewModel()

    init {
        audioViewModel.localFiles.onEach {
            if (it is UIState.Content) {
                val newList = it.value.filterIsInstance<LocalAudio>()
                setMediaItems(newList)
            }
        }.launchIn(viewModelScope)
    }

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
}