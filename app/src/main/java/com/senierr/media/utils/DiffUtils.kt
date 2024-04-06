package com.senierr.media.utils

import androidx.recyclerview.widget.DiffUtil
import com.senierr.media.repository.entity.LocalAudio
import com.senierr.media.repository.entity.LocalFile
import com.senierr.media.repository.entity.LocalFolder
import com.senierr.media.repository.entity.LocalImage
import com.senierr.media.repository.entity.LocalVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 对比工具类
 *
 * @author senierr_zhou
 * @date 2023/07/30
 */
object DiffUtils {

    /**
     * 对比U盘文件
     *
     * @param oldList 旧数据
     * @param newList 新数据
     */
    suspend fun diffLocalFile(oldList: List<LocalFile>, newList: List<LocalFile>): DiffUtil.DiffResult {
        return withContext(Dispatchers.IO) {
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = oldList.size

                override fun getNewListSize() = newList.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = oldList[oldItemPosition]
                    val newItem = newList[newItemPosition]
                    return if (oldItem is LocalFolder && newItem is LocalFolder) {
                        oldItem.path == newItem.path
                    } else if (oldItem is LocalImage && newItem is LocalImage) {
                        oldItem.path == newItem.path
                    } else if (oldItem is LocalAudio && newItem is LocalAudio) {
                        oldItem.path == newItem.path
                    } else if (oldItem is LocalVideo && newItem is LocalVideo) {
                        oldItem.path == newItem.path
                    } else {
                        false
                    }
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = oldList[oldItemPosition]
                    val newItem = newList[newItemPosition]
                    return if (oldItem is LocalFolder && newItem is LocalFolder) {
                        oldItem.path == newItem.path
                    } else if (oldItem is LocalImage && newItem is LocalImage) {
                        oldItem.path == newItem.path
                    } else if (oldItem is LocalAudio && newItem is LocalAudio) {
                        oldItem.path == newItem.path
                    } else if (oldItem is LocalVideo && newItem is LocalVideo) {
                        oldItem.path == newItem.path
                    } else {
                        false
                    }
                }
            }, false)
            return@withContext diffResult
        }
    }
}