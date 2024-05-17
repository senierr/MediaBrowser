package com.senierr.media.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import com.senierr.media.R
import java.lang.ref.WeakReference
import java.util.Random

/**
 * 音乐正在播放时的柱状图
 *
 * @author senierr_zhou
 * @date 2024/04/02
 */
class PlayingIcon @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    companion object {
        private const val LOOP_ACTION = 99
        private const val PLAYING_DANCE_INTERVAL = 240L
    }

    /**
     * 指示柱高度
     */
    class Pointer(var height: Float)

    /**
     * 画笔
     */
    private lateinit var paint: Paint

    /**
     * 跳动指示柱的集合
     */
    private var pointers: MutableList<Pointer> = mutableListOf()

    /**
     * 跳动指示柱的数量，默认为 4根
     */
    private var pointerNum = 4

    /**
     * 逻辑坐标 原点（左下）
     */
    private var basePointX = 0f
    private var basePointY = 0f

    /**
     * 指示柱间的间隙  默认5dp
     */
    private var pointerPadding = 5f

    /**
     * 每个指示柱的宽度 默认3dp
     */
    private var pointerWidth = 3f

    /**
     * 指示柱的颜色
     */
    private var pointerColor = Color.RED

    /**
     * 控制开始/停止
     */
    @Volatile
    var isPlaying = false
        private set

    /**
     * 指示柱波动速率
     */
    private var mViewWeakReference: WeakReference<View>? = null
    private var random: Random? = null
    private var mHandler: Handler? = null

    init {
        //取出自定义属性
        val ta: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.MusicPlayingIcon)
        pointerColor = ta.getColor(R.styleable.MusicPlayingIcon_pointerColor, Color.RED)
        //指针的数量，默认为4
        pointerNum = ta.getInt(R.styleable.MusicPlayingIcon_pointerNum, 4)
        //指针的宽度，默认5dp
        pointerWidth = ta.getDimensionPixelSize(R.styleable.MusicPlayingIcon_pointerWidth, 5).toFloat()
        ta.recycle()
        initView()
    }

    /**
     * 初始化画笔与指示柱的集合
     */
    private fun initView() {
        mHandler = Handler(Looper.getMainLooper()) { msg ->
            if (msg.what == LOOP_ACTION) {
                // 循环改变每个指针高度
                for (j in pointers.indices) {
                    // 获取0~1的数。
                    val rate = Math.random().toFloat()
                    // rate 乘以 可绘制高度，来改变每个指针的高度
                    if (j < pointers.size) {
                        pointers[j].height = (basePointY - paddingTop) * rate
                    }
                }
                // 通知刷新
                val view = mViewWeakReference!!.get()
                view?.postInvalidate()
                loop()
            }
            false
        }
        paint = Paint()
        paint.isAntiAlias = true
        paint.setColor(pointerColor)
        mViewWeakReference = WeakReference(this)
        random = Random()
        // 初始化的时候，就把pointer对象创建了，放在list中
        for (i in 0 until pointerNum) {
            pointers.add(Pointer(0f))
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // 获取逻辑原点的，也就是画布左下角的坐标。这里减去了paddingBottom的距离
        basePointY = (height - paddingBottom).toFloat()
        for (i in pointers.indices) {
            // 创建指针对象，利用0~1的随机数 乘以 可绘制区域的高度。作为每个指针的初始高度。
            val randomPointHeight = (0.1 * (random!!.nextInt(10) + 1) * (height - paddingBottom - paddingTop)).toFloat()
            pointers[i].height = randomPointHeight
        }
        // 计算每个指针之间的间隔  总宽度 - 左右两边的padding - 所有指针占去的宽度  然后再除以间隔的数量
        pointerPadding = (width - getPaddingLeft() - getPaddingRight() - pointerWidth * pointerNum) / (pointerNum - 1)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 将x坐标移动到逻辑原点，也就是左下角
        basePointX = 0f + getPaddingLeft()
        // 循环绘制每一个指针。
        for (i in pointers.indices) {
            // 绘制指针，也就是绘制矩形
            canvas.drawRect(basePointX, basePointY - pointers[i].height, basePointX + pointerWidth, basePointY, paint)
            basePointX += pointerPadding + pointerWidth
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            if (isPlaying) {
                start()
            }
        } else {
            mHandler?.removeMessages(LOOP_ACTION)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isPlaying) {
            start()
        }
    }

    override fun onDetachedFromWindow() {
        // 此处之所以只是移除 循环任务，是因为这个view看不见了，等这个view能看见了，需要继续跳动，还是延续之前的逻辑
        mHandler?.removeMessages(LOOP_ACTION)
        super.onDetachedFromWindow()
    }

    private fun start() {
        loop()
        isPlaying = true
    }

    private fun loop() {
        mHandler?.removeMessages(LOOP_ACTION)
        mHandler?.sendEmptyMessageDelayed(LOOP_ACTION, PLAYING_DANCE_INTERVAL)
    }

    private fun stop() {
        mHandler?.removeMessages(LOOP_ACTION)
        isPlaying = false
    }

    /**
     * 开始播放
     */
    fun setMusicPlaying(isPlaying: Boolean) {
        if (isPlaying) {
            start()
        } else {
            stop()
        }
    }

    /**
     * 设置律动条颜色
     */
    fun setIconColor(color: Int) {
        pointerColor = color
        paint.setColor(color)
    }
}