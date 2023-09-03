package com.pateo.module.usb.utils

import androidx.recyclerview.widget.DiffUtil
import com.qinggan.usbvideo.repository.entity.UsbFile
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
    suspend fun diffUsbFile(oldList: List<UsbFile>, newList: List<UsbFile>): DiffUtil.DiffResult {
        return withContext(Dispatchers.IO) {
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = oldList.size

                override fun getNewListSize() = newList.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldList[oldItemPosition].path == newList[newItemPosition].path
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldList[oldItemPosition].path == newList[newItemPosition].path
                }
            }, false)
            return@withContext diffResult
        }
    }
}