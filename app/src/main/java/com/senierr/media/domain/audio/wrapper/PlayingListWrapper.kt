package com.senierr.media.domain.audio.wrapper

import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper
import com.senierr.base.support.coroutine.ktx.setGone
import com.senierr.base.util.LogUtil
import com.senierr.media.R
import com.senierr.media.databinding.ItemAudioPlayingListBinding
import com.senierr.media.domain.audio.viewmodel.AudioControlViewModel
import com.senierr.media.ktx.applicationViewModel
import com.senierr.media.repository.entity.LocalAudio
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 播放列表
 *
 * @author chunjiezhou
 * @date 2021/08/05
 */
class PlayingListWrapper : ViewHolderWrapper<LocalAudio>(R.layout.item_audio_playing_list) {

    companion object {
        private const val TAG = "PlayingListWrapper"

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
        val binding = ItemAudioPlayingListBinding.bind(holder.itemView)
        if (item.id == controlViewModel.playingItem.value?.id) {
            binding.root.setBackgroundResource(R.color.app_theme_light)
            binding.tvIndex.setGone(true)
            binding.ivPlayingIcon.setGone(false)
            binding.ivPlayingIcon.setMusicPlaying(controlViewModel.isPlaying())
            binding.tvDisplayName.setTextColor(binding.tvDisplayName.context.getColor(R.color.text_theme))
        } else {
            binding.root.setBackgroundResource(R.color.transparent)
            binding.tvIndex.setGone(false)
            binding.ivPlayingIcon.setMusicPlaying(false)
            binding.ivPlayingIcon.setGone(true)
            binding.tvDisplayName.setTextColor(binding.tvDisplayName.context.getColor(R.color.text_title))
        }
        if (!payloads.contains(PAYLOAD_REFRESH_PLAY_STATUS)) {
            binding.tvIndex.text = "${holder.layoutPosition}"
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