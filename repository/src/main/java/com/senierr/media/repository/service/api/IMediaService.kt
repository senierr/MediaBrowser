package com.senierr.media.local.repository.service.api

import com.senierr.media.repository.entity.VolumeInfo
import com.senierr.media.local.repository.entity.LocalAudio
import com.senierr.media.local.repository.entity.LocalFolder
import com.senierr.media.local.repository.entity.LocalImage
import com.senierr.media.local.repository.entity.LocalVideo

/**
 * 数据服务
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
interface IMediaService {

    /**
     * 获取U盘挂载信息
     */
    suspend fun fetchUsbVolumes(): List<VolumeInfo>

    /**
     * 拉取指定目录下包含图片的文件夹
     *
     * @param bucketPath 父文件夹路径
     */
    suspend fun fetchLocalFoldersWithImage(bucketPath: String): List<LocalFolder>

    /**
     * 拉取指定目录下包含音频的文件夹
     *
     * @param bucketPath 父文件夹路径
     */
    suspend fun fetchLocalFoldersWithAudio(bucketPath: String): List<LocalFolder>

    /**
     * 拉取指定目录下包含视频的文件夹
     *
     * @param bucketPath 父文件夹路径
     */
    suspend fun fetchLocalFoldersWithVideo(bucketPath: String): List<LocalFolder>

    /**
     * 拉取指定目录下图片
     *
     * @param bucketPath 父文件夹路径
     */
    suspend fun fetchLocalImages(bucketPath: String, includeSubfolder: Boolean = false): List<LocalImage>

    /**
     * 拉取指定目录下音频
     *
     * @param bucketPath 父文件夹路径
     */
    suspend fun fetchLocalAudios(bucketPath: String, includeSubfolder: Boolean = false): List<LocalAudio>

    /**
     * 拉取指定目录下视频
     *
     * @param bucketPath 父文件夹路径
     */
    suspend fun fetchLocalVideos(bucketPath: String, includeSubfolder: Boolean = false): List<LocalVideo>
}