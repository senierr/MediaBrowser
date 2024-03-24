package com.pateo.module.usb.domain.wrapper

import coil.load
import com.pateo.module.usb.R
import com.pateo.module.usb.databinding.ItemUsbAudioBinding
import com.senierr.media.repository.entity.UsbAudio
import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper

/**
 *
 * @author senierr_zhou
 * @date 2023/07/29
 */
class UsbAudioWrapper : ViewHolderWrapper<UsbAudio>(R.layout.item_usb_audio) {

    override fun onBindViewHolder(holder: ViewHolder, item: UsbAudio) {
        val binding = ItemUsbAudioBinding.bind(holder.itemView)

        binding.ivAlbum.load(item.getUri())
        binding.tvDisplayName.text = item.displayName
    }
}