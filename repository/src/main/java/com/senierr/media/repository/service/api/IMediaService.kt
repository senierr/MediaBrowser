package com.senierr.media.repository.service.api

import com.senierr.media.repository.entity.VolumeInfo
import com.senierr.media.repository.entity.LocalAudio
import com.senierr.media.repository.entity.LocalFolder
import com.senierr.media.repository.entity.LocalImage
import com.senierr.media.repository.entity.LocalVideo
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 数据服务
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
interface IMediaService {

    /**
     * 获取盘符挂载信息
     */
    suspend fun fetchVolumeInfoList(): List<VolumeInfo>

    /**
     * 订阅盘符挂载状态
     */
    suspend fun observeVolumeStatus(): SharedFlow<VolumeInfo>

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