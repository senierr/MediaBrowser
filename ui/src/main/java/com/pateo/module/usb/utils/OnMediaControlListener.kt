package com.pateo.module.usb.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.senierr.base.util.ScreenUtil
import kotlin.math.absoluteValue

/**
 *
 * @author chunjiezhou
 */
open class OnMediaControlListener : GestureDetector.SimpleOnGestureListener(), View.OnTouchListener {

    companion object {
        private const val MODEL_UNKNOWN = -1      // 未知模式
        private const val MODEL_VOLUME = 0        // 音量调节模式
        private const val MODEL_BRIGHTNESS = 1    // 亮度调节模式
        private const val MODEL_SEEK = 2          // 进度调节模式
    }

    private lateinit var context: Context
    private lateinit var gestureDetector: GestureDetector

    // 初次触发模式，保证操作统一
    private var touchModel = MODEL_UNKNOWN
    // 是否可操作
    var enable = true

    /**
     * 初始化
     */
    fun initialize(context: Context) {
        this.context = context
        this.gestureDetector = GestureDetector(context, this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            touchModel = MODEL_UNKNOWN
        }
        return gestureDetector.onTouchEvent(event)
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        onClick()
        return true
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (!enable) return true
        val screenWidth = ScreenUtil.getScreenWidth(context)
        if (e1.rawX < screenWidth / 2 && distanceX.absoluteValue < distanceY.absoluteValue
            && (touchModel == MODEL_VOLUME || touchModel == MODEL_UNKNOWN)) {
            touchModel = MODEL_VOLUME
            // 音量调节
            onVolumeChanged(distanceY)
        }
        if (e1.rawX > screenWidth / 2 && distanceX.absoluteValue < distanceY.absoluteValue
            && (touchModel == MODEL_BRIGHTNESS || touchModel == MODEL_UNKNOWN)) {
            touchModel = MODEL_BRIGHTNESS
            // 亮度调节
            onBrightnessChanged(distanceY)
        }
        if (distanceX.absoluteValue > distanceY.absoluteValue
            && (touchModel == MODEL_SEEK || touchModel == MODEL_UNKNOWN)) {
            touchModel = MODEL_SEEK
            // 进度回调
            onSeekChanged(distanceX)
        }
        return true
    }

    /**
     * 点击回调
     */
    open fun onClick() {}

    /**
     * 音量变更回调
     */
    open fun onVolumeChanged(distanceY: Float) {}

    /**
     * 亮度变更回调
     */
    open fun onBrightnessChanged(distanceY: Float) {}

    /**
     * 进度变更回调
     */
    open fun onSeekChanged(distanceX: Float) {}
}