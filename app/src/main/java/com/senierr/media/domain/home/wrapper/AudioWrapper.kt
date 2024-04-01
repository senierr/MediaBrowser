package com.senierr.media.domain.home.wrapper

import coil.load
import coil.transform.RoundedCornersTransformation
import com.senierr.adapter.internal.ViewHolder
import com.senierr.adapter.internal.ViewHolderWrapper
import com.senierr.base.util.ScreenUtil
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
        binding.ivAlbum.load(item) {
            placeholder(R.drawable.ic_audio_file)
            error(R.drawable.ic_audio_file)
        }
        binding.tvDisplayName.text = item.displayName
    }
}