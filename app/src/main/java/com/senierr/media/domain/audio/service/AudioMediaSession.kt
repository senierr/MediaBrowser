package com.senierr.media.domain.audio.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.senierr.base.support.ktx.showToast
import com.senierr.base.util.LogUtil
import com.senierr.media.R
import com.senierr.media.domain.audio.viewmodel.AudioControlViewModel
import com.senierr.media.domain.audio.viewmodel.BaseControlViewModel
import com.senierr.media.ktx.applicationViewModel
import com.senierr.media.repository.entity.LocalAudio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 媒体会话
 *
 * @author senierr_zhou
 * @date 2022/08/14
 */
@SuppressLint("RestrictedApi")
class AudioMediaSession(
    private val context: Context,
    tag: String
) : MediaSessionCompat(context, tag), CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Main) {

    companion object {
        private const val TAG = "XmlyMediaSession"

        // 状态同步
        private const val ACTION_SYNC_STATUS = "com.pateo.sgmw.media.ACTION_SYNC_STATUS"
        // 收藏
        private const val ACTION_FAVORITE = "com.pateo.sgmw.media.ACTION_FAVORITE"

        // 进度
        private const val EVENT_PROGRESS = "com.pateo.sgmw.media.EVENT_PROGRESS"
        // 收藏状态
        private const val EVENT_FAVORITE = "com.pateo.sgmw.media.EVENT_FAVORITE"

        private const val KEY_POSITION = "POSITION"
        private const val KEY_DURATION = "DURATION"
        private const val KEY_IS_FAVORITE = "IS_FAVORITE"
    }

    private val controlViewModel: AudioControlViewModel by applicationViewModel()

    // 指令接收
    private val callback: Callback = object : Callback() {
        override fun onPlay() {
            LogUtil.logD(TAG, "onPlay")
            controlViewModel.play()
        }

        override fun onPause() {
            LogUtil.logD(TAG, "onPause")
            controlViewModel.pause(true)
        }

        override fun onSkipToNext() {
            val hasNext = controlViewModel.hasNextItem()
            LogUtil.logD(TAG, "onSkipToNext: $hasNext")
            if (hasNext) {
                controlViewModel.skipToNext()
            } else {
                context.showToast(R.string.has_no_next)
            }
        }

        override fun onSkipToPrevious() {
            val hasPrevious = controlViewModel.hasPreviousItem()
            LogUtil.logD(TAG, "onSkipToPrevious: $hasPrevious")
            if (hasPrevious) {
                controlViewModel.skipToPrevious()
            } else {
                context.showToast(R.string.has_no_previous)
            }
        }

        override fun onCustomAction(action: String, extras: Bundle) {
            Log.i(TAG, "onCustomAction: $action, $extras")
        }
    }

    init {
        setCallback(callback, Handler(Looper.getMainLooper()))
        initViewModel()
    }

    override fun release() {
        LogUtil.logD(TAG, "release")
        cancel()
        super.release()
    }

    private fun initViewModel() {
        controlViewModel.playingItem
            .onEach { notifyPlayingItemChanged(it) }
            .launchIn(this)
        controlViewModel.playStatus
            .onEach { notifyIsPlayingChanged(it) }
            .launchIn(this)
        controlViewModel.progress
            .onEach { notifyProgressChanged(it) }
            .launchIn(this)
        controlViewModel.playMode
            .onEach { notifyPlayModeChanged(it) }
            .launchIn(this)
    }

    /**
     * 播放媒体变更
     */
    private fun notifyPlayingItemChanged(playingItem: LocalAudio?) {
        LogUtil.logD(TAG, "notifyPlayingItemChanged: ${playingItem?.displayName}")
        if (playingItem == null) {
            setMetadata(null)
        } else {
            val mediaMetadata: MediaMetadataCompat = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, playingItem.displayName)
//                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mediaItem.mediaMetadata.artist.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, playingItem.getUri().toString())
                .build()
            setMetadata(mediaMetadata)
        }
    }

    /**
     * 播放状态变更
     */
    private fun notifyIsPlayingChanged(isPlaying: Boolean) {
        LogUtil.logD(TAG, "notifyIsPlayingChanged: $isPlaying")
        val state = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, 0, 1.0f)
            .build()
        setPlaybackState(playbackState)
    }

    /**
     * 播放进度变更
     */
    private fun notifyProgressChanged(progress: BaseControlViewModel.Progress) {
        sendSessionEvent(
            EVENT_PROGRESS,
            Bundle().apply {
                putLong(KEY_POSITION, progress.position)
                putLong(KEY_DURATION, progress.duration)
            }
        )
    }

    /**
     * 播放模式变更
     */
    private fun notifyPlayModeChanged(playMode: BaseControlViewModel.PlayMode) {
        LogUtil.logD(TAG, "notifyPlayModeChanged: $playMode")
//        sendSessionEvent(
//            EVENT_FAVORITE,
//            Bundle().apply {
//                putBoolean(KEY_IS_FAVORITE, isFavorite)
//            }
//        )
//
//        when (controlViewModel.playMode.value) {
//            BaseControlViewModel.PlayMode.ONE -> {
//                binding.btnPlayMode.setImageResource(R.drawable.ic_mode_repeat_one)
//            }
//            BaseControlViewModel.PlayMode.LIST -> {
//                binding.btnPlayMode.setImageResource(R.drawable.ic_mode_repeat_list)
//            }
//            BaseControlViewModel.PlayMode.ALL -> {
//                binding.btnPlayMode.setImageResource(R.drawable.ic_mode_repeat_all)
//            }
//            BaseControlViewModel.PlayMode.SHUFFLE -> {
//                binding.btnPlayMode.setImageResource(R.drawable.ic_mode_shuffle)
//            }
//        }
    }
}