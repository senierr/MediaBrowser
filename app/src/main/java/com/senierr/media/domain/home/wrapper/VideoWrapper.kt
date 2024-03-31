package com.senierr.media.domain.home.wrapper

import coil.load
import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper
import com.senierr.media.R
import com.senierr.media.databinding.ItemVideoBinding
import com.senierr.media.repository.entity.LocalVideo

/**
 *
 * @author senierr_zhou
 * @date 2023/07/29
 */
class VideoWrapper : ViewHolderWrapper<LocalVideo>(R.layout.item_video) {

    override fun onBindViewHolder(holder: ViewHolder, item: LocalVideo) {
        val binding = ItemVideoBinding.bind(holder.itemView)

        binding.ivAlbum.load(item.getUri())
        binding.tvDisplayName.text = item.displayName
    }
}