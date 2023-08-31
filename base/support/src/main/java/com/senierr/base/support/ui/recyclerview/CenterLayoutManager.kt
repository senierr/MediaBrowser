package com.senierr.base.support.ui.recyclerview

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 * 滚动至中心LinearLayoutManager
 *
 * @author senierr_zhou
 * @date 2023/07/30
 */
class CenterLayoutManager : LinearLayoutManager {

    companion object {
        // 显示到中间的动画时长
        private const val DURATION_DEFAULT = 400f
    }

    var duration = DURATION_DEFAULT

    private var lastPosition = 0
    private var targetPosition = 0

    constructor(context: Context) : super(context)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(
        context: Context,
        orientation: Int,
        reverseLayout: Boolean
    ) : super(context, orientation, reverseLayout)

    override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
        val smoothScroller: CenterSmoothScroller =
            object : CenterSmoothScroller(recyclerView.context) {
                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                    // 重新计算相近两个位置的滚动间隔
                    val newDuration = duration / abs(targetPosition - lastPosition)
                    return newDuration / displayMetrics.densityDpi
                }
            }
        smoothScroller.targetPosition = position
        lastPosition = targetPosition
        targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    open class CenterSmoothScroller(context: Context?) : LinearSmoothScroller(context) {
        override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
            return boxStart + (boxEnd - boxStart) / 2 - (viewStart + (viewEnd - viewStart) / 2)
        }
    }
}