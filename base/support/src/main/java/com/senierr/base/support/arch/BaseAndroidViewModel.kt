package com.senierr.base.support.arch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.senierr.base.util.LogUtil

/**
 * AndroidViewModel基类
 *
 * @author senierr_zhou
 * @date 2023/08/08
 */
open class BaseAndroidViewModel(application: Application) : AndroidViewModel(application) {

    protected val TAG: String = this.javaClass.simpleName

    override fun onCleared() {
        super.onCleared()
        LogUtil.logI(TAG, "onCleared")
    }
}