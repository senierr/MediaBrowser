package com.senierr.media.domain.audio.service

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.senierr.base.util.LogUtil

/**
 * @author senierr_zhou
 * @date 2022/08/13
 */
class AudioMediaBrowserService : MediaBrowserServiceCompat() {

    companion object {
        private const val TAG = "AudioMediaBrowserService"
        private const val MC_RECENT_ROOT = "__RECENT__"
    }

    private var mediaSession: AudioMediaSession? = null

    override fun onCreate() {
        super.onCreate()
        LogUtil.logD(TAG, "onCreate")
        mediaSession = AudioMediaSession(this, this, TAG)
        mediaSession?.isActive = true
        sessionToken = mediaSession?.sessionToken
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mediaSession?.let { MediaButtonReceiver.handleIntent(it, intent) }
        return super.onStartCommand(intent, flags, startId)
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