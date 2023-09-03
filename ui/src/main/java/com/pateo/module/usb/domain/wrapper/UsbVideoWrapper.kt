package com.pateo.module.usb.domain.wrapper

import coil.load
import com.pateo.module.usb.R
import com.pateo.module.usb.databinding.ItemUsbVideoBinding
import com.qinggan.usbvideo.repository.entity.UsbVideo
import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper

/**
 *
 * @author senierr_zhou
 * @date 2023/07/29
 */
class UsbVideoWrapper : ViewHolderWrapper<UsbVideo>(R.layout.item_usb_video) {

    override fun onBindViewHolder(holder: ViewHolder, item: UsbVideo) {
        val binding = ItemUsbVideoBinding.bind(holder.itemView)

        binding.ivAlbum.load(item.getUri())
        binding.tvDisplayName.text = item.displayName
    }
}