package com.pateo.module.usb.ktx

import com.senierr.base.widget.MultiStateView

/**
 * 多状态布局扩展函数
 *
 * @author zhouchunjie
 * @date 2020/5/23 22:08
 */

/**
 * 显示加载进度
 */
fun MultiStateView.showLoadingView() {
    viewState = MultiStateView.VIEW_STATE_LOADING
}

/**
 * 显示内容
 */
fun MultiStateView.showContentView() {
    viewState = MultiStateView.VIEW_STATE_CONTENT
}

/**
 * 显示空布局
 */
fun MultiStateView.showEmptyView() {
    viewState = MultiStateView.VIEW_STATE_EMPTY
}

/**
 * 显示U盘未挂载布局
 */
fun MultiStateView.showUnmountView() {
    viewState = MultiStateView.VIEW_STATE_ERROR
}
