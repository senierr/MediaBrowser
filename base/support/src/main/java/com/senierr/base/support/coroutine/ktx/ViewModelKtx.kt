package com.senierr.base.support.coroutine.ktx

import android.app.Application
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

/**
 * ViewModel扩展函数
 *
 * @author chunjiezhou
 * @date 2020/12/25
 */
@MainThread
inline fun <reified VM : ViewModel> ViewModelStoreOwner.viewModel(): Lazy<VM> {
    return object : Lazy<VM> {
        private var cached: VM? = null

        override val value: VM
            get() {
                val viewModel = cached
                return viewModel ?: ViewModelProvider(this@viewModel)[VM::class.java].also { cached = it }
            }

        override fun isInitialized() = cached != null
    }
}

@MainThread
inline fun <reified VM : ViewModel> Fragment.activityViewModel(): Lazy<VM> {
    return object : Lazy<VM> {
        private var cached: VM? = null

        override val value: VM
            get() {
                val viewModel = cached
                return viewModel ?: ViewModelProvider(requireActivity())[VM::class.java].also { cached = it }
            }

        override fun isInitialized() = cached != null
    }
}

@MainThread
inline fun <reified VM : ViewModel> ViewModelStoreOwner.androidViewModel(application: Application): Lazy<VM> {
    return object : Lazy<VM> {
        private var cached: VM? = null

        override val value: VM
            get() {
                val viewModel = cached
                return viewModel ?: ViewModelProvider(
                    this@androidViewModel, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                )[VM::class.java].also { cached = it }
            }

        override fun isInitialized() = cached != null
    }
}

@MainThread
inline fun <reified VM : ViewModel> Fragment.activityAndroidViewModel(application: Application): Lazy<VM> {
    return object : Lazy<VM> {
        private var cached: VM? = null

        override val value: VM
            get() {
                val viewModel = cached
                return viewModel ?: ViewModelProvider(
                    requireActivity(), ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                )[VM::class.java].also { cached = it }
            }

        override fun isInitialized() = cached != null
    }
}