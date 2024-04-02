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
    var id: Long,
    var path: String,           // 路径
    var mediaType: Int,         // 媒体类型 1：音频；2：视频
    var isPlaying: Boolean,     // 是否正在播放
    var position: Long,         // 播放进度
    var duration: Long,         // 总时长
    var timestamp: Long         // 开始播放时间戳
) {
    companion object {
        fun create(localAudio: LocalAudio): PlaySession {
            return PlaySession(0, localAudio.path, 1, false, 0, 0, System.currentTimeMillis())
        }
    }
}
