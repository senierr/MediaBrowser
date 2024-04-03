package com.senierr.media.repository.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 播放会话
 *
 * @author senierr_zhou
 * @date 2023/07/30
 */
@Entity(tableName = "PlaySession")
data class PlaySession(
    @PrimaryKey
    var id: Long = 0,
    var path: String = "",                              // 路径
    var bucketPath: String = "",                        // 文件夹路径
    var mediaType: Int = 0,                             // 媒体类型 1：音频；2：视频
    var isPlaying: Boolean = false,                     // 是否正在播放
    var position: Long = 0,                             // 播放进度
    var duration: Long = 0,                             // 总时长
    var timestamp: Long = System.currentTimeMillis()    // 开始播放时间戳
) {
    companion object {
        const val MEDIA_TYPE_AUDIO = 1
        const val MEDIA_TYPE_VIDEO = 2
    }
}
