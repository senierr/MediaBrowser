package com.senierr.media.ktx

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.senierr.media.SessionApplication

/**
 * ViewModel扩展函数
 *
 * @author chunjiezhou
 * @date 2020/12/25
 */

@MainThread
inline fun <reified VM : ViewModel> applicationViewModel(): Lazy<VM> {
    return object : Lazy<VM> {
        private var cached: VM? = null

        override val value: VM
            get() {
                val viewModel = cached
                return viewModel ?: ViewModelProvider(SessionApplication.getInstance())[VM::class.java].also { cached = it }
            }

        override fun isInitialized() = cached != null
    }
}

@MainThread
inline fun <reified VM : ViewModel> applicationAndroidViewModel(): Lazy<VM> {
    return object : Lazy<VM> {
        private var cached: VM? = null

        override val value: VM
            get() {
                val viewModel = cached
                return viewModel ?: ViewModelProvider(
                    SessionApplication.getInstance(),
                    ViewModelProvider.AndroidViewModelFactory.getInstance(SessionApplication.getInstance())
                )[VM::class.java].also { cached = it }
            }

        override fun isInitialized() = cached != null
    }
}