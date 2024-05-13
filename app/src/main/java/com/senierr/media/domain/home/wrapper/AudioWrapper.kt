package com.senierr.media.domain.home.wrapper

import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper
import com.senierr.base.support.coroutine.ktx.setGone
import com.senierr.base.util.LogUtil
import com.senierr.media.R
import com.senierr.media.databinding.ItemAudioBinding
import com.senierr.media.domain.audio.viewmodel.AudioControlViewModel
import com.senierr.media.ktx.applicationViewModel
import com.senierr.media.repository.entity.LocalAudio
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 *
 * @author senierr_zhou
 * @date 2023/07/29
 */
class AudioWrapper : ViewHolderWrapper<LocalAudio>(R.layout.item_audio) {

    companion object {
        private const val TAG = "AudioWrapper"

        private const val PAYLOAD_REFRESH_PLAY_STATUS = "refreshPlayStatus"
    }

    private val controlViewModel: AudioControlViewModel by applicationViewModel()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val lifecycleOwner = recyclerView.findViewTreeLifecycleOwner()
        LogUtil.logD(TAG, "onAttachedToRecyclerView: $lifecycleOwner")
        if (lifecycleOwner == null) return
        // 播放状态
        controlViewModel.playStatus
            .onEach {
                LogUtil.logD(TAG, "playStatus: $it")
                notifyPlayStatus()
            }
            .launchIn(lifecycleOwner.lifecycleScope)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: LocalAudio) {}

    override fun onBindViewHolder(holder: ViewHolder, item: LocalAudio, payloads: List<Any>) {
        val binding = ItemAudioBinding.bind(holder.itemView)
        if (item.id == controlViewModel.playingItem.value?.id) {
            binding.ivPlayingIcon.setGone(false)
            binding.ivPlayingIcon.setMusicPlaying(controlViewModel.isPlaying())
        } else {
            binding.ivPlayingIcon.setMusicPlaying(false)
            binding.ivPlayingIcon.setGone(true)
        }
        if (!payloads.contains(PAYLOAD_REFRESH_PLAY_STATUS)) {
            binding.ivAlbum.load(item) {
                placeholder(R.drawable.ic_audio_file)
                error(R.drawable.ic_audio_file)
            }
            binding.tvDisplayName.text = item.displayName
        }
    }

    /**
     * 刷新播放状态
     */
    private fun notifyPlayStatus() {
        multiTypeAdapter.notifyItemRangeChanged(0, multiTypeAdapter.itemCount, PAYLOAD_REFRESH_PLAY_STATUS)
    }
}