package com.senierr.media.domain.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.senierr.adapter.internal.MultiTypeAdapter
import com.senierr.base.support.arch.viewmodel.state.UIState
import com.senierr.base.support.ktx.onClick
import com.senierr.base.support.ktx.setGone
import com.senierr.base.support.ui.BaseFragment
import com.senierr.base.support.ui.recyclerview.GridItemDecoration
import com.senierr.base.util.LogUtil
import com.senierr.media.R
import com.senierr.media.databinding.FragmentHomeAudioBinding
import com.senierr.media.domain.audio.AudioPlayerActivity
import com.senierr.media.domain.home.viewmodel.AudioViewModel
import com.senierr.media.domain.home.wrapper.AudioWrapper
import com.senierr.media.domain.home.wrapper.FolderWrapper
import com.senierr.media.ktx.applicationViewModel
import com.senierr.media.ktx.showContentView
import com.senierr.media.ktx.showEmptyView
import com.senierr.media.ktx.showLoadingView
import com.senierr.media.ktx.showUnmountView
import com.senierr.media.repository.entity.LocalFile
import com.senierr.media.utils.DiffUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * 音乐首页
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class AudioHomeFragment : BaseFragment<FragmentHomeAudioBinding>() {

    private val multiTypeAdapter = MultiTypeAdapter()
    private val folderWrapper = FolderWrapper()
    private val audioWrapper = AudioWrapper()

    private val audioViewModel by applicationViewModel<AudioViewModel>()

    override fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeAudioBinding {
        return FragmentHomeAudioBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initViewModel()
        // 加载初始数据
        val rootFolderPath = audioViewModel.getRootFolderPath()
        if (rootFolderPath.isNullOrBlank()) {
            binding?.msvState?.showUnmountView()
        } else {
            audioViewModel.fetchLocalFiles(rootFolderPath)
        }
    }

    private fun initView() {
        val backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                LogUtil.logD(TAG, "onBackPressedDispatcher")
                val rootFolderPath = audioViewModel.getRootFolderPath()
                if (audioViewModel.currentFolder.value != rootFolderPath) {
                    if (rootFolderPath.isNullOrBlank()) {
                        binding?.msvState?.showUnmountView()
                    } else {
                        audioViewModel.fetchLocalFiles(rootFolderPath)
                    }
                } else {
                    requireActivity().finish()
                }
            }
        }
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                backPressedCallback.isEnabled = true
            }

            override fun onPause(owner: LifecycleOwner) {
                backPressedCallback.isEnabled = false
            }
        })
        requireActivity().onBackPressedDispatcher.addCallback(backPressedCallback)

        binding?.layoutTopBar?.btnBack?.onClick {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding?.rvList?.layoutManager = GridLayoutManager(requireContext(), 4)
        binding?.rvList?.addItemDecoration(GridItemDecoration(16))
        binding?.rvList?.itemAnimator = null
        binding?.rvList?.setHasFixedSize(true)

        folderWrapper.setOnItemClickListener { _, _, item ->
            LogUtil.logD(TAG, "folder onClick: $item")
            // 拉取数据
            audioViewModel.fetchLocalFiles(item.path)
        }
        audioWrapper.setOnItemClickListener { _, _, item ->
            LogUtil.logD(TAG, "audio onClick: $item")
            AudioPlayerActivity.start(requireContext(), item)
        }

        multiTypeAdapter.register(folderWrapper)
        multiTypeAdapter.register(audioWrapper)
        binding?.rvList?.adapter = multiTypeAdapter
    }

    private fun initViewModel() {
        audioViewModel.currentFolder
            .onEach { notifyTitle(it) }
            .launchIn(lifecycleScope)
        audioViewModel.localFiles
            .onEach { notifyLocalFilesChanged(it) }
            .launchIn(lifecycleScope)
    }

    /**
     * 更新标题信息
     */
    private fun notifyTitle(folderPath: String?) {
        Log.d(TAG, "notifyTitle: $folderPath")
        lifecycleScope.launch {
            // 更新标题
            if (folderPath == null || audioViewModel.getRootFolderPath() == folderPath) {
                binding?.layoutTopBar?.btnBack?.setGone(true)
                binding?.layoutTopBar?.tvTitle?.text = getString(R.string.app_name)
            } else {
                binding?.layoutTopBar?.btnBack?.setGone(false)
                binding?.layoutTopBar?.tvTitle?.text = folderPath.substringAfterLast(File.separatorChar)
            }
        }
    }

    /**
     * 更新页面数据
     */
    private fun notifyLocalFilesChanged(state: UIState<List<LocalFile>>) {
        LogUtil.logD(TAG, "notifyLocalFilesChanged: $state")
        when (state) {
            is UIState.Loading -> {
                binding?.msvState?.showLoadingView()
            }
            is UIState.Empty -> {
                binding?.msvState?.showEmptyView()
            }
            is UIState.Content -> {
                binding?.msvState?.showContentView()
                lifecycleScope.launchSingle("notifyLocalFilesChanged") {
                    val oldList = multiTypeAdapter.data.filterIsInstance<LocalFile>()
                    val diffResult = DiffUtils.diffLocalFile(oldList, state.value)
                    if (isActive) {
                        diffResult.dispatchUpdatesTo(multiTypeAdapter)
                        multiTypeAdapter.data.clear()
                        multiTypeAdapter.data.addAll(state.value)
                        // 重置页面浏览索引
                        binding?.rvList?.scrollToPosition(0)
                        Log.d(TAG, "notifyLocalFilesChanged: ${oldList.size} -> ${state.value.size}")
                    }
                }
            }
            is UIState.Error -> {
                binding?.msvState?.showUnmountView()
            }
        }
    }
}