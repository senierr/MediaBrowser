package com.senierr.media.domain.image.wrapper

import android.view.ViewGroup
import coil.load
import com.github.chrisbanes.photoview.OnPhotoTapListener
import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper
import com.senierr.media.R
import com.senierr.media.databinding.ItemImagePreviewBinding
import com.senierr.media.repository.entity.LocalImage

/**
 *
 * @author senierr_zhou
 * @date 2023/07/29
 */
class ImagePreviewWrapper : ViewHolderWrapper<LocalImage>(R.layout.item_image_preview) {

    var onPhotoTapListener: OnPhotoTapListener? = null
    
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val viewHolder = super.onCreateViewHolder(parent)
        val binding = ItemImagePreviewBinding.bind(viewHolder.itemView)
        binding.ivAlbum.setOnPhotoTapListener { view, x, y ->
            onPhotoTapListener?.onPhotoTap(view, x, y)
        }
        return viewHolder
    }
    
    override fun onBindViewHolder(holder: ViewHolder, item: LocalImage) {
        val binding = ItemImagePreviewBinding.bind(holder.itemView)

        binding.ivAlbum.load(item.getUri())
    }
}