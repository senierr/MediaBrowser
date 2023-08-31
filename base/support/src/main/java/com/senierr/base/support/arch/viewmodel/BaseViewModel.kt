package com.senierr.base.support.arch.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * ViewModel基类
 *
 * @author senierr_zhou
 * @date 2023/08/08
 */
open class BaseViewModel : ViewModel() {

    protected val TAG: String = this.javaClass.simpleName

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

    override fun onCleared() {
        jobMap.clear()
        super.onCleared()
    }
}