package com.senierr.media.domain.audio.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import coil.imageLoader
import coil.request.ImageRequest
import com.senierr.base.support.ktx.runCatchSilent
import com.senierr.base.support.ktx.showToast
import com.senierr.base.util.LogUtil
import com.senierr.media.R
import com.senierr.media.domain.audio.AudioPlayerActivity
import com.senierr.media.domain.audio.viewmodel.AudioControlViewModel
import com.senierr.media.domain.common.BaseControlViewModel
import com.senierr.media.ktx.applicationViewModel
import com.senierr.media.repository.entity.LocalAudio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 媒体会话
 *
 * @author senierr_zhou
 * @date 2022/08/14
 */
@SuppressLint("RestrictedApi")
class AudioMediaSession(
    private val context: Context,
    private val service: AudioMediaBrowserService,
    tag: String
) : MediaSessionCompat(context, tag), CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Main) {

    companion object {
        private const val TAG = "AudioMediaSession"

        private const val CHANNEL_ID = "100"
        private const val CHANNEL_NAME = "AudioMediaBrowserService"
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

    private var updateMetadataJob: Job? = null

    init {
        val manager = context.getSystemService(MediaBrowserServiceCompat.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
        val pendingIntent = PendingIntent.getActivity(context, 1, Intent(context, AudioPlayerActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        setSessionActivity(pendingIntent)
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
            updateMetadataJob?.cancel()
            updateMetadataJob = launch {
                setMetadata(createMediaMetadata(playingItem, controlViewModel.progress.value.duration))
                updateNotification()
            }
        }
    }

    /**
     * 播放状态变更
     */
    private fun notifyIsPlayingChanged(isPlaying: Boolean) {
        LogUtil.logD(TAG, "notifyIsPlayingChanged: $isPlaying")
        setPlaybackState(createPlaybackState(controlViewModel.isPlaying(), controlViewModel.progress.value.position))
    }

    /**
     * 播放进度变更
     */
    private fun notifyProgressChanged(progress: BaseControlViewModel.Progress) {
        val playingItem = controlViewModel.playingItem.value?: return
        // 更新总时长
        val currentDuration = controller.metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        if (currentDuration != progress.duration) {
            updateMetadataJob?.cancel()
            updateMetadataJob = launch {
                setMetadata(createMediaMetadata(playingItem, progress.duration))
            }
        }
        // 更新进度
        setPlaybackState(createPlaybackState(controlViewModel.isPlaying(), progress.position))
    }

    /**
     * 播放模式变更
     */
    private fun notifyPlayModeChanged(playMode: BaseControlViewModel.PlayMode) {
        LogUtil.logD(TAG, "notifyPlayModeChanged: $playMode")
        when (playMode) {
            BaseControlViewModel.PlayMode.ONE -> {
                setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
                setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
            }
            BaseControlViewModel.PlayMode.LIST -> {
                setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
                setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
            }
            BaseControlViewModel.PlayMode.ALL -> {
                setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
                setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
            }
            BaseControlViewModel.PlayMode.SHUFFLE -> {
                setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
                setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
            }
        }
    }

    private suspend fun createMediaMetadata(playingItem: LocalAudio, duration: Long): MediaMetadataCompat {
        val artIcon = runCatchSilent({
            context.imageLoader
                .execute(ImageRequest.Builder(context).data(playingItem).build())
                .drawable?.toBitmapOrNull(200, 200)
        }, {
            LogUtil.logW(TAG, "toBitmapOrNull: $it")
            null
        })
        LogUtil.logD(TAG, "createMediaMetadata: $duration")
        return MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, playingItem.id.toString())
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, playingItem.displayName)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, playingItem.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, playingItem.album)
            .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, playingItem.path)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artIcon)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
            .build()
    }

    private fun createPlaybackState(isPlaying: Boolean, position: Long): PlaybackStateCompat {
        val state = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        return PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, position, 1.0f)
            .build()
    }

    private fun updateNotification() {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(sessionToken)
                    .setShowActionsInCompactView(1)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
            )
            setContentIntent(controller.sessionActivity)
            setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            val mediaMetadata = controller.metadata
            val description = mediaMetadata.description
            setContentTitle(description.title)
            setContentText(description.subtitle)
            setSubText(description.description)
            setLargeIcon(description.iconBitmap)
            setSmallIcon(R.mipmap.ic_launcher)

            addAction(NotificationCompat.Action(
                R.drawable.ic_skip_previous,
                context.getString(R.string.skip_previous),
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            ))
            addAction(NotificationCompat.Action(
                R.drawable.ic_pause,
                context.getString(R.string.pause),
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE)
            ))
            addAction(NotificationCompat.Action(
                R.drawable.ic_skip_next,
                context.getString(R.string.skip_next),
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
            ))
        }
        service.startForeground(1, builder.build())
    }
}