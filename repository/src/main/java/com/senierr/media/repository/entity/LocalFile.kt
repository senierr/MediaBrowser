package com.senierr.media.local.repository.entity

import android.content.ContentUris
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import kotlinx.parcelize.Parcelize

/**
 * 本地文件
 *
 * @author senierr_zhou
 * @date 2023/07/29
 */
interface LocalFile

// 文件夹
@Parcelize
data class LocalFolder(
    val path: String,           // 路径
    val displayName: String,    // 文件名
    val bucketPath: String,     // 上级文件夹路径
    var imageCount: Int = 0,    // 所含图片数量
    var audioCount: Int = 0,    // 所含音频数量
    var videoCount: Int = 0,    // 所含视频数量
) : LocalFile, Parcelable

// 音频
@Parcelize
data class LocalAudio(
    val id: Long,               // ID
    val path: String,           // 路径
    val displayName: String,    // 文件名
    val bucketPath: String,     // 上级文件夹路径
    val mimeType: String,       // MIME类型
    var duration: Long = 0      // 时长，默认为0，需要后续去获取
) : LocalFile, Parcelable {
    fun getUri(): Uri {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    }
}

// 图片
@Parcelize
data class LocalImage(
    val id: Long,               // ID
    val path: String,           // 路径
    val displayName: String,    // 文件名
    val bucketPath: String,     // 上级文件夹路径
    val mimeType: String        // MIME类型
) : LocalFile, Parcelable {
    fun getUri(): Uri {
        return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
    }
}

// 视频
@Parcelize
data class LocalVideo(
    val id: Long,               // ID
    val path: String,           // 路径
    val displayName: String,    // 文件名
    val bucketPath: String,     // 上级文件夹路径
    val mimeType: String,       // MIME类型
    var duration: Long = 0      // 时长，默认为0，需要后续去获取
) : LocalFile, Parcelable {
    fun getUri(): Uri {
        return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
    }
}