package com.senierr.media.domain.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.senierr.adapter.internal.MultiTypeAdapter
import com.senierr.base.support.ktx.onClick
import com.senierr.base.support.ui.BaseActivity
import com.senierr.base.util.LogUtil
import com.senierr.media.databinding.ActivityVideoPlayerBinding
import com.senierr.media.domain.audio.wrapper.PlayingListWrapper
import com.senierr.media.repository.entity.LocalVideo
import com.senierr.media.utils.Utils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 音乐播放页面
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class AudioPlayerActivity : BaseActivity<ActivityVideoPlayerBinding>() {

    companion object {
        private const val INTERVAL_HIDE_SHORT = 1 * 1000L
        private const val INTERVAL_HIDE_LONG = 5 * 1000L

        fun start(context: Context, usbVideo: LocalVideo) {
            context.startActivity(Intent(context, AudioPlayerActivity::class.java).apply {
                putExtra("usbVideo", usbVideo)
            })
        }
    }

    private val multiTypeAdapter = MultiTypeAdapter()
    private val playingListWrapper = PlayingListWrapper()

    // 进度条是否处于拖动状态
    private var isSeekBarDragging = false

    // 隐藏控制栏任务
    private var hideControlBarJob: Job? = null
    // 隐藏定位进度任务
    private var hideSeekProgressJob: Job? = null

//    // 播放状态变更回调
//    private val stateUiListener = GSYStateUiListener { state ->
//        LogUtil.logD(TAG, "state-changed: $state")
//        // 图标
//        if (state == StandardGSYVideoPlayer.CURRENT_STATE_PLAYING) {
//            binding.btnPlayOrPause.setImageResource(R.drawable.ic_pause_circle)
//        } else {
//            binding.btnPlayOrPause.setImageResource(R.drawable.ic_play_circle)
//        }
//    }
//    // 播放进度回调
//    private val videoProgressListener = GSYVideoProgressListener { _, _, currentPosition, duration ->
//        notifyBottomProgress(currentPosition, duration)
//    }
//    // 播放器回调
//    private val videoAllCallBack = object : GSYSampleCallBack() {
//        override fun onStartPrepared(url: String?, vararg objects: Any?) {
//            val title = objects.elementAt(0)
//            if (title is String) {
//                notifyTitle(title)
//            }
//        }
//
//        override fun onPrepared(url: String?, vararg objects: Any?) {
//            // 检查是否能播放 TODO
//
//            // 缓存播放记录 TODO
//        }
//
//        override fun onAutoComplete(url: String?, vararg objects: Any?) {
//            // 自动播放下一首
//            playNext(true)
//        }
//
//        override fun onPlayError(url: String?, vararg objects: Any?) {
//            showToast("播放错误")
//            finish()
//        }
//    }
//
//    // 触摸控制
//    private val onMediaControlListener = object : OnMediaControlListener() {
//        override fun onClick() {
//            // 单击切换控制栏显示
//            if (isControlBarShowed()) {
//                hideControlBar()
//            } else {
//                showControlBar()
//            }
//        }
//
//        override fun onVolumeChanged(distanceY: Float) {
//            // 音量调节 TODO
////            statusViewModel.adjustVolume(distanceY > 0)
//        }
//
//        override fun onBrightnessChanged(distanceY: Float) {
//            // 亮度调节 TODO
////            statusViewModel.adjustBrightness(distanceY > 0)
//        }
//    }



    // 当前播放的视频
    private var currentUsbVideo: LocalVideo? = null
    // 更新数据任务
    private var notifyDataChangedJob: Job? = null

    override fun createViewBinding(layoutInflater: LayoutInflater): ActivityVideoPlayerBinding {
        return ActivityVideoPlayerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val usbVideo: LocalVideo? = intent.getParcelableExtra("usbVideo")
        if (usbVideo == null) {
            finish()
            return
        }
        initView()
        initViewModel()
    }

    /********************************************* 初始化 *********************************************/

    /**
     * 初始化页面
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        // 返回按钮
        binding.btnBack.onClick { onBackPressed() }
        // 上一首
        binding.btnPlayPrevious.onClick {
            showControlBar()
        }
        // 播放/暂停
        binding.btnPlayOrPause.onClick {
            showControlBar()
        }
        // 下一首
        binding.btnPlayNext.onClick {
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
//                binding.mpPlayer.cancelFastPlay() TODO
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                isSeekBarDragging = false
//                binding.vpPlayer.seekTo(binding.sbSeek.progress.toLong())
            }
        })
        // 控制区域
        binding.llControl.onClick {
            if (isControlBarShowed()) {
                hideControlBar()
            } else {
                showControlBar()
            }
        }
        // 播放列表
        binding.btnPlayingList.onClick {
            showPlayingList()
            hideControlBar()
        }
        binding.btnClose.onClick { hidePlayingList() }
        binding.llPlayingList.onClick { hidePlayingList() }

        binding.rvPlayingList.layoutManager = LinearLayoutManager(this)
//        playingListWrapper.setOnItemClickListener { _, _, item -> startPlay(item) }
        multiTypeAdapter.register(playingListWrapper)
        binding.rvPlayingList.adapter = multiTypeAdapter
    }

    private fun initViewModel() {
//        mainViewModel.usbStatus
//            .onEach {
//                LogUtil.logD(TAG, "usbStatus-onChanged: $it")
//                if (it.action == UsbStatus.ACTION_EJECT) {
//                    finish()
//                }
//            }
//            .launchIn(lifecycleScope)
//        mainViewModel.usbFiles
//            .onEach {
//                val items = it.filterIsInstance<UsbVideo>()
//                LogUtil.logD(TAG, "usbFiles-onChanged: ${items.size}")
//                if (items.isNotEmpty()) {
//                    notifyPlayingListDataChanged(items)
//                }
//            }
//            .launchIn(lifecycleScope)
    }

    /********************************************* UI刷新 *********************************************/
    /**
     * 控制栏是否显示
     */
    private fun isControlBarShowed() = binding.llControl.visibility == View.VISIBLE

    /**
     * 显示控制栏
     */
    private fun showControlBar() {
        binding.llControl.visibility = View.VISIBLE
        hideControlBar(INTERVAL_HIDE_LONG)
    }

    /**
     * 隐藏控制栏
     */
    private fun hideControlBar(delayTimes: Long = 0) {
        hideControlBarJob?.cancel()
        hideControlBarJob = lifecycleScope.launch {
            if (delayTimes != 0L) {
                delay(delayTimes)
            }
            binding.llControl.visibility = View.GONE
        }
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

    /**
     * 更新页面数据
     */
//    private fun notifyPlayingListDataChanged(newList: List<UsbFile>) {
//        notifyDataChangedJob?.cancel()
//        notifyDataChangedJob = lifecycleScope.launch {
//            val oldList = multiTypeAdapter.data.filterIsInstance<UsbFile>()
//            val diffResult = DiffUtils.diffUsbFile(oldList, newList)
//            if (isActive) {
//                diffResult.dispatchUpdatesTo(multiTypeAdapter)
//                multiTypeAdapter.data.clear()
//                multiTypeAdapter.data.addAll(newList)
//                LogUtil.logD(TAG, "notifyPlayingListDataChanged: ${oldList.size} -> ${newList.size}")
//            }
//        }
//    }
    
    /**
     * 更新标题
     */
    private fun notifyTitle(title: String) {
        LogUtil.logD(TAG, "notifyTitle: $title")
        binding.tvTitle.text = title
    }

    /**
     * 更新底部播放进度
     */
    private fun notifyBottomProgress(position: Long, total: Long) {
        LogUtil.logD(TAG, "notifyBottomProgress: $position / $total, $isSeekBarDragging")
        if (position < 0 || total <= 0) return
        if (isSeekBarDragging) return
        binding.sbSeek.max = total.toInt()
        binding.sbSeek.progress = position.toInt()
        binding.tvCurrent.text = Utils.formatDuration(position)
        binding.tvTotal.text = Utils.formatDuration(total)
    }
}