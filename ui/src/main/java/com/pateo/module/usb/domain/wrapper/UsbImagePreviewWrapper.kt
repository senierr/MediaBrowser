package com.pateo.module.usb.domain.wrapper

import android.view.ViewGroup
import coil.load
import com.pateo.module.usb.R
import com.pateo.module.usb.databinding.ItemUsbImagePreviewBinding
import com.qinggan.usbvideo.repository.entity.UsbImage
import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper

/**
 *
 * @author senierr_zhou
 * @date 2023/07/29
 */
class UsbImagePreviewWrapper : ViewHolderWrapper<UsbImage>(R.layout.item_usb_image_preview) {

    var onPhotoTapListener: com.pateo.module.usb.widget.photoview.OnPhotoTapListener? = null
    
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val viewHolder = super.onCreateViewHolder(parent)
        val binding = ItemUsbImagePreviewBinding.bind(viewHolder.itemView)
        binding.ivAlbum.setOnPhotoTapListener { view, x, y ->
            onPhotoTapListener?.onPhotoTap(view, x, y)
        }
        return viewHolder
    }
    
    override fun onBindViewHolder(holder: ViewHolder, item: UsbImage) {
        val binding = ItemUsbImagePreviewBinding.bind(holder.itemView)

        binding.ivAlbum.load(item.getUri())
    }
}