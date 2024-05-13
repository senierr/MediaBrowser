package com.senierr.base.support.arch

/**
 * 页面状态
 *
 * @author senierr
 * @date 2023/8/18
 */
sealed class UIState<out T> {

    /**
     * 正在加载
     */
    object Loading: UIState<Nothing>()

    /**
     * 数据内容
     *
     * @param value 数据
     */
    data class Content<out T>(val value: T): UIState<T>()

    /**
     * 无数据
     */
    object Empty: UIState<Nothing>()

    /**
     * 异常
     */
    data class Error(val throwable: Throwable?): UIState<Nothing>()
}