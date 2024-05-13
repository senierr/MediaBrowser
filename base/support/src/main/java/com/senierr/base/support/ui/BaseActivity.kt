package com.senierr.base.support.ui

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.senierr.base.util.LogUtil

/**
 * Activity基类
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
abstract class BaseActivity<VB: ViewBinding> : AppCompatActivity() {

    protected val TAG: String = this.javaClass.simpleName

    protected lateinit var binding: VB

    /**
     * 创建视图绑定
     */
    abstract fun createViewBinding(layoutInflater: LayoutInflater): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = createViewBinding(layoutInflater)
        setContentView(binding.root)
        LogUtil.logI(TAG, "onCreate")
    }

    override fun onResume() {
        super.onResume()
        LogUtil.logI(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        LogUtil.logI(TAG, "onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.logI(TAG, "onDestroy")
    }
}