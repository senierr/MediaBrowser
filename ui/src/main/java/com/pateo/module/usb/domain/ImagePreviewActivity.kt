package com.pateo.module.usb.domain

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.pateo.module.usb.databinding.ActivityUsbImagePreviewBinding
import com.pateo.module.usb.domain.viewmodel.UsbMainViewModel
import com.pateo.module.usb.domain.wrapper.UsbImagePreviewWrapper
import com.pateo.module.usb.ktx.applicationViewModel
import com.pateo.module.usb.utils.DiffUtils
import com.senierr.media.repository.entity.UsbFile
import com.senierr.media.repository.entity.UsbImage
import com.senierr.media.repository.entity.UsbStatus
import com.senierr.adapter.internal.MultiTypeAdapter
import com.senierr.base.support.ui.BaseActivity
import com.senierr.base.util.LogUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 图片预览页面
 *
 * @author senierr_zhou
 * @date 2023/07/29
 */
class ImagePreviewActivity : BaseActivity<ActivityUsbImagePreviewBinding>() {

    companion object {
        private const val INTERVAL_CONTROL_BAR_HIDE = 3 * 1000L

        fun start(context: Context, imageId: Long) {
            context.startActivity(Intent(context, com.pateo.module.usb.domain.ImagePreviewActivity::class.java).apply {
                putExtra("imageId", imageId)
            })
        }
    }

    // 当前浏览图片IDs
    private val currentImageId: Long by lazy { intent.getLongExtra("imageId", -1) }

    private val multiTypeAdapter = MultiTypeAdapter()
    private val imagePreviewWrapper = UsbImagePreviewWrapper()

    private val usbMainViewModel by applicationViewModel<UsbMainViewModel>()

    // 更新数据任务
    private var notifyDataChangedJob: Job? = null
    // 隐藏控制栏任务
    private var hideControlBarJob: Job? = null

    override fun createViewBinding(layoutInflater: LayoutInflater): ActivityUsbImagePreviewBinding {
        return ActivityUsbImagePreviewBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initViewModel()
    }

    private fun initView() {
        setSupportActionBar(binding.tbTop)
        binding.tbTop.setNavigationOnClickListener { onBackPressed() }

        imagePreviewWrapper.onPhotoTapListener =
            com.pateo.module.usb.widget.photoview.OnPhotoTapListener { _, _, _ ->
                // 控制栏状态切换
                if (binding.tbTop.visibility == View.GONE) {
                    showControlBar()
                } else {
                    hideControlBar()
                }
            }
        multiTypeAdapter.register(imagePreviewWrapper)
        binding.vpMain.adapter = multiTypeAdapter
        binding.vpMain.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                LogUtil.logD(TAG, "onPageSelected: $position")
                val usbImage = multiTypeAdapter.data.filterIsInstance<UsbImage>().elementAtOrNull(position)
                usbImage?.let {
                    notifyTitle(it)
                }
            }
        })
    }

    private fun initViewModel() {
        usbMainViewModel.usbStatus
            .onEach {
                LogUtil.logD(TAG, "usbStatus-onChanged: $it")
                if (it.action == UsbStatus.ACTION_EJECT) {
                    finish()
                }
            }
            .launchIn(lifecycleScope)
        usbMainViewModel.usbFiles
            .onEach {
                val images = it.filterIsInstance<UsbImage>()
                LogUtil.logD(TAG, "usbFiles-onChanged: ${images.size}")
                if (images.isEmpty()) {
                    finish()
                } else {
                    notifyDataChanged(images)
                }
            }
            .launchIn(lifecycleScope)
    }

    /**
     * 显示控制栏
     */
    private fun showControlBar() {
        binding.tbTop.visibility = View.VISIBLE
        hideControlBar()
    }

    /**
     * 隐藏控制栏
     */
    private fun hideControlBar(delayTimes: Long = com.pateo.module.usb.domain.ImagePreviewActivity.Companion.INTERVAL_CONTROL_BAR_HIDE) {
        hideControlBarJob?.cancel()
        hideControlBarJob = lifecycleScope.launch {
            delay(delayTimes)
            binding.tbTop.visibility = View.GONE
        }
    }

    /**
     * 更新标题信息
     */
    private fun notifyTitle(usbImage: UsbImage) {
        Log.d(TAG, "notifyTitle: $usbImage")
        binding.tbTop.title = usbImage.displayName
        binding.tbTop.subtitle = "${binding.vpMain.currentItem + 1}/${multiTypeAdapter.data.size}"
    }

    /**
     * 更新页面数据
     */
    private fun notifyDataChanged(newList: List<UsbFile>) {
        notifyDataChangedJob?.cancel()
        notifyDataChangedJob = lifecycleScope.launch {
            val oldList = multiTypeAdapter.data.filterIsInstance<UsbFile>()
            val diffResult = DiffUtils.diffUsbFile(oldList, newList)
            if (isActive) {
                diffResult.dispatchUpdatesTo(multiTypeAdapter)
                multiTypeAdapter.data.clear()
                multiTypeAdapter.data.addAll(newList)
                Log.d(TAG, "notifyDataChanged: ${oldList.size} -> ${newList.size}")
                // 重置页面浏览索引
                val position = newList.filterIsInstance<UsbImage>().indexOfFirst { it.id == currentImageId }
                if (position >= 0) {
                    binding.vpMain.setCurrentItem(position, false)
                }
            }
        }
    }
}