package com.senierr.base.support.arch

/**
 * 事件数据
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
sealed class FlowEvent<out T> {

    /**
     * 进度
     *
     * @param totalSize 总大小
     * @param currentSize 当前大小
     */
    data class Progress(val totalSize: Long, val currentSize: Long): FlowEvent<Nothing>()

    /**
     * 成功
     *
     * @param value 数据
     */
    data class Success<out T>(val value: T?): FlowEvent<T>()

    /**
     * 失败
     *
     * @param throwable 异常
     */
    data class Failure(val throwable: Throwable?): FlowEvent<Nothing>()
}