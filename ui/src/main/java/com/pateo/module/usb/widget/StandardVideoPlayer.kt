package com.pateo.module.usb.widget

import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.WindowManager
import com.pateo.module.usb.R
import com.senierr.base.util.LogUtil
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer


/**
 * 拦截原控件控制，交由自定义控制
 *
 * @author senierr_zhou
 * @date 2023/08/01
 */
class StandardVideoPlayer : StandardGSYVideoPlayer {

    companion object {
        private const val TAG = "StandardVideoPlayer"
    }

    private var audioManager: AudioManager? = null
    // 音频焦点申请回调
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        LogUtil.logD(TAG, "audioFocusChangeListener: $focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> onGankAudio()
            AudioManager.AUDIOFOCUS_LOSS -> onLossAudio()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> onLossTransientAudio()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onLossTransientCanDuck()
        }
    }

    constructor(context: Context?): super(context)
    constructor(context: Context?, attrs: AttributeSet?): super(context, attrs)

    override fun init(context: Context?) {
        super.init(context)
        audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
    }

    override fun getLayoutId(): Int = R.layout.layout_standard_video_player

    override fun touchSurfaceMoveFullLogic(absDeltaX: Float, absDeltaY: Float) {
        super.touchSurfaceMoveFullLogic(absDeltaX, absDeltaY)
        // 不给触摸快进
        mChangePosition = false
        // 不给触摸音量
        mChangeVolume = false
        // 不给触摸亮度
        mBrightness = false
    }

    override fun touchDoubleUp(e: MotionEvent) {
        // 不需要双击暂停
    }

    /**
     * 重写音频焦点申请逻辑
     */
    override fun startPrepare() {
        gsyVideoManager.listener()?.onCompletion()
        mVideoAllCallBack?.onStartPrepared(mOriginUrl, mTitle, this)
        gsyVideoManager.setListener(this)
        gsyVideoManager.playTag = mPlayTag
        gsyVideoManager.playPosition = mPlayPosition
        requestAudioFocus()
        try {
            if (mContext is Activity) {
                (mContext as Activity).window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mBackUpPlayingBufferState = -1
        gsyVideoManager.prepare(mUrl, if (mMapHeadData == null) HashMap() else mMapHeadData, mLooping, mSpeed, mCache, mCachePath, mOverrideExtension)
        setStateAndUi(CURRENT_STATE_PREPAREING)
    }

    override fun onVideoResume(seek: Boolean) {
        mPauseBeforePrepared = false
        if (mCurrentState == CURRENT_STATE_PAUSE) {
            try {
                if (mCurrentPosition >= 0 && gsyVideoManager != null) {
                    if (seek) {
                        gsyVideoManager.seekTo(mCurrentPosition)
                    }
                    gsyVideoManager.start()
                    setStateAndUi(CURRENT_STATE_PLAYING)
                    if (!mReleaseWhenLossAudio) {
                        requestAudioFocus()
                    }
                    mCurrentPosition = 0
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 申请音频焦点
     */
    private fun requestAudioFocus() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(audioFocusChangeListener, handler)
            .build()
        val result = audioManager?.requestAudioFocus(focusRequest)?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

        }
    }

    /**
     * 播放/暂停
     */
    fun playOrPause() {
        clickStartIcon()
    }
}