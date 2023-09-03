package com.pateo.module.usb.domain

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.pateo.module.usb.R
import com.pateo.module.usb.databinding.ActivityVideoPlayerBinding
import com.pateo.module.usb.domain.viewmodel.UsbMainViewModel
import com.pateo.module.usb.domain.wrapper.PlayingListWrapper
import com.pateo.module.usb.ktx.applicationViewModel
import com.pateo.module.usb.utils.DiffUtils
import com.pateo.module.usb.utils.OnMediaControlListener
import com.pateo.module.usb.utils.Utils
import com.qinggan.usbvideo.repository.entity.UsbFile
import com.qinggan.usbvideo.repository.entity.UsbStatus
import com.qinggan.usbvideo.repository.entity.UsbVideo
import com.senierr.adapter.internal.MultiTypeAdapter
import com.senierr.base.support.ktx.onClick
import com.senierr.base.support.ktx.showToast
import com.senierr.base.support.ui.BaseActivity
import com.senierr.base.util.LogUtil
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import com.shuyu.gsyvideoplayer.listener.GSYStateUiListener
import com.shuyu.gsyvideoplayer.listener.GSYVideoProgressListener
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tv.danmaku.ijk.media.player.IjkMediaPlayer

/**
 * 视频播放页面
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class VideoPlayerActivity : BaseActivity<ActivityVideoPlayerBinding>() {

    companion object {
        private const val INTERVAL_HIDE_SHORT = 1 * 1000L
        private const val INTERVAL_HIDE_LONG = 5 * 1000L

        fun start(context: Context, usbVideo: UsbVideo) {
            context.startActivity(Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra("usbVideo", usbVideo)
            })
        }
    }

    private val multiTypeAdapter = MultiTypeAdapter()
    private val playingListWrapper = PlayingListWrapper()

    private val usbMainViewModel by applicationViewModel<UsbMainViewModel>()

    // 进度条是否处于拖动状态
    private var isSeekBarDragging = false

    // 隐藏控制栏任务
    private var hideControlBarJob: Job? = null
    // 隐藏定位进度任务
    private var hideSeekProgressJob: Job? = null

    // 播放状态变更回调
    private val stateUiListener = GSYStateUiListener { state ->
        LogUtil.logD(TAG, "state-changed: $state")
        // 图标
        if (state == StandardGSYVideoPlayer.CURRENT_STATE_PLAYING) {
            binding.btnPlayOrPause.setImageResource(R.drawable.ic_pause_circle)
        } else {
            binding.btnPlayOrPause.setImageResource(R.drawable.ic_play_circle)
        }
    }
    // 播放进度回调
    private val videoProgressListener = GSYVideoProgressListener { _, _, currentPosition, duration ->
        notifyBottomProgress(currentPosition, duration)
    }
    // 播放器回调
    private val videoAllCallBack = object : GSYSampleCallBack() {
        override fun onStartPrepared(url: String?, vararg objects: Any?) {
            val title = objects.elementAt(0)
            if (title is String) {
                notifyTitle(title)
            }
        }

        override fun onPrepared(url: String?, vararg objects: Any?) {
            // 检查是否能播放 TODO

            // 缓存播放记录 TODO
        }

        override fun onAutoComplete(url: String?, vararg objects: Any?) {
            // 自动播放下一首
            playNext(true)
        }

        override fun onPlayError(url: String?, vararg objects: Any?) {
            showToast("播放错误")
            finish()
        }
    }

    // 触摸控制
    private val onMediaControlListener = object : OnMediaControlListener() {
        override fun onClick() {
            // 单击切换控制栏显示
            if (isControlBarShowed()) {
                hideControlBar()
            } else {
                showControlBar()
            }
        }

        override fun onVolumeChanged(distanceY: Float) {
            // 音量调节 TODO
//            statusViewModel.adjustVolume(distanceY > 0)
        }

        override fun onBrightnessChanged(distanceY: Float) {
            // 亮度调节 TODO
//            statusViewModel.adjustBrightness(distanceY > 0)
        }
    }



    // 当前播放的视频
    private var currentUsbVideo: UsbVideo? = null
    // 更新数据任务
    private var notifyDataChangedJob: Job? = null

    override fun createViewBinding(layoutInflater: LayoutInflater): ActivityVideoPlayerBinding {
        return ActivityVideoPlayerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val usbVideo: UsbVideo? = intent.getParcelableExtra("usbVideo")
        if (usbVideo == null) {
            finish()
            return
        }
        initPlayer()
        initView()
        initViewModel()
        startPlay(usbVideo)
    }

    override fun onResume() {
        super.onResume()
        binding.vpPlayer.onVideoResume()
        // 刚进入显示控制栏
        showControlBar()
    }

    override fun onPause() {
        super.onPause()
        binding.vpPlayer.onVideoPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.vpPlayer.release()
    }

    /********************************************* 初始化 *********************************************/

    /**
     * 初始化播放器
     */
    private fun initPlayer() {
        IjkPlayerManager.setLogLevel(IjkMediaPlayer.IJK_LOG_SILENT)
        val imageView = ImageView(this)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        val gsyVideoOption = GSYVideoOptionBuilder()
        gsyVideoOption.setThumbImageView(imageView)
            .setStartAfterPrepared(true)
            .setNeedShowWifiTip(false)
            .setIsTouchWiget(false)
            .setIsTouchWigetFull(false)
            .setRotateViewAuto(false)
            .setLockLand(false)
            .setAutoFullWithSize(false)
            .setShowFullAnimation(false)
            .setNeedLockFull(false)
            .setCacheWithPlay(false)
            .setNeedOrientationUtils(false)
            .setVideoAllCallBack(videoAllCallBack)
            .setGSYStateUiListener(stateUiListener)
            .setGSYVideoProgressListener(videoProgressListener)
            .build(binding.vpPlayer)
    }

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
            playPrevious()
        }
        // 播放/暂停
        binding.btnPlayOrPause.onClick {
            showControlBar()
            playOrPause()
        }
        // 下一首
        binding.btnPlayNext.onClick {
            showControlBar()
            playNext()
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
                binding.vpPlayer.seekTo(binding.sbSeek.progress.toLong())
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
        playingListWrapper.setOnItemClickListener { _, _, item -> startPlay(item) }
        multiTypeAdapter.register(playingListWrapper)
        binding.rvPlayingList.adapter = multiTypeAdapter
        // 触摸控制
        onMediaControlListener.initialize(this)
        binding.flTouchControl.setOnTouchListener(onMediaControlListener)
    }

    private fun initViewModel() {
        usbMainViewModel.usbStatus
            .onEach {
                LogUtil.logD(TAG, "usbStatus-onChanged: $it")
                if (it.action == UsbStatus.ACTION_EJECT) {
                    finish()
                }
            }
            .launchIn(lifecycleScope)
        usbMainViewModel.usbFiles
            .onEach {
                val items = it.filterIsInstance<UsbVideo>()
                LogUtil.logD(TAG, "usbFiles-onChanged: ${items.size}")
                if (items.isNotEmpty()) {
                    notifyPlayingListDataChanged(items)
                }
            }
            .launchIn(lifecycleScope)
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
    private fun notifyPlayingListDataChanged(newList: List<UsbFile>) {
        notifyDataChangedJob?.cancel()
        notifyDataChangedJob = lifecycleScope.launch {
            val oldList = multiTypeAdapter.data.filterIsInstance<UsbFile>()
            val diffResult = DiffUtils.diffUsbFile(oldList, newList)
            if (isActive) {
                diffResult.dispatchUpdatesTo(multiTypeAdapter)
                multiTypeAdapter.data.clear()
                multiTypeAdapter.data.addAll(newList)
                LogUtil.logD(TAG, "notifyPlayingListDataChanged: ${oldList.size} -> ${newList.size}")
            }
        }
    }
    
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

    /********************************************* 内部方法 *********************************************/
    /**
     * 获取当前播放列表
     */
    private fun getPlayingList(): List<UsbVideo> {
        return usbMainViewModel.usbFiles.replayCache.firstOrNull()?.filterIsInstance<UsbVideo>()?: emptyList()
    }

    /********************************************* 播放控制 *********************************************/
    /**
     * 开始播放
     */
    private fun startPlay(usbVideo: UsbVideo) {
        LogUtil.logD(TAG, "startPlay: $usbVideo")
        currentUsbVideo = usbVideo
        // 更新播放列表索引
        val position = getPlayingList().indexOfFirst { it.id == usbVideo.id }
        playingListWrapper.notifyPlayingItemChanged(position)
        binding.rvPlayingList.scrollToPosition(position)
        // 设置封面
        val thumbImageView = binding.vpPlayer.thumbImageView as ImageView
        thumbImageView.load(usbVideo.getUri())
        // 启动
        binding.vpPlayer.setUp("file:///${usbVideo.path}", false, usbVideo.displayName)

        // 开始播放前seek，用于恢复播放
//        binding.vpPlayer.seekOnStart = 0
        // 播放
        binding.vpPlayer.startPlayLogic()
    }

    /**
     * 上一首
     *
     * @param isLoop 是否循环：即如果是第一首，是否播放上一首（列表最后一首）
     */
    private fun playPrevious(isLoop: Boolean = false) {
        val playingUsbVideos = getPlayingList()
        if (playingUsbVideos.isEmpty()) return
        val currentPosition = playingUsbVideos.indexOfFirst { it.id == currentUsbVideo?.id }
        var newPosition = currentPosition - 1
        if (newPosition < 0) {
            if (isLoop) {
                newPosition = playingUsbVideos.size - 1
            } else {
//                Utils.showToast(getString(R.string.usb_video_last_already))
                return
            }
        }
        Log.d(TAG, "playPrevious: ${newPosition + 1} / ${playingUsbVideos.size}, isLoop: $isLoop")
        playingUsbVideos.elementAtOrNull(newPosition)?.let { startPlay(it) }
    }

    /**
     * 播放/暂停
     */
    private fun playOrPause() {
        Log.d(TAG, "playOrPause")
        binding.vpPlayer.playOrPause()
    }

    /**
     * 下一首
     *
     * @param isLoop 是否循环：即如果是最后一首，是否播放下一首（列表第一首）
     */
    private fun playNext(isLoop: Boolean = false) {
        val playingUsbVideos = getPlayingList()
        if (playingUsbVideos.isEmpty()) return
        val currentPosition = playingUsbVideos.indexOfFirst { it.id == currentUsbVideo?.id }
        var newPosition = currentPosition + 1
        if (newPosition > (playingUsbVideos.size - 1)) {
            if (isLoop) {
                newPosition = 0
            } else {
//                Utils.showToast(getString(R.string.usb_video_last_already))
                return
            }
        }
        Log.d(TAG, "PlayNext: ${newPosition + 1} / ${playingUsbVideos.size}, isLoop: $isLoop")
        playingUsbVideos.elementAtOrNull(newPosition)?.let { startPlay(it) }
    }
}