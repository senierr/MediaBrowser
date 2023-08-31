package com.senierr.base.support.arch.viewmodel.event

/**
 * 执行事件
 *
 * @author senierr
 * @date 2023/8/18
 */
sealed class UIEvent<out T> {

    /**
     * 开始
     */
    object Start: UIEvent<Nothing>()

    /**
     * 进度
     *
     * @param position 进度
     * @param duration 总进度
     */
    data class Progress(val position: Long, val duration: Long): UIEvent<Nothing>()

    /**
     * 成功
     *
     * @param value 数据
     */
    data class Success<out T>(val value: T): UIEvent<T>()

    /**
     * 失败
     *
     * @param throwable 异常
     */
    data class Failure(val throwable: Throwable?): UIEvent<Nothing>()
}