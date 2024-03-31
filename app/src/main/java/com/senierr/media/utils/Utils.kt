package com.senierr.media.utils

import android.media.MediaMetadataRetriever

/**
 *
 * @author chunjiezhou
 * @date 2021/08/05
 */
object Utils {

    /**
     * 格式化播放时间
     */
    fun formatDuration(duration: Long): String {
        val d = duration / 1000
        val hour = d / 60 / 60
        val minute = d % (60 * 60) / 60
        val second = d % 60
        val h: String = if (hour < 10) "0$hour" else "$hour"
        val m: String = if (minute < 10) "0$minute" else "$minute"
        val s: String = if (second < 10) "0$second" else "$second"
        return "$h:$m:$s"
    }

    /**
     * 获取视频时长
     */
    fun getVideoDuration(videoPath: String?): Long {
        val duration: Long
        try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(videoPath)
            duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
        return duration
    }
}