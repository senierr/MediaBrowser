package com.pateo.module.usb.domain.wrapper

import coil.load
import com.pateo.module.usb.R
import com.pateo.module.usb.databinding.ItemUsbPlayingListBinding
import com.pateo.module.usb.utils.Utils
import com.qinggan.usbvideo.repository.entity.UsbVideo
import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper
import com.senierr.base.util.LogUtil

/**
 * 播放列表
 *
 * @author chunjiezhou
 * @date 2021/08/05
 */
class PlayingListWrapper : ViewHolderWrapper<UsbVideo>(R.layout.item_usb_playing_list) {

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

    override fun onBindViewHolder(holder: ViewHolder, item: UsbVideo) {
        val binding = ItemUsbPlayingListBinding.bind(holder.itemView)

        if (holder.layoutPosition == nowPlayingPosition) {
            binding.root.setBackgroundResource(R.color.app_theme)
        } else {
            binding.root.setBackgroundResource(R.color.transparent)
        }

        binding.tvDisplayName.text = item.displayName
        binding.tvDuration.text = Utils.formatDuration(item.duration)
        binding.ivCover.load(item.getUri())
    }

    override fun onBindViewHolder(holder: ViewHolder, item: UsbVideo, payloads: List<Any>) {
        if (payloads.contains(TAG_PAYLOAD_BG)) {
            val binding = ItemUsbPlayingListBinding.bind(holder.itemView)
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