package com.pateo.module.usb.domain.wrapper

import com.pateo.module.usb.R
import com.pateo.module.usb.databinding.ItemUsbFolderBinding
import com.senierr.media.repository.entity.UsbFolder
import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper

/**
 *
 * @author senierr_zhou
 * @date 2023/07/29
 */
class UsbFolderWrapper : ViewHolderWrapper<UsbFolder>(R.layout.item_usb_folder) {

    override fun onBindViewHolder(holder: ViewHolder, item: UsbFolder) {
        val binding = ItemUsbFolderBinding.bind(holder.itemView)

        binding.tvCount.text = (item.imageCount + item.audioCount + item.videoCount).toString()
        binding.tvDisplayName.text = item.displayName
    }
}