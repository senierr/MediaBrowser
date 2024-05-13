package com.senierr.base.support.arch

import androidx.lifecycle.ViewModel
import com.senierr.base.util.LogUtil

/**
 * ViewModel基类
 *
 * @author senierr_zhou
 * @date 2023/08/08
 */
open class BaseViewModel : ViewModel() {

    protected val TAG: String = this.javaClass.simpleName

    override fun onCleared() {
        super.onCleared()
        LogUtil.logI(TAG, "onCleared")
    }
}