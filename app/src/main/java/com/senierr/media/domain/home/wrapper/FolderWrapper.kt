package com.senierr.media.domain.home.wrapper

import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper
import com.senierr.media.R
import com.senierr.media.databinding.ItemFolderBinding
import com.senierr.media.repository.entity.LocalFolder

/**
 *
 * @author senierr_zhou
 * @date 2023/07/29
 */
class FolderWrapper : ViewHolderWrapper<LocalFolder>(R.layout.item_folder) {

    override fun onBindViewHolder(holder: ViewHolder, item: LocalFolder) {
        val binding = ItemFolderBinding.bind(holder.itemView)

        binding.tvCount.text = (item.imageCount + item.audioCount + item.videoCount).toString()
        binding.tvDisplayName.text = item.displayName
    }
}