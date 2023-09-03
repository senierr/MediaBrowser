package com.pateo.module.usb.domain

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.pateo.module.usb.R
import com.pateo.module.usb.databinding.ActivityUsbMainBinding
import com.pateo.module.usb.domain.viewmodel.UsbMainViewModel
import com.pateo.module.usb.domain.wrapper.UsbAudioWrapper
import com.pateo.module.usb.domain.wrapper.UsbFolderWrapper
import com.pateo.module.usb.domain.wrapper.UsbImageWrapper
import com.pateo.module.usb.domain.wrapper.UsbVideoWrapper
import com.pateo.module.usb.ktx.applicationViewModel
import com.pateo.module.usb.ktx.showContentView
import com.pateo.module.usb.ktx.showEmptyView
import com.pateo.module.usb.ktx.showLoadingView
import com.pateo.module.usb.ktx.showUnmountView
import com.pateo.module.usb.utils.DiffUtils
import com.qinggan.usbvideo.repository.entity.UsbFile
import com.qinggan.usbvideo.repository.entity.UsbStatus
import com.senierr.adapter.internal.MultiTypeAdapter
import com.senierr.base.support.ui.BaseActivity
import com.senierr.base.support.ui.recyclerview.GridItemDecoration
import com.senierr.base.util.LogUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * U盘首页
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class UsbMainActivity : BaseActivity<ActivityUsbMainBinding>() {

    private val multiTypeAdapter = MultiTypeAdapter()
    private val folderWrapper = UsbFolderWrapper()
    private val imageWrapper = UsbImageWrapper()
    private val audioWrapper = UsbAudioWrapper()
    private val videoWrapper = UsbVideoWrapper()

    private val usbMainViewModel by applicationViewModel<UsbMainViewModel>()

    // 更新数据任务
    private var notifyDataChangedJob: Job? = null

    override fun createViewBinding(layoutInflater: LayoutInflater): ActivityUsbMainBinding {
        return ActivityUsbMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission(onGrant = {
            initView()
            initViewModel()
        })
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed")
        val volume = usbMainViewModel.getVolume()
        if (volume == null) {
            super.onBackPressed()
        } else if (volume.path == usbMainViewModel.currentFolderPath.replayCache.firstOrNull()) {
            // 当前已经是根目录
            super.onBackPressed()
        } else {
            // 返回上一级
            usbMainViewModel.fetchUsbFiles(volume.path)
        }
    }

    private fun initView() {
        setSupportActionBar(binding.tbTop)
        binding.tbTop.setNavigationOnClickListener { onBackPressed() }

        binding.rvList.layoutManager = GridLayoutManager(this, 4)
        binding.rvList.addItemDecoration(GridItemDecoration(12))
        binding.rvList.itemAnimator = null

        folderWrapper.setOnItemClickListener { _, _, item ->
            LogUtil.logD(TAG, "folder onClick: $item")
            // 拉取数据
            usbMainViewModel.fetchUsbFiles(item.path)
        }
        imageWrapper.setOnItemClickListener { _, _, item ->
            LogUtil.logD(TAG, "image onClick: $item")
            ImagePreviewActivity.start(this, item.id)
        }
        audioWrapper.setOnItemClickListener { _, _, item ->
            LogUtil.logD(TAG, "audio onClick: $item")
        }
        videoWrapper.setOnItemClickListener { _, _, item ->
            LogUtil.logD(TAG, "video onClick: $item")
            VideoPlayerActivity.start(this, item)
        }

        multiTypeAdapter.register(folderWrapper)
        multiTypeAdapter.register(imageWrapper)
        multiTypeAdapter.register(audioWrapper)
        multiTypeAdapter.register(videoWrapper)
        binding.rvList.adapter = multiTypeAdapter
    }

    private fun initViewModel() {
        usbMainViewModel.usbStatus
            .onEach {
                LogUtil.logD(TAG, "usbStatus-onChanged: $it")
                when (it.action) {
                    UsbStatus.ACTION_MOUNTED -> {
                        binding.msvState.showLoadingView()
                    }
                    UsbStatus.ACTION_SCANNER_STARTED, UsbStatus.ACTION_SCANNER_FINISHED -> {
                        binding.msvState.showContentView()
                    }
                    UsbStatus.ACTION_EJECT -> {
                        binding.msvState.showUnmountView()
                    }
                }
            }
            .launchIn(lifecycleScope)
        usbMainViewModel.currentFolderPath
            .onEach { notifyTitle(it) }
            .launchIn(lifecycleScope)
        usbMainViewModel.usbFiles
            .onEach {
                LogUtil.logD(TAG, "usbFiles-onChanged: ${it.size}")
                if (it.isEmpty()) {
                    if (usbMainViewModel.getVolume() == null) {
                        binding.msvState.showUnmountView()
                    } else {
                        binding.msvState.showEmptyView()
                    }
                } else {
                    binding.msvState.showContentView()
                    notifyDataChanged(it)
                }
            }
            .launchIn(lifecycleScope)
    }

    /**
     * 权限检查
     */
    private fun checkPermission(onGrant: () -> Unit, onFailure: (() -> Unit)? = null) {
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.values.none { !it }) {
                onGrant.invoke()
            } else {
                onFailure?.invoke()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            ))
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ))
        }
    }

    /**
     * 更新标题信息
     */
    private fun notifyTitle(folderPath: String?) {
        Log.d(TAG, "notifyTitle: $folderPath")
        lifecycleScope.launch {
            // 更新标题
            if (folderPath == null || usbMainViewModel.getVolume()?.path == folderPath) {
                binding.tbTop.setNavigationIcon(R.drawable.ic_close)
                binding.tbTop.title = getString(R.string.usb_app_name)
            } else {
                binding.tbTop.setNavigationIcon(R.drawable.ic_arrow_back)
                binding.tbTop.title = folderPath.substringAfterLast(File.separatorChar)
            }
        }
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
                // 重置页面浏览索引
                binding.rvList.scrollToPosition(0)
                Log.d(TAG, "notifyDataChanged: ${oldList.size} -> ${newList.size}")
            }
        }
    }
}