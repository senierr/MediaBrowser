package com.senierr.media.domain.video.wrapper

import coil.load
import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper
import com.senierr.base.util.LogUtil
import com.senierr.media.R
import com.senierr.media.databinding.ItemPlayingListBinding
import com.senierr.media.repository.entity.LocalVideo
import com.senierr.media.utils.Utils

/**
 * 播放列表
 *
 * @author chunjiezhou
 * @date 2021/08/05
 */
class PlayingListWrapper : ViewHolderWrapper<LocalVideo>(R.layout.item_playing_list) {

    companion object {
        private const val TAG = "PlayingListWrapper"

        private const val TAG_PAYLOAD_BG = "payload_bg"
    }

    // 上次播放的索引
    private var lastPlayingPosition: Int = -1
    // 当前播放索引
    private var nowPlayingPosition: Int = -1
        set(value) {
            lastPlayingPosition = field
            field = value
        }

    override fun onBindViewHolder(holder: ViewHolder, item: LocalVideo) {
        val binding = ItemPlayingListBinding.bind(holder.itemView)

        if (holder.layoutPosition == nowPlayingPosition) {
            binding.root.setBackgroundResource(R.color.app_theme)
        } else {
            binding.root.setBackgroundResource(R.color.transparent)
        }

        binding.tvDisplayName.text = item.displayName
        binding.tvDuration.text = Utils.formatDuration(item.duration)
        binding.ivCover.load(item.getUri())
    }

    override fun onBindViewHolder(holder: ViewHolder, item: LocalVideo, payloads: List<Any>) {
        if (payloads.contains(TAG_PAYLOAD_BG)) {
            val binding = ItemPlayingListBinding.bind(holder.itemView)
            if (holder.layoutPosition == nowPlayingPosition) {
                binding.root.setBackgroundResource(R.color.app_theme)
            } else {
                binding.root.setBackgroundResource(R.color.transparent)
            }
        } else {
            super.onBindViewHolder(holder, item, payloads)
        }
    }

    /**
     * 更新当前播放列表项
     */
    fun notifyPlayingItemChanged(position: Int) {
        LogUtil.logD(TAG, "notifyPlayingItemChanged: $position")
        if (position < 0) return
        nowPlayingPosition = position
        LogUtil.logD(TAG, "notifyPlayingItemChanged now: $nowPlayingPosition, last: $lastPlayingPosition")
        multiTypeAdapter.notifyItemChanged(nowPlayingPosition, TAG_PAYLOAD_BG)
        multiTypeAdapter.notifyItemChanged(lastPlayingPosition, TAG_PAYLOAD_BG)
    }
}