package com.senierr.media.domain.audio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import com.senierr.base.util.LogUtil
import com.senierr.media.R

/**
 * @author senierr_zhou
 * @date 2022/08/13
 */
class AudioMediaBrowserService : MediaBrowserServiceCompat() {

    companion object {
        private const val TAG = "XmlyMediaBrowserService"
        private const val MC_RECENT_ROOT = "__RECENT__"
        private const val CHANNEL_ID = "100"
        private const val CHANNEL_NAME = "XmlyMediaBrowserService"
    }

    private var mediaSession: AudioMediaSession? = null

    override fun onCreate() {
        super.onCreate()
        LogUtil.logD(TAG, "onCreate")
        mediaSession = AudioMediaSession(this, TAG)
        mediaSession?.isActive = true
        sessionToken = mediaSession?.sessionToken
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(CHANNEL_NAME)
            .setContentText(CHANNEL_NAME)
            .setSmallIcon(R.drawable.ic_audio)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        LogUtil.logD(TAG, "onDestroy")
        mediaSession?.release()
        super.onDestroy()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        LogUtil.logD(TAG, "onGetRoot: clientPackageName=$clientPackageName")
        return BrowserRoot(MC_RECENT_ROOT, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        LogUtil.logD(TAG, "onLoadChildren")
        result.sendResult(null)
    }
}