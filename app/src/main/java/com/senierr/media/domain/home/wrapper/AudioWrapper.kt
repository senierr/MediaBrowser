package com.senierr.media.domain.home.wrapper

import coil.load
import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper
import com.senierr.media.R
import com.senierr.media.databinding.ItemAudioBinding
import com.senierr.media.repository.entity.LocalAudio

/**
 *
 * @author senierr_zhou
 * @date 2023/07/29
 */
class AudioWrapper : ViewHolderWrapper<LocalAudio>(R.layout.item_audio) {

    override fun onBindViewHolder(holder: ViewHolder, item: LocalAudio) {
        val binding = ItemAudioBinding.bind(holder.itemView)
        binding.ivAlbum.load(item)
        binding.tvDisplayName.text = item.displayName
    }
}