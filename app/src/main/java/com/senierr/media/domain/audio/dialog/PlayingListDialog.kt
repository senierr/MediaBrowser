package com.senierr.media.domain.audio.dialog

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.senierr.adapter.internal.MultiTypeAdapter
import com.senierr.base.support.arch.UIState
import com.senierr.base.support.coroutine.CoroutineCompat
import com.senierr.base.support.ui.BaseBottomDialogFragment
import com.senierr.base.util.LogUtil
import com.senierr.base.util.ScreenUtil
import com.senierr.media.databinding.DialogPlayingListBinding
import com.senierr.media.domain.audio.viewmodel.AudioControlViewModel
import com.senierr.media.domain.audio.wrapper.PlayingListWrapper
import com.senierr.media.ktx.applicationViewModel
import com.senierr.media.repository.entity.LocalAudio
import com.senierr.media.utils.DiffUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive


/**
 * 播放列表弹出框
 *
 * @author senierr_zhou
 * @date 2024/04/01
 */
class PlayingListDialog(
    private val onItemSelectedListener: (position: Int, item: LocalAudio) -> Unit
) : BaseBottomDialogFragment<DialogPlayingListBinding>() {

    private val coroutineCompat = CoroutineCompat(lifecycleScope)

    private val multiTypeAdapter = MultiTypeAdapter()
    private val playingListWrapper = PlayingListWrapper()

    private val controlViewModel: AudioControlViewModel by applicationViewModel()

    override fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): DialogPlayingListBinding {
        return DialogPlayingListBinding.inflate(inflater, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.peekHeight = (ScreenUtil.getScreenHeight(requireContext()) * 0.8).toInt()
        dialog.behavior.maxHeight = dialog.behavior.peekHeight
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initViewModel()
    }

    private fun initView() {
        binding?.rvPlayingList?.layoutManager = LinearLayoutManager(requireContext())
        binding?.rvPlayingList?.setHasFixedSize(true)
        playingListWrapper.setOnItemClickListener { _, position, item ->
            LogUtil.logD(TAG, "playingListWrapper onClick: $item")
            onItemSelectedListener.invoke(position, item)
        }
        multiTypeAdapter.register(playingListWrapper)
        binding?.rvPlayingList?.adapter = multiTypeAdapter
    }

    private fun initViewModel() {
        controlViewModel.localAudios
            .onEach { notifyLocalFilesChanged(it) }
            .launchIn(lifecycleScope)
    }

    /**
     * 更新页面数据
     */
    private fun notifyLocalFilesChanged(state: UIState<List<LocalAudio>>) {
        LogUtil.logD(TAG, "notifyLocalFilesChanged: $state")
        when (state) {
            is UIState.Content -> {
                coroutineCompat.launchSingle("notifyLocalFilesChanged") {
                    val oldList = multiTypeAdapter.data.filterIsInstance<LocalAudio>()
                    val newList = state.value
                    val diffResult = DiffUtils.diffLocalFile(oldList, newList)
                    if (isActive) {
                        // 更新播放列表
                        multiTypeAdapter.data.clear()
                        multiTypeAdapter.data.addAll(newList)
                        diffResult.dispatchUpdatesTo(multiTypeAdapter)
                        Log.d(TAG, "notifyLocalFilesChanged: ${oldList.size} -> ${newList.size}")
                    }
                }
            }
            else -> {}
        }
    }
}