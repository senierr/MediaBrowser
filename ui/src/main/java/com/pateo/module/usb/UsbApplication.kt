package com.pateo.module.usb

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.qinggan.usbvideo.repository.UsbRepository
import com.senierr.base.util.LogUtil

/**
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
object UsbApplication : ViewModelStoreOwner {

    private const val TAG = "SessionApplication"

    private lateinit var application: Application

    private val _viewModelStore = ViewModelStore()

    fun onCreate(context: Application) {
        Log.i(TAG, "onCreate")
        application = context

        LogUtil.isDebug = true

        UsbRepository.initialize(application)
    }

    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    fun getContext(): Application = application
}