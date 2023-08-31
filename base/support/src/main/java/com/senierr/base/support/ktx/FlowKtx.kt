package com.senierr.base.support.ktx

import com.senierr.base.support.arch.FlowEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach

/**
 * Flow扩展函数
 *
 * @author chunjiezhou
 * @date 2021/02/26
 */

suspend fun <T> MutableSharedFlow<FlowEvent<T>>.emitProgress(totalSize: Long, currentSize: Long) {
    this.emit(FlowEvent.Progress(totalSize, currentSize))
}

suspend fun <T> MutableSharedFlow<FlowEvent<T>>.emitSuccess(value: T?) {
    this.emit(FlowEvent.Success(value))
}

suspend fun <T> MutableSharedFlow<FlowEvent<T>>.emitFailure(throwable: Throwable?) {
    this.emit(FlowEvent.Failure(throwable))
}

fun <T> Flow<FlowEvent<T>>.onProgress(
    action: suspend (totalSize: Long, currentSize: Long) -> Unit
) = onEach {
    if (it is FlowEvent.Progress) {
        action.invoke(it.totalSize, it.currentSize)
    }
}

fun <T> Flow<FlowEvent<T>>.onSuccess(
    action: suspend (data: T?) -> Unit
) = onEach {
    if (it is FlowEvent.Success) {
        action.invoke(it.value)
    }
}

fun <T> Flow<FlowEvent<T>>.onFailure(
    action: suspend (throwable: Throwable?) -> Unit
) = onEach {
    if (it is FlowEvent.Failure) {
        action.invoke(it.throwable)
    }
}