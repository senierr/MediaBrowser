package com.senierr.media.domain.home.wrapper

import coil.load
import coil.transform.RoundedCornersTransformation
import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper
import com.senierr.base.util.ScreenUtil
import com.senierr.media.R
import com.senierr.media.databinding.ItemImageBinding
import com.senierr.media.repository.entity.LocalImage

/**
 *
 * @author senierr_zhou
 * @date 2023/07/29
 */
class ImageWrapper : ViewHolderWrapper<LocalImage>(R.layout.item_image) {

    override fun onBindViewHolder(holder: ViewHolder, item: LocalImage) {
        val binding = ItemImageBinding.bind(holder.itemView)

        binding.ivAlbum.load(item.getUri())
    }
}