package com.qinggan.usbvideo.repository.entity

import android.content.ContentUris
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * USB文件
 *
 * @author senierr_zhou
 * @date 2023/07/29
 */
open class UsbFile(
    open val id: Long,                      // ID
    open val path: String,                  // 路径
    open val displayName: String,           // 文件名
    open val bucketPath: String             // 所属文件夹路径
)

// USB文件夹
@Entity(tableName = "UsbFolder")
@Parcelize
data class UsbFolder(
    @PrimaryKey
    override val id: Long,                  // ID
    override val path: String,              // 路径
    override val displayName: String,       // 文件名
    override val bucketPath: String,        // 所属文件夹路径
    var imageCount: Int = 0,                // 所含图片数量
    var audioCount: Int = 0,                // 所含音频数量
    var videoCount: Int = 0,                // 所含视频数量
) : UsbFile(id, path, displayName, bucketPath), Parcelable

// USB音频
@Entity(tableName = "UsbAudio")
@Parcelize
data class UsbAudio(
    @PrimaryKey
    override val id: Long,                  // ID
    override val path: String,              // 路径
    override val displayName: String,       // 文件名
    override val bucketPath: String,        // 所属文件夹路径
    val mimeType: String,                   // MIME类型
    var duration: Long = 0                  // 时长
) : UsbFile(id, path, displayName, bucketPath), Parcelable {
    fun getUri(): Uri {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    }
}

// USB图片
@Entity(tableName = "UsbImage")
@Parcelize
data class UsbImage(
    @PrimaryKey
    override val id: Long,                  // ID
    override val path: String,              // 路径
    override val displayName: String,       // 文件名
    override val bucketPath: String,        // 所属文件夹路径
    val mimeType: String                    // MIME类型
) : UsbFile(id, path, displayName, bucketPath), Parcelable {
    fun getUri(): Uri {
        return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
    }
}

// USB视频
@Entity(tableName = "UsbVideo")
@Parcelize
data class UsbVideo(
    @PrimaryKey
    override val id: Long,                  // ID
    override val path: String,              // 路径
    override val displayName: String,       // 文件名
    override val bucketPath: String,        // 所属文件夹路径
    val mimeType: String,                   // MIME类型
    var duration: Long = 0                  // 时长
) : UsbFile(id, path, displayName, bucketPath), Parcelable {
    fun getUri(): Uri {
        return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
    }
}