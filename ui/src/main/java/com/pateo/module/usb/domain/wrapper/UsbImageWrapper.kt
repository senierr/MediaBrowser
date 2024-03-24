package com.pateo.module.usb.domain.wrapper

import coil.load
import com.pateo.module.usb.R
import com.pateo.module.usb.databinding.ItemUsbImageBinding
import com.senierr.media.repository.entity.UsbImage
import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper

/**
 *
 * @author senierr_zhou
 * @date 2023/07/29
 */
class UsbImageWrapper : ViewHolderWrapper<UsbImage>(R.layout.item_usb_image) {

    override fun onBindViewHolder(holder: ViewHolder, item: UsbImage) {
        val binding = ItemUsbImageBinding.bind(holder.itemView)

        binding.ivAlbum.load(item.getUri())
        binding.tvDisplayName.text = item.displayName
    }
}