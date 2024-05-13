package com.senierr.base.support.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.senierr.base.util.LogUtil

/**
 * BottomSheetDialogFragment基类
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
abstract class BaseBottomDialogFragment<VB: ViewBinding> : BottomSheetDialogFragment() {

    protected val TAG: String = this.javaClass.simpleName

    protected var binding: VB? = null

    /**
     * 创建视图绑定
     */
    abstract fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = createViewBinding(inflater, container)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LogUtil.logI(TAG, "onViewCreated")
    }

    override fun onStart() {
        super.onStart()
        LogUtil.logI(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        LogUtil.logI(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        LogUtil.logI(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        LogUtil.logI(TAG, "onStop")
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
        LogUtil.logI(TAG, "onDestroyView")
    }
}