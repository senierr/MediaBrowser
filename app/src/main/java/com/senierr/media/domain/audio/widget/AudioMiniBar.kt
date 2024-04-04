package com.senierr.media.domain.audio.widget

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.senierr.base.support.ktx.onThrottleClick
import com.senierr.base.support.ktx.showToast
import com.senierr.base.util.LogUtil
import com.senierr.media.R
import com.senierr.media.databinding.LayoutAudioPlayerBinding
import com.senierr.media.domain.audio.AudioPlayerActivity
import com.senierr.media.domain.audio.widget.viewmodel.BaseControlViewModel
import com.senierr.media.domain.audio.widget.viewmodel.MiniBarControlViewModel
import com.senierr.media.repository.entity.LocalAudio
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 *
 * @author senierr_zhou
 * @date 2024/04/03
 */
class AudioMiniBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RelativeLayout(context, attrs, defStyle) {

    companion object {
        private const val TAG = "AudioMiniBar"
    }

    private val binding: LayoutAudioPlayerBinding

    private var controlViewModel: MiniBarControlViewModel? = null

    init {
        binding = LayoutAudioPlayerBinding.inflate(LayoutInflater.from(context), this, true)

        binding.root.onThrottleClick {
            LogUtil.logD(TAG, "root onClick")
            controlViewModel?.getSessionActivityIntent()?.send()
        }
        // 上一首
        binding.btnPlayPrevious.onThrottleClick {
            LogUtil.logD(TAG, "btnPlayPrevious onClick")
            controlViewModel?.skipToPrevious()
        }
        // 播放/暂停
        binding.btnPlayOrPause.onThrottleClick {
            LogUtil.logD(TAG, "btnPlayOrPause onClick")
            if (controlViewModel?.isPlaying() == true) {
                controlViewModel?.pause()
            } else {
                controlViewModel?.play()
            }
        }
        // 下一首
        binding.btnPlayNext.onThrottleClick {
            LogUtil.logD(TAG, "btnPlayNext onClick")
            controlViewModel?.skipToNext()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val viewModelStore = findViewTreeViewModelStoreOwner()
        LogUtil.logD(TAG, "viewModelStore: $viewModelStore")
        viewModelStore?.let {
            controlViewModel = ViewModelProvider(it)[MiniBarControlViewModel::class.java]
            controlViewModel?.initialize(context)
        }
        val lifecycleOwner = findViewTreeLifecycleOwner()
        lifecycleOwner?.run {
            controlViewModel?.playingItem?.onEach {
                notifyPlayingItemChanged(it)
            }?.launchIn(lifecycleScope)
            controlViewModel?.playStatus?.onEach {
                notifyPlayStatusChanged(it)
            }?.launchIn(lifecycleScope)
            controlViewModel?.progress?.onEach {
                notifyProgressChanged(it)
            }?.launchIn(lifecycleScope)
        }
    }

    /**
     * 更新播放项
     */
    private fun notifyPlayingItemChanged(mediaMetadata: MediaMetadataCompat?) {
        LogUtil.logD(TAG, "notifyPlayingItemChanged: ${mediaMetadata?.description?.title}")
        val tempLocalAudio = LocalAudio(0, mediaMetadata?.getString(MediaMetadataCompat.METADATA_KEY_ART_URI)?: "", "", "", "", "")
        binding.ivCover.load(tempLocalAudio) {
            transformations(CircleCropTransformation())
            error(R.drawable.ic_album)
        }
        binding.tvTitle.text = mediaMetadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        binding.tvSubtitle.text = mediaMetadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
    }

    /**
     * 更新播放状态
     */
    private fun notifyPlayStatusChanged(isPlaying: Boolean) {
        LogUtil.logD(TAG, "notifyPlayStatusChanged: $isPlaying")
        binding.btnPlayOrPause.isSelected = isPlaying
    }

    /**
     * 更新底部播放进度
     */
    private fun notifyProgressChanged(progress: BaseControlViewModel.Progress) {
//        LogUtil.logD(TAG, "notifyProgressChanged: ${progress.position} / ${progress.duration}, $isSeekBarDragging")
        if (progress.position < 0 || progress.duration < 0) return
        binding.progressBar.max = progress.duration.toInt()
        binding.progressBar.progress = progress.position.toInt()
    }
}