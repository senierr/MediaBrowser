package com.senierr.media.domain.video

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.senierr.adapter.internal.MultiTypeAdapter
import com.senierr.base.support.arch.UIState
import com.senierr.base.support.coroutine.CoroutineCompat
import com.senierr.base.support.coroutine.ktx.onThrottleClick
import com.senierr.base.support.coroutine.ktx.setGone
import com.senierr.base.support.coroutine.ktx.showToast
import com.senierr.base.support.coroutine.ktx.viewModel
import com.senierr.base.support.ui.BaseActivity
import com.senierr.base.util.LogUtil
import com.senierr.media.R
import com.senierr.media.databinding.ActivityVideoPlayerBinding
import com.senierr.media.domain.common.BaseControlViewModel
import com.senierr.media.domain.video.viewmodel.VideoControlViewModel
import com.senierr.media.domain.video.wrapper.PlayingListWrapper
import com.senierr.media.repository.entity.LocalAudio
import com.senierr.media.repository.entity.LocalVideo
import com.senierr.media.utils.DiffUtils
import com.senierr.media.utils.Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive

/**
 * 视频播放页面
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class VideoPlayerActivity : BaseActivity<ActivityVideoPlayerBinding>() {

    companion object {
        private const val INTERVAL_CONTROL_BAR_HIDE = 5 * 1000L

        fun start(context: Context) {
            context.startActivity(Intent(context, VideoPlayerActivity::class.java))
        }

        fun start(context: Context, bucketPath: String, localVideo: LocalVideo? = null) {
            context.startActivity(Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra("bucketPath", bucketPath)
                putExtra("localVideo", localVideo)
            })
        }
    }

    private val multiTypeAdapter = MultiTypeAdapter()
    private val playingListWrapper = PlayingListWrapper(this)

    private val coroutineCompat = CoroutineCompat(lifecycleScope)
    private val controlViewModel: VideoControlViewModel by viewModel()

    // 进度条是否处于拖动状态
    private var isSeekBarDragging = false

    override fun createViewBinding(layoutInflater: LayoutInflater): ActivityVideoPlayerBinding {
        return ActivityVideoPlayerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initViewModel()
        hideControlBar()
        // 主动播放
        val bucketPath: String? = intent.getStringExtra("bucketPath")
        val localVideo = intent.getParcelableExtra<LocalVideo>("localVideo")
        if (bucketPath.isNullOrBlank()) {
            controlViewModel.autoPlay()
        } else {
            controlViewModel.forcePlay(bucketPath, localVideo)
        }
    }

    override fun onStart() {
        super.onStart()
        controlViewModel.onResume()
    }

    override fun onStop() {
        super.onStop()
        controlViewModel.onPause()
    }

    private fun initView() {
        // 根布局
        binding.root.onThrottleClick {
            LogUtil.logD(TAG, "root onClick")
            if (isControlBarShowed()) {
                hideControlBar()
            } else {
                showControlBar()
            }
        }
        // 播放器
        binding.pvPlayer.useController = false

        // 返回按钮
        binding.btnBack.onThrottleClick {
            LogUtil.logD(TAG, "btnClose onClick")
            finish()
        }
        // 上一首
        binding.btnPlayPrevious.onThrottleClick {
            LogUtil.logD(TAG, "btnPlayPrevious onClick")
            if (controlViewModel.hasPreviousItem()) {
                controlViewModel.skipToPrevious()
            } else {
                showToast(R.string.has_no_previous)
            }
            showControlBar()
        }
        // 播放/暂停
        binding.btnPlayOrPause.onThrottleClick {
            LogUtil.logD(TAG, "btnPlayOrPause onClick")
            if (controlViewModel.isPlaying()) {
                controlViewModel.pause(true)
            } else {
                controlViewModel.play()
            }
            showControlBar()
        }
        // 下一首
        binding.btnPlayNext.onThrottleClick {
            LogUtil.logD(TAG, "btnPlayNext onClick")
            if (controlViewModel.hasNextItem()) {
                controlViewModel.skipToNext()
            } else {
                showToast(R.string.has_no_next)
            }
            showControlBar()
        }
        // 进度条
        binding.sbSeek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (p2 && isSeekBarDragging) {
                    showControlBar()
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                isSeekBarDragging = true
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                isSeekBarDragging = false
                controlViewModel.seekTo(binding.sbSeek.progress.toLong())
            }
        })
        // 播放列表
        binding.btnPlayingList.onThrottleClick {
            LogUtil.logD(TAG, "showPlayingList")
            binding.flPlayingList.setGone(false)
            hideControlBar()
        }

        binding.flPlayingList.onThrottleClick {
            LogUtil.logD(TAG, "showPlayingList")
            binding.flPlayingList.setGone(true)
        }
        binding.rvPlayingList.layoutManager = LinearLayoutManager(this)
        binding.rvPlayingList.setHasFixedSize(true)
        playingListWrapper.setOnItemClickListener { _, position, item ->
            LogUtil.logD(TAG, "playingListWrapper onClick: $item")
            controlViewModel.play(position)
        }
        multiTypeAdapter.register(playingListWrapper)
        binding.rvPlayingList.adapter = multiTypeAdapter
    }

    private fun initViewModel() {
        controlViewModel.initialize()
        binding.pvPlayer.player = controlViewModel.getPlayer()

        controlViewModel.localVideos
            .onEach { notifyLocalFilesChanged(it) }
            .launchIn(lifecycleScope)
        controlViewModel.playingItem
            .onEach { notifyPlayingItemChanged(it) }
            .launchIn(lifecycleScope)
        controlViewModel.playStatus
            .onEach { notifyPlayStatusChanged(it) }
            .launchIn(lifecycleScope)
        controlViewModel.progress
            .onEach { notifyProgressChanged(it) }
            .launchIn(lifecycleScope)
        controlViewModel.playError
            .onEach { notifyPlayErrorChanged(it) }
            .launchIn(lifecycleScope)
    }

    /**
     * 更新页面数据
     */
    private fun notifyLocalFilesChanged(state: UIState<List<LocalVideo>>) {
        LogUtil.logD(TAG, "notifyLocalFilesChanged: $state")
        when (state) {
            is UIState.Content -> {
                coroutineCompat.launchSingle("notifyLocalFilesChanged") {
                    val oldList = multiTypeAdapter.data.filterIsInstance<LocalAudio>()
                    val newList = state.value
                    val diffResult = DiffUtils.diffLocalFile(oldList, newList)
                    if (isActive) {
                        // 更新播放列表
                        multiTypeAdapter.data.clear()
                        multiTypeAdapter.data.addAll(newList)
                        diffResult.dispatchUpdatesTo(multiTypeAdapter)
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
    private fun notifyPlayingItemChanged(playingItem: LocalVideo?) {
        LogUtil.logD(TAG, "notifyPlayingItemChanged: $playingItem")
        binding.tvTitle.text = playingItem?.displayName
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
        LogUtil.logD(TAG, "notifyProgressChanged: ${progress.position} / ${progress.duration}, $isSeekBarDragging")
        if (progress.position < 0 || progress.duration < 0) return
        if (isSeekBarDragging) return
        binding.sbSeek.max = progress.duration.toInt()
        binding.sbSeek.progress = progress.position.toInt()
        binding.tvPosition.text = Utils.formatDuration(progress.position)
        binding.tvDuration.text = Utils.formatDuration(progress.duration)
    }

    /**
     * 更新播放异常
     */
    private fun notifyPlayErrorChanged(playError: Throwable) {
        LogUtil.logW(TAG, "notifyPlayErrorChanged: $playError")
        showToast(R.string.play_error)
    }

    /**
     * 控制栏是否显示
     */
    private fun isControlBarShowed() = binding.llTopBar.visibility == View.VISIBLE

    /**
     * 显示控制栏
     */
    private fun showControlBar() {
        LogUtil.logD(TAG, "showControlBar")
        binding.llTopBar.visibility = View.VISIBLE
        binding.llContent.visibility = View.VISIBLE
        binding.rlControlBottom.visibility = View.VISIBLE
        hideControlBar(INTERVAL_CONTROL_BAR_HIDE)
    }

    /**
     * 隐藏控制栏
     */
    private fun hideControlBar(delayTimes: Long = 0) {
        LogUtil.logD(TAG, "hideControlBar: $delayTimes")
        coroutineCompat.launchSingle("hideControlBar") {
            if (delayTimes > 0L) {
                delay(delayTimes)
            }
            binding.llTopBar.visibility = View.GONE
            binding.llContent.visibility = View.GONE
            binding.rlControlBottom.visibility = View.GONE
        }
    }

    /**
     * 显示播放列表
     */
    private fun showPlayingList() {
        LogUtil.logD(TAG, "showPlayingList")
        binding.flPlayingList.visibility = View.VISIBLE
    }

    /**
     * 隐藏播放列表
     */
    private fun hidePlayingList() {
        LogUtil.logD(TAG, "hidePlayingList")
        binding.flPlayingList.visibility = View.GONE
    }
}