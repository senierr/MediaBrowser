package com.senierr.media.domain.image

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.github.chrisbanes.photoview.OnPhotoTapListener
import com.senierr.adapter.internal.MultiTypeAdapter
import com.senierr.base.support.arch.viewmodel.state.UIState
import com.senierr.base.support.ktx.onThrottleClick
import com.senierr.base.support.ui.BaseActivity
import com.senierr.base.util.LogUtil
import com.senierr.media.databinding.ActivityImagePreviewBinding
import com.senierr.media.domain.home.viewmodel.ImageViewModel
import com.senierr.media.domain.image.wrapper.ImagePreviewWrapper
import com.senierr.media.ktx.applicationViewModel
import com.senierr.media.repository.entity.LocalFile
import com.senierr.media.repository.entity.LocalImage
import com.senierr.media.utils.DiffUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive

/**
 * 图片预览页面
 *
 * @author senierr_zhou
 * @date 2023/07/29
 */
class ImagePreviewActivity : BaseActivity<ActivityImagePreviewBinding>() {

    companion object {
        private const val INTERVAL_CONTROL_BAR_HIDE = 3 * 1000L

        fun start(context: Context, imageId: Long) {
            context.startActivity(Intent(context, ImagePreviewActivity::class.java).apply {
                putExtra("imageId", imageId)
            })
        }
    }

    // 当前浏览图片IDs
    private var currentImageId: Long = 0

    private val multiTypeAdapter = MultiTypeAdapter()
    private val imagePreviewWrapper = ImagePreviewWrapper()

    private val imageViewModel by applicationViewModel<ImageViewModel>()

    override fun createViewBinding(layoutInflater: LayoutInflater): ActivityImagePreviewBinding {
        return ActivityImagePreviewBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentImageId = intent.getLongExtra("imageId", -1)
        initView()
        initViewModel()
    }

    private fun initView() {
        binding.layoutTopBar.btnBack.onThrottleClick { finish() }
        hideControlBar(0)

        imagePreviewWrapper.onPhotoTapListener = OnPhotoTapListener { _, _, _ ->
            // 控制栏状态切换
            if (binding.layoutTopBar.root.visibility == View.GONE) {
                showControlBar()
            } else {
                hideControlBar(0)
            }
        }
        multiTypeAdapter.register(imagePreviewWrapper)
        binding.vpMain.adapter = multiTypeAdapter
        binding.vpMain.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                LogUtil.logD(TAG, "onPageSelected: $position")
                multiTypeAdapter.data
                    .filterIsInstance<LocalImage>()
                    .elementAtOrNull(position)
                    ?.let { notifyPreviewItemChanged(it) }
            }
        })
    }

    private fun initViewModel() {
        imageViewModel.localFiles
            .onEach { notifyLocalFilesChanged(it) }
            .launchIn(lifecycleScope)
    }

    /**
     * 显示控制栏
     */
    private fun showControlBar() {
        binding.layoutTopBar.root.visibility = View.VISIBLE
        hideControlBar(INTERVAL_CONTROL_BAR_HIDE)
    }

    /**
     * 隐藏控制栏
     */
    private fun hideControlBar(delayTimes: Long) {
        lifecycleScope.launchSingle("hideControlBar") {
            delay(delayTimes)
            binding.layoutTopBar.root.visibility = View.GONE
        }
    }

    /**
     * 更新标题信息
     */
    private fun notifyPreviewItemChanged(localImage: LocalImage) {
        Log.d(TAG, "notifyPreviewItemChanged: $localImage")
        currentImageId = localImage.id

        binding.layoutTopBar.tvTitle.text = localImage.displayName
//        binding.tbTop.subtitle = "${binding.vpMain.currentItem + 1}/${multiTypeAdapter.data.size}"
    }

    /**
     * 更新页面数据
     */
    private fun notifyLocalFilesChanged(state: UIState<List<LocalFile>>) {
        LogUtil.logD(TAG, "notifyLocalFilesChanged: $state")
        when (state) {
            is UIState.Content -> {
                lifecycleScope.launchSingle("notifyLocalFilesChanged") {
                    val oldList = multiTypeAdapter.data.filterIsInstance<LocalImage>()
                    val newList = state.value.filterIsInstance<LocalImage>()
                    val diffResult = DiffUtils.diffLocalFile(oldList, newList)
                    if (isActive) {
                        diffResult.dispatchUpdatesTo(multiTypeAdapter)
                        multiTypeAdapter.data.clear()
                        multiTypeAdapter.data.addAll(newList)
                        // 重置页面浏览索引
                        val position = newList.indexOfFirst { it.id == currentImageId }
                        if (position >= 0) {
                            binding.vpMain.setCurrentItem(position, false)
                        }
                        Log.d(TAG, "notifyLocalFilesChanged: ${oldList.size} -> ${newList.size}")
                    }
                }
            }
            is UIState.Error -> { finish() }
            else -> {}
        }
    }
}