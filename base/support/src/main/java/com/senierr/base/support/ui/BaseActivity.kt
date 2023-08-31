package com.senierr.base.support.ui

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.senierr.base.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Activity基类
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
abstract class BaseActivity<VB: ViewBinding> : AppCompatActivity() {

    protected val TAG: String = this.javaClass.simpleName

    protected lateinit var binding: VB

    private val jobMap = hashMapOf<String, Job>()

    protected fun CoroutineScope.launchSingle(
        tag: String,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        // 若有相同tag任务，先结束之前的
        val oldJob = jobMap[tag]
        oldJob?.let { oldJob.cancel() }
        // 执行新任务
        val newJob = launch(context, start, block)
        // 缓存新任务
        jobMap[tag] = newJob
        return newJob
    }

    /**
     * 创建视图绑定
     */
    abstract fun createViewBinding(layoutInflater: LayoutInflater): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = createViewBinding(layoutInflater)
        setContentView(binding.root)
        LogUtil.logI(TAG, "onCreate")
    }

    override fun onResume() {
        super.onResume()
        LogUtil.logI(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        LogUtil.logI(TAG, "onPause")
    }

    override fun onDestroy() {
        jobMap.clear()
        super.onDestroy()
        LogUtil.logI(TAG, "onDestroy")
    }
}