package com.senierr.media.domain.video.wrapper

import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper
import com.senierr.base.support.ktx.viewModel
import com.senierr.base.util.LogUtil
import com.senierr.media.R
import com.senierr.media.databinding.ItemVideoPlayingListBinding
import com.senierr.media.domain.video.viewmodel.VideoControlViewModel
import com.senierr.media.repository.entity.LocalVideo
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 播放列表
 *
 * @author chunjiezhou
 * @date 2021/08/05
 */
class PlayingListWrapper(viewModelStoreOwner: ViewModelStoreOwner) : ViewHolderWrapper<LocalVideo>(R.layout.item_video_playing_list) {

    companion object {
        private const val TAG = "PlayingListWrapper"

        private const val PAYLOAD_REFRESH_PLAYING_ITEM = "refreshPlayingItem"
    }

    private val controlViewModel: VideoControlViewModel by viewModelStoreOwner.viewModel()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val lifecycleOwner = recyclerView.findViewTreeLifecycleOwner()
        LogUtil.logD(TAG, "onAttachedToRecyclerView: $lifecycleOwner")
        if (lifecycleOwner == null) return
        controlViewModel.playingItem
            .onEach { notifyPlayingItemChanged() }
            .launchIn(lifecycleOwner.lifecycleScope)
        controlViewModel.playStatus
            .onEach { notifyPlayingItemChanged() }
            .launchIn(lifecycleOwner.lifecycleScope)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: LocalVideo) {}

    override fun onBindViewHolder(holder: ViewHolder, item: LocalVideo, payloads: List<Any>) {
        val binding = ItemVideoPlayingListBinding.bind(holder.itemView)
        if (item.id == controlViewModel.playingItem.value?.id) {
            binding.root.setBackgroundResource(R.color.app_theme_light)
            binding.tvDisplayName.setTextColor(binding.tvDisplayName.context.getColor(R.color.text_theme))
        } else {
            binding.root.setBackgroundResource(R.color.transparent)
            binding.tvDisplayName.setTextColor(binding.tvDisplayName.context.getColor(R.color.text_title))
        }
        if (!payloads.contains(PAYLOAD_REFRESH_PLAYING_ITEM)) {
            binding.ivCover.load(item.getUri())
            binding.tvDisplayName.text = item.displayName
        }
    }

    /**
     * 更新当前播放列表项
     */
    private fun notifyPlayingItemChanged() {
        LogUtil.logD(TAG, "notifyPlayingItemChanged")
        multiTypeAdapter.notifyItemRangeChanged(0, multiTypeAdapter.itemCount, PAYLOAD_REFRESH_PLAYING_ITEM)
    }
}