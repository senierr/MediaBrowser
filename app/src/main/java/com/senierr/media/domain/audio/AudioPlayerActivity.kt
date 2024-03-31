package com.senierr.media.domain.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.senierr.adapter.internal.MultiTypeAdapter
import com.senierr.base.support.arch.viewmodel.state.UIState
import com.senierr.base.support.ktx.onClick
import com.senierr.base.support.ui.BaseActivity
import com.senierr.base.util.LogUtil
import com.senierr.media.databinding.ActivityAudioPlayerBinding
import com.senierr.media.domain.audio.viewmodel.AudioControlViewModel
import com.senierr.media.domain.audio.wrapper.PlayingListWrapper
import com.senierr.media.domain.home.viewmodel.AudioViewModel
import com.senierr.media.ktx.applicationViewModel
import com.senierr.media.repository.entity.LocalAudio
import com.senierr.media.repository.entity.LocalFile
import com.senierr.media.utils.DiffUtils
import com.senierr.media.utils.Utils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive

/**
 * 音乐播放页面
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class AudioPlayerActivity : BaseActivity<ActivityAudioPlayerBinding>() {

    companion object {
        fun start(context: Context, localAudio: LocalAudio) {
            context.startActivity(Intent(context, AudioPlayerActivity::class.java).apply {
                putExtra("localAudio", localAudio)
            })
        }
    }

    private val multiTypeAdapter = MultiTypeAdapter()
    private val playingListWrapper = PlayingListWrapper()

    // 进度条是否处于拖动状态
    private var isSeekBarDragging = false

    private val audioViewModel: AudioViewModel by applicationViewModel()
    private val controlViewModel: AudioControlViewModel by applicationViewModel()

    override fun createViewBinding(layoutInflater: LayoutInflater): ActivityAudioPlayerBinding {
        return ActivityAudioPlayerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initViewModel()
    }

    private fun initView() {
        // 返回按钮
        binding.layoutTopBar.btnBack.onClick {
            LogUtil.logD(TAG, "btnBack onClick")
            finish()
        }
        // 上一首
        binding.btnPlayPrevious.onClick {
            LogUtil.logD(TAG, "btnPlayPrevious onClick")
            controlViewModel.getMediaController()?.run {
                if (hasPreviousMediaItem()) {
                    seekToPreviousMediaItem()
                }
            }
        }
        // 播放/暂停
        binding.btnPlayOrPause.onClick {
            LogUtil.logD(TAG, "btnPlayOrPause onClick")
            controlViewModel.getMediaController()?.run {
                if (isPlaying) {
                    pause()
                } else {
                    play()
                }
            }
        }
        // 下一首
        binding.btnPlayNext.onClick {
            LogUtil.logD(TAG, "btnPlayNext onClick")
            controlViewModel.getMediaController()?.run {
                if (hasNextMediaItem()) {
                    seekToNextMediaItem()
                }
            }
        }
        // 进度条
        binding.sbSeek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}

            override fun onStartTrackingTouch(p0: SeekBar?) {
                isSeekBarDragging = true
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                isSeekBarDragging = false
                controlViewModel.getMediaController()?.run {
                    seekTo(binding.sbSeek.progress.toLong())
                }
            }
        })
        // 播放列表
        binding.btnPlayingList.onClick {
            showPlayingList()
        }
        binding.btnClose.onClick { hidePlayingList() }
        binding.llPlayingList.onClick { hidePlayingList() }

        binding.rvPlayingList.layoutManager = LinearLayoutManager(this)
        playingListWrapper.setOnItemClickListener { _, _, item ->
            LogUtil.logD(TAG, "playingListWrapper onClick: $item")

        }
        multiTypeAdapter.register(playingListWrapper)
        binding.rvPlayingList.adapter = multiTypeAdapter
    }

    private fun initViewModel() {
        audioViewModel.localFiles
            .onEach { notifyLocalFilesChanged(it) }
            .launchIn(lifecycleScope)
        controlViewModel.playingItem
            .onEach { notifyPlayingItem(it) }
            .launchIn(lifecycleScope)
        controlViewModel.playStatus
            .onEach { notifyPlayStatus(it) }
            .launchIn(lifecycleScope)
        controlViewModel.progress
            .onEach { notifyProgress(it) }
            .launchIn(lifecycleScope)
    }

    /**
     * 更新页面数据
     */
    private fun notifyLocalFilesChanged(state: UIState<List<LocalFile>>) {
        LogUtil.logD(TAG, "notifyLocalFilesChanged: $state")
        when (state) {
            is UIState.Content -> {
                lifecycleScope.launchSingle("notifyLocalFilesChanged") {
                    val oldList = multiTypeAdapter.data.filterIsInstance<LocalAudio>()
                    val newList = state.value.filterIsInstance<LocalAudio>()
                    val diffResult = DiffUtils.diffLocalFile(oldList, newList)
                    if (isActive) {
                        // 更新播放列表
                        diffResult.dispatchUpdatesTo(multiTypeAdapter)
                        multiTypeAdapter.data.clear()
                        multiTypeAdapter.data.addAll(newList)
                        // 更新队列
                        val localAudio: LocalAudio? = intent.getParcelableExtra("localAudio")
                        val startIndex = newList.indexOfFirst { it.id == localAudio?.id }
                        controlViewModel.getMediaController()?.run {
                            clearMediaItems()
                            val mediaItems = newList.map { audio ->
                                MediaItem.Builder()
                                    .setUri(audio.getUri())
                                    .setTag(audio)
                                    .build()
                            }
                            if (startIndex == -1) {
                                setMediaItems(mediaItems)
                            } else {
                                setMediaItems(mediaItems, startIndex, 0)
                            }
                            playWhenReady = true
                            prepare()
                        }
                        Log.d(TAG, "notifyLocalFilesChanged: ${oldList.size} -> ${newList.size}")
                    }
                }
            }
            is UIState.Error -> { finish() }
            else -> {}
        }
    }

    /**
     * 更新播放项
     */
    private fun notifyPlayingItem(playingItem: MediaItem?) {
        LogUtil.logD(TAG, "notifyPlayingItem: $playingItem")
        val localAudio = playingItem?.localConfiguration?.tag
        if (localAudio is LocalAudio) {
            binding.layoutTopBar.tvTitle.text = localAudio.displayName
            binding.ivCover.load(localAudio)
        }
    }

    /**
     * 更新播放状态
     */
    private fun notifyPlayStatus(isPlaying: Boolean) {
        LogUtil.logD(TAG, "notifyPlayStatus: $isPlaying")
        binding.btnPlayOrPause.isSelected = isPlaying
    }

    /**
     * 更新底部播放进度
     */
    private fun notifyProgress(progress: AudioControlViewModel.Progress) {
        LogUtil.logD(TAG, "notifyProgress: ${progress.position} / ${progress.duration}, $isSeekBarDragging")
        if (progress.position < 0 || progress.duration <= 0) return
        if (isSeekBarDragging) return
        binding.sbSeek.max = progress.duration.toInt()
        binding.sbSeek.progress = progress.position.toInt()
        binding.tvPosition.text = Utils.formatDuration(progress.position)
        binding.tvDuration.text = Utils.formatDuration(progress.duration)
    }

    /**
     * 显示播放列表
     */
    private fun showPlayingList() {
        LogUtil.logD(TAG, "showPlayingList")
        binding.llPlayingList.visibility = View.VISIBLE
    }

    /**
     * 隐藏播放列表
     */
    private fun hidePlayingList() {
        LogUtil.logD(TAG, "hidePlayingList")
        binding.llPlayingList.visibility = View.GONE
    }
}