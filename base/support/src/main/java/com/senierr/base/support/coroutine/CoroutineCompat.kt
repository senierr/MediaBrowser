package com.senierr.base.support.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 协程支持类
 *
 * @author senierr_zhou
 * @date 2024/05/13
 */
class CoroutineCompat(
    private val coroutineScope: CoroutineScope,
    private val jobMap: HashMap<String, Job> = hashMapOf()
) {

    init {
        coroutineScope.launch {
            try {
                awaitCancellation()
            } finally {
                jobMap.clear()
            }
        }
    }

    fun launchSingle(
        tag: String,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        // 若有相同tag任务，先结束之前的
        val oldJob = jobMap[tag]
        oldJob?.let { oldJob.cancel() }
        // 执行新任务
        val newJob = coroutineScope.launch(context, start, block)
        // 缓存新任务
        jobMap[tag] = newJob
        return newJob
    }
}