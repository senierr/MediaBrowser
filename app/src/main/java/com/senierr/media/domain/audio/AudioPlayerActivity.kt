package com.senierr.media.domain.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.dirror.lyricviewx.OnPlayClickListener
import com.dirror.lyricviewx.OnSingleClickListener
import com.senierr.base.support.arch.viewmodel.state.UIState
import com.senierr.base.support.ktx.onThrottleClick
import com.senierr.base.support.ktx.setGone
import com.senierr.base.support.ktx.showToast
import com.senierr.base.support.ui.BaseActivity
import com.senierr.base.util.LogUtil
import com.senierr.base.util.ScreenUtil
import com.senierr.media.R
import com.senierr.media.databinding.ActivityAudioPlayerBinding
import com.senierr.media.domain.audio.dialog.PlayingListDialog
import com.senierr.media.domain.audio.viewmodel.AudioControlViewModel
import com.senierr.media.domain.audio.viewmodel.BaseControlViewModel
import com.senierr.media.ktx.applicationViewModel
import com.senierr.media.repository.entity.LocalAudio
import com.senierr.media.utils.Utils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File

/**
 * 音乐播放页面
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class AudioPlayerActivity : BaseActivity<ActivityAudioPlayerBinding>() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AudioPlayerActivity::class.java))
        }

        fun start(context: Context, bucketPath: String, localAudio: LocalAudio? = null) {
            context.startActivity(Intent(context, AudioPlayerActivity::class.java).apply {
                putExtra("bucketPath", bucketPath)
                putExtra("localAudio", localAudio)
            })
        }
    }

    private val controlViewModel: AudioControlViewModel by applicationViewModel()

    // 进度条是否处于拖动状态
    private var isSeekBarDragging = false

    override fun createViewBinding(layoutInflater: LayoutInflater): ActivityAudioPlayerBinding {
        return ActivityAudioPlayerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initViewModel()
        // 主动播放
        val bucketPath: String? = intent.getStringExtra("bucketPath")
        val localAudio = intent.getParcelableExtra<LocalAudio>("localAudio")
        if (bucketPath.isNullOrBlank()) {
            controlViewModel.autoPlay()
        } else {
            controlViewModel.forcePlay(bucketPath, localAudio)
        }
    }

    private fun initView() {
        // 返回按钮
        binding.btnClose.onThrottleClick {
            LogUtil.logD(TAG, "btnClose onClick")
            finish()
        }

        binding.llContent.onThrottleClick {
            LogUtil.logD(TAG, "llContent onClick")
            binding.llContent.setGone(true)
            binding.lvLyric.setGone(false)
        }
        // 上一首
        binding.btnPlayPrevious.onThrottleClick {
            LogUtil.logD(TAG, "btnPlayPrevious onClick")
            if (controlViewModel.hasPreviousItem()) {
                controlViewModel.skipToPrevious()
            } else {
                showToast(R.string.has_no_previous)
            }
        }
        // 播放/暂停
        binding.btnPlayOrPause.onThrottleClick {
            LogUtil.logD(TAG, "btnPlayOrPause onClick")
            if (controlViewModel.isPlaying()) {
                controlViewModel.pause(true)
            } else {
                controlViewModel.play()
            }
        }
        // 下一首
        binding.btnPlayNext.onThrottleClick {
            LogUtil.logD(TAG, "btnPlayNext onClick")
            if (controlViewModel.hasNextItem()) {
                controlViewModel.skipToNext()
            } else {
                showToast(R.string.has_no_next)
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
                controlViewModel.seekTo(binding.sbSeek.progress.toLong())
            }
        })
        // 播放模式
        binding.btnPlayMode.onThrottleClick {
            LogUtil.logD(TAG, "btnPlayMode onClick")
            val newPlayMode = when (controlViewModel.playMode.value) {
                BaseControlViewModel.PlayMode.ONE -> {
                    showToast(R.string.play_mode_list)
                    BaseControlViewModel.PlayMode.LIST
                }
                BaseControlViewModel.PlayMode.LIST -> {
                    showToast(R.string.play_mode_all)
                    BaseControlViewModel.PlayMode.ALL
                }
                BaseControlViewModel.PlayMode.ALL -> {
                    showToast(R.string.play_mode_shuffle)
                    BaseControlViewModel.PlayMode.SHUFFLE
                }
                BaseControlViewModel.PlayMode.SHUFFLE -> {
                    showToast(R.string.play_mode_one)
                    BaseControlViewModel.PlayMode.ONE
                }
            }
            controlViewModel.setPlayMode(newPlayMode)
        }
        // 播放列表
        binding.btnPlayingList.onThrottleClick {
            LogUtil.logD(TAG, "showPlayingList")
            val playingListDialog = PlayingListDialog { position, _ ->
                controlViewModel.play(position)
            }
            playingListDialog.showNow(supportFragmentManager, "playingListDialog")
        }

        // 歌词
        binding.lvLyric.setLabel(getString(R.string.song_have_no_lyric))
        binding.lvLyric.setNormalTextSize(ScreenUtil.dp2px(this, 14F).toFloat())
        binding.lvLyric.setNormalColor(getColor(R.color.text_content_sub))
        binding.lvLyric.setCurrentTextSize(ScreenUtil.dp2px(this, 16F).toFloat())
        binding.lvLyric.setCurrentColor(getColor(R.color.text_theme))
        binding.lvLyric.setDraggable(true, object : OnPlayClickListener {
            override fun onPlayClick(time: Long): Boolean {
                controlViewModel.seekTo(time)
                return true
            }
        })
        binding.lvLyric.setTimelineColor(getColor(R.color.window_layer_black))
        binding.lvLyric.setTimelineTextColor(getColor(R.color.text_title))
        binding.lvLyric.setTimeTextColor(getColor(R.color.text_title))
        binding.lvLyric.setOnSingerClickListener(object : OnSingleClickListener {
            override fun onClick() {
                LogUtil.logD(TAG, "lvLyric onClick")
                binding.lvLyric.setGone(true)
                binding.llContent.setGone(false)
            }
        })
    }

    private fun initViewModel() {
        controlViewModel.localAudios
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
        controlViewModel.playMode
            .onEach { notifyPlayModeChanged(it) }
            .launchIn(lifecycleScope)
        controlViewModel.playError
            .onEach { notifyPlayErrorChanged(it) }
            .launchIn(lifecycleScope)
    }

    /**
     * 更新页面数据
     */
    private fun notifyLocalFilesChanged(state: UIState<List<LocalAudio>>) {
        LogUtil.logD(TAG, "notifyLocalFilesChanged: $state")
        when (state) {
            is UIState.Error -> { finish() }
            else -> {}
        }
    }

    /**
     * 更新播放项
     */
    private fun notifyPlayingItemChanged(playingItem: LocalAudio?) {
        LogUtil.logD(TAG, "notifyPlayingItemChanged: $playingItem")
        binding.tvTitle.text = playingItem?.displayName
        binding.tvArtist.text = playingItem?.artist
        binding.ivCover.load(playingItem) {
            transformations(CircleCropTransformation())
            error(R.drawable.ic_album)
        }
        if (playingItem != null) {
            val lrcFile = File(playingItem.path.substringBefore(".") + ".lrc")
            if (lrcFile.exists()) {
                binding.lvLyric.loadLyric(lrcFile.readText())
            } else {
                binding.lvLyric.loadLyric(null)
            }
        }
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
        if (isSeekBarDragging) return
        binding.sbSeek.max = progress.duration.toInt()
        binding.sbSeek.progress = progress.position.toInt()
        binding.tvPosition.text = Utils.formatDuration(progress.position)
        binding.tvDuration.text = Utils.formatDuration(progress.duration)
        // 更新歌词
        binding.lvLyric.updateTime(progress.position)
    }

    /**
     * 更新播放模式
     */
    private fun notifyPlayModeChanged(playMode: BaseControlViewModel.PlayMode) {
        LogUtil.logD(TAG, "notifyPlayModeChanged: $playMode")
        when (controlViewModel.playMode.value) {
            BaseControlViewModel.PlayMode.ONE -> {
                binding.btnPlayMode.setImageResource(R.drawable.ic_mode_repeat_one)
            }
            BaseControlViewModel.PlayMode.LIST -> {
                binding.btnPlayMode.setImageResource(R.drawable.ic_mode_repeat_list)
            }
            BaseControlViewModel.PlayMode.ALL -> {
                binding.btnPlayMode.setImageResource(R.drawable.ic_mode_repeat_all)
            }
            BaseControlViewModel.PlayMode.SHUFFLE -> {
                binding.btnPlayMode.setImageResource(R.drawable.ic_mode_shuffle)
            }
        }
    }

    /**
     * 更新播放异常
     */
    private fun notifyPlayErrorChanged(playError: Throwable) {
        LogUtil.logW(TAG, "notifyPlayErrorChanged: $playError")
        showToast(R.string.play_error)
    }
}