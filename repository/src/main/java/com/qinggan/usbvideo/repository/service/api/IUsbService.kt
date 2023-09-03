package com.qinggan.usbvideo.repository.service.api

import com.qinggan.usbvideo.repository.entity.UsbFile
import com.qinggan.usbvideo.repository.entity.VolumeInfo

/**
 * U盘数据服务
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
interface IUsbService {

    /**
     * 获取U盘挂载信息
     */
    suspend fun fetchUsbVolumes(): List<VolumeInfo>

    /**
     * 同步U盘数据
     */
    suspend fun syncUsbFiles(
        volumePath: String,
        includeAudio: Boolean = true,
        includeImage: Boolean = true,
        includeVideo: Boolean = true
    )

    /**
     * 清除U盘数据数据
     */
    suspend fun clear(volumePath: String?)

    /**
     * 拉取指定文件夹下U盘数据
     *
     * @param bucketPath 父文件夹路径。默认空，即根目录。
     */
    suspend fun fetchUsbFilesByBucket(
        bucketPath: String,
        includeFolder: Boolean = true,
        includeAudio: Boolean = true,
        includeImage: Boolean = true,
        includeVideo: Boolean = true
    ): List<UsbFile>

    /**
     * 拉取指定挂载盘下U盘数据
     *
     * @param volumePath 父文件夹路径。默认空，即根目录。
     */
    suspend fun fetchUsbFilesByVolume(
        volumePath: String,
        includeFolder: Boolean = true,
        includeAudio: Boolean = true,
        includeImage: Boolean = true,
        includeVideo: Boolean = true
    ): List<UsbFile>
}