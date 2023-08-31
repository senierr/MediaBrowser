package com.senierr.base.support.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.senierr.base.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * BottomSheetDialogFragment基类
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
abstract class BaseBottomDialogFragment<VB: ViewBinding> : BottomSheetDialogFragment() {

    protected val TAG: String = this.javaClass.simpleName

    protected var binding: VB? = null

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
    abstract fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = createViewBinding(inflater, container)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LogUtil.logI(TAG, "onViewCreated")
    }

    override fun onResume() {
        super.onResume()
        LogUtil.logI(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        LogUtil.logI(TAG, "onPause")
    }

    override fun onDestroyView() {
        binding = null
        jobMap.clear()
        super.onDestroyView()
        LogUtil.logI(TAG, "onDestroyView")
    }
}