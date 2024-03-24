package com.pateo.module.usb

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.senierr.media.repository.MediaRepository
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

        MediaRepository.initialize(application)
    }

    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    fun getContext(): Application = application
}